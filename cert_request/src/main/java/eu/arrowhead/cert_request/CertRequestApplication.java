package eu.arrowhead.cert_request;

import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.ServiceConfigurationError;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
public class CertRequestApplication {

  private static String SYSTEM_NAME;
  private static String CLOUD_NAME;
  private static String OPERATOR;
  private static char[] KEYSTORE_PASSWORD;
  private static String CA_URL;
  private static String COMMON_NAME;

  public static void main(String[] args) {
    SpringApplication.run(CertRequestApplication.class, args);
    COMMON_NAME = SYSTEM_NAME + "." + CLOUD_NAME + "." + OPERATOR + ".arrowhead.eu";
    getSignedCertificate();
    System.exit(0);
  }

  private static void getSignedCertificate() {
    Security.addProvider(new BouncyCastleProvider());

    //Generate a new 2048 bit RSA key pair
    KeyPairGenerator keyGen;
    try {
      keyGen = KeyPairGenerator.getInstance("RSA");
    } catch (NoSuchAlgorithmException e) {
      throw new ServiceConfigurationError("KeyPairGenerator has no RSA algorithm", e);
    }
    keyGen.initialize(2048);
    KeyPair keyPair = keyGen.generateKeyPair();

    //Create the PKCS10 certificate request (signed by private key)
    ContentSigner signer;
    try {
      signer = new JcaContentSignerBuilder("SHA512withRSA").setProvider("BC").build(keyPair.getPrivate());
    } catch (OperatorCreationException e) {
      throw new RuntimeException("Certificate request signing failed! (" + e.getMessage() + ")", e);
    }
    PKCS10CertificationRequest csr = new JcaPKCS10CertificationRequestBuilder(new X500Name("CN=" + COMMON_NAME), keyPair.getPublic()).build(signer);

    //Encode the CSR, and send it to the Certificate Authority core system
    String encodedCertRequest;
    try {
      encodedCertRequest = Base64.getEncoder().encodeToString(csr.getEncoded());
    } catch (IOException e) {
      throw new RuntimeException("Failed to encode certificate signing request!", e);
    }
    CertificateSigningRequest request = new CertificateSigningRequest(encodedCertRequest);
    RestTemplate restTemplate = new RestTemplate();
    CertificateSigningResponse response = restTemplate.postForObject(CA_URL, request, CertificateSigningResponse.class);
    if (response == null) {
      throw new RuntimeException("Response object was null from CA module");
    }

    //Get the reconstructed certs from the CA response
    X509Certificate signedCert = getCertFromString(response.getEncodedSignedCert());
    X509Certificate cloudCert = getCertFromString(response.getIntermediateCert());
    X509Certificate rootCert = getCertFromString(response.getRootCert());

    //Save the certificate chain to a keystore
    try {
      KeyStore ks = KeyStore.getInstance("pkcs12");
      ks.load(null, KEYSTORE_PASSWORD);
      Certificate[] chain = new Certificate[]{signedCert, cloudCert, rootCert};
      ks.setKeyEntry(COMMON_NAME, keyPair.getPrivate(), KEYSTORE_PASSWORD, chain);
      FileOutputStream fos = new FileOutputStream(SYSTEM_NAME + ".p12");
      ks.store(fos, KEYSTORE_PASSWORD);
      fos.close();
    } catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException e) {
      throw new RuntimeException("Saving cert to keystore failed!", e);
    }
    System.out.println("keystore saved");
  }

  private static X509Certificate getCertFromString(String encodedCert) {
    try {
      byte[] rawCert = Base64.getDecoder().decode(encodedCert);
      ByteArrayInputStream bIn = new ByteArrayInputStream(rawCert);
      CertificateFactory cf = CertificateFactory.getInstance("X.509");
      return (X509Certificate) cf.generateCertificate(bIn);
    } catch (CertificateException e) {
      throw new RuntimeException("Encapsulated exceptions...", e);
    }
  }

  @Value("${system_name}")
  public void setSystemName(String systemName) {
    SYSTEM_NAME = systemName;
  }

  @Value("${cloud_name}")
  public void setCloudName(String cloudName) {
    CLOUD_NAME = cloudName;
  }

  @Value("${operator}")
  public void setOperator(String operator) {
    OPERATOR = operator;
  }

  @Value("${keystore_password}")
  public void setKeystorePassword(char[] keystorePassword) {
    KEYSTORE_PASSWORD = keystorePassword;
  }

  @Value("${cert_authority_url}")
  public void setCaUrl(String caUrl) {
    CA_URL = caUrl;
  }
}
