package eu.arrowhead.client.common;

import eu.arrowhead.client.common.exception.ArrowheadException;
import eu.arrowhead.client.common.exception.AuthException;
import eu.arrowhead.client.common.misc.SecurityUtils;
import eu.arrowhead.client.common.model.CertificateSigningRequest;
import eu.arrowhead.client.common.model.CertificateSigningResponse;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.ServiceConfigurationError;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;

public final class CertificateBootstrapper {

  private static String CA_URL;

  //Static initializer: get the Certificate Authority URL when the class first gets used
  static {
    Security.addProvider(new BouncyCastleProvider());
    CA_URL = Utility.getProp().getProperty("cert_authority_url");
    if (CA_URL == null) {
      throw new ArrowheadException("cert_authority_url property is not provided in config file, but certificate bootstrapping is requested!",
                                   Status.BAD_REQUEST.getStatusCode());
    }
  }

  private CertificateBootstrapper() {
    throw new AssertionError("CertificateBootstrapper is a non-instantiable class");
  }

  /*
    Gets the Cloud Common Name from the Certificate Authority Core System, proper URL is read from the config file
   */
  public static String getCloudCommonNameFromCA() {
    Response caResponse = Utility.sendRequest(CA_URL, "GET", null);
    return caResponse.readEntity(String.class);
  }

  /**
   * Get a KeyStore with an Arrowhead compliant certificate chain for an Application System from the Certificate Authority
   *
   * @param commonName will be used at the CN (Common Name) field of the certificate, this has to be Arrowhead compliant: &lt;system_name&gt;
   *     .&lt;cloud_name&gt;.&lt;operator&gt;.arrowhead.eu
   * @param keyStorePassword the password to load the KeyStore and to protect the private key
   *
   * @return the constructed KeyStore
   *
   * @see <a href="https://tools.ietf.org/html/rfc5280.html#section-7.1">X.509 certificate specification: distinguished names</a>
   */
  public static KeyStore obtainSystemKeyStore(String commonName, char[] keyStorePassword) {
    CertificateSigningResponse signingResponse = getSignedCertFromCA(commonName);

    //Get the reconstructed certs from the CA response
    X509Certificate signedCert = getCertFromString(signingResponse.getEncodedSignedCert());
    X509Certificate cloudCert = getCertFromString(signingResponse.getIntermediateCert());
    X509Certificate rootCert = getCertFromString(signingResponse.getRootCert());

    //Create the new KeyStore
    try {
      KeyStore ks = KeyStore.getInstance("pkcs12");
      ks.load(null, keyStorePassword);
      Certificate[] chain = new Certificate[]{signedCert, cloudCert, rootCert};
      ks.setKeyEntry(commonName, signingResponse.getLocalPrivateKey(), keyStorePassword, chain);
      return ks;
    } catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException e) {
      throw new ArrowheadException("KeyStore creation failed!", e);
    }
  }

  /**
   * Gets
   * 1) a KeyStore with an Arrowhead compliant certificate chain for an Application System
   * 2) a KeyStore containing a single LocalCloud level certificate (which is issued my the Arrowhead master certificate), without a private key
   * <p>
   * from the local Certificate authority.
   *
   * @param systemName Name of the application system needing the new certificate (will be used in the common name field)
   * @param cloudCN LocalCloud level common name: &lt;cloud_name&gt;.&lt;operator&gt;.arrowhead.eu
   * @param systemKsPassword password for the application system keystore
   * @param cloudKsPassword password for the cloud keystore
   *
   * @return the constructed KeyStores in an array,
   *
   * @see <a href="https://tools.ietf.org/html/rfc5280.html#section-7.1">X.509 certificate specification: distinguished names</a>
   */
  public static KeyStore[] obtainSystemAndCloudKeyStore(String systemName, String cloudCN, char[] systemKsPassword, char[] cloudKsPassword) {
    String commonName = systemName + "." + cloudCN;
    CertificateSigningResponse signingResponse = getSignedCertFromCA(commonName);

    //Get the reconstructed certs from the CA response
    X509Certificate signedCert = getCertFromString(signingResponse.getEncodedSignedCert());
    X509Certificate cloudCert = getCertFromString(signingResponse.getIntermediateCert());
    X509Certificate rootCert = getCertFromString(signingResponse.getRootCert());

    //Create the System KeyStore
    KeyStore[] keyStores = new KeyStore[2];
    try {
      KeyStore ks = KeyStore.getInstance("pkcs12");
      ks.load(null, systemKsPassword);
      Certificate[] chain = new Certificate[]{signedCert, cloudCert, rootCert};
      ks.setKeyEntry(commonName, signingResponse.getLocalPrivateKey(), systemKsPassword, chain);
      keyStores[0] = ks;
    } catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException e) {
      throw new ArrowheadException("System key store creation failed!", e);
    }

    /*
      Create the Cloud KeyStore (with a different KeyStore Entry type,
      since we do not have the private key for the cloud cert)
     */
    try {
      KeyStore ks = KeyStore.getInstance("pkcs12");
      ks.load(null, cloudKsPassword);
      KeyStore.Entry certEntry = new KeyStore.TrustedCertificateEntry(cloudCert);
      ks.setEntry(cloudCN, certEntry, new KeyStore.PasswordProtection(cloudKsPassword));
      keyStores[1] = ks;
    } catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException e) {
      throw new ArrowheadException("System key store creation failed!", e);
    }

    return keyStores;
  }

  /**
   * Saves the provided KeyStore to a file
   *
   * @param keyStore the certificate storage facility needed to be saved to file
   * @param keyStorePassword password to open the keystore
   * @param fileName filename must end with a valid keystore file extension (p12 or jks)
   * @param saveLocation optional relative or absolute path where the keystore should be saved (must point to directory). If not provided, the file
   *     will be placed into the working directory.
   */
  public static void saveKeyStoreToFile(KeyStore keyStore, char[] keyStorePassword, String fileName, String saveLocation) {
    if (keyStore == null || fileName == null) {
      throw new NullPointerException("Saving the key store to file is not possible, key store or file name is null!");
    }
    if (!fileName.endsWith(".p12") || !fileName.endsWith(".jks")) {
      throw new ServiceConfigurationError("File name should end with its extension! (p12 or jks)");
    }
    if (saveLocation != null) {
      fileName = saveLocation + File.separator + fileName;
    }
    try {
      FileOutputStream fos = new FileOutputStream(fileName);
      keyStore.store(fos, keyStorePassword);
      fos.close();
    } catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException e) {
      throw new ArrowheadException("Saving keystore to file " + fileName + " failed!", e);
    }
  }

  /*
     Updates the given properties file with the given key-value pairs.
   */
  public static void updateConfigurationFiles(String configLocation, Map<String, String> configValues) {
    try {
      FileInputStream in = new FileInputStream(configLocation);
      Properties props = new Properties();
      props.load(in);
      in.close();

      FileOutputStream out = new FileOutputStream(configLocation);
      for (Entry<String, String> entry : configValues.entrySet()) {
        props.setProperty(entry.getKey(), entry.getValue());
      }
      props.store(out, null);
      out.close();
    } catch (IOException e) {
      throw new ArrowheadException("Cert bootstrapping: IOException during configuration file update", e);
    }
  }

  /*
    Authorization Public Key is used by ArrowheadProviders to verify the signatures by the Authorization Core System in secure mode
   */
  public static PublicKey getAuthorizationPublicKey() {
    Response caResponse = Utility.sendRequest(CA_URL + "/auth", "GET", null);
    try {
      return SecurityUtils.getPublicKey(caResponse.readEntity(String.class));
    } catch (InvalidKeySpecException e) {
      throw new ArrowheadException("Could not decode public key from CA response!", e);
    }
  }

  //Generate a new 2048 bit RSA key pair
  private static KeyPair generateRSAKeyPair() {
    KeyPairGenerator keyGen;
    try {
      keyGen = KeyPairGenerator.getInstance("RSA");
    } catch (NoSuchAlgorithmException e) {
      throw new ServiceConfigurationError("KeyPairGenerator has no RSA algorithm", e);
    }
    keyGen.initialize(2048);
    return keyGen.generateKeyPair();
  }

  private static CertificateSigningResponse getSignedCertFromCA(String commonName) {
    //Get a new locally generated public/private key pair
    KeyPair keyPair = generateRSAKeyPair();

    //Create the PKCS10 certificate request (signed by private key)
    ContentSigner signer;
    try {
      signer = new JcaContentSignerBuilder("SHA512withRSA").setProvider("BC").build(keyPair.getPrivate());
    } catch (OperatorCreationException e) {
      throw new AuthException("Certificate request signing failed! (" + e.getMessage() + ")", e);
    }
    PKCS10CertificationRequest csr = new JcaPKCS10CertificationRequestBuilder(new X500Name("CN=" + commonName), keyPair.getPublic()).build(signer);

    //Encode the CSR, and send it to the Certificate Authority core system
    String encodedCertRequest;
    try {
      encodedCertRequest = Base64.getEncoder().encodeToString(csr.getEncoded());
    } catch (IOException e) {
      throw new AuthException("Failed to encode certificate signing request!", e);
    }
    CertificateSigningRequest request = new CertificateSigningRequest(encodedCertRequest);
    Response caResponse = Utility.sendRequest(CA_URL, "POST", request);
    CertificateSigningResponse signingResponse = caResponse.readEntity(CertificateSigningResponse.class);
    signingResponse.setLocalPrivateKey(keyPair.getPrivate());
    return signingResponse;
  }

  //Convert PEM encoded cert back to an X509Certificate
  @SuppressWarnings("Duplicates")
  private static X509Certificate getCertFromString(String encodedCert) {
    try {
      byte[] rawCert = Base64.getDecoder().decode(encodedCert);
      ByteArrayInputStream bIn = new ByteArrayInputStream(rawCert);
      CertificateFactory cf = CertificateFactory.getInstance("X.509");
      return (X509Certificate) cf.generateCertificate(bIn);
    } catch (CertificateException e) {
      throw new AuthException("Encapsulated exceptions...", e);
    }
  }

}
