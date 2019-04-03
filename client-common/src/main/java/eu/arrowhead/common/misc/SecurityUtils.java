/*
 *  Copyright (c) 2018 AITIA International Inc.
 *
 *  This work is part of the Productive 4.0 innovation project, which receives grants from the
 *  European Commissions H2020 research and innovation programme, ECSEL Joint Undertaking
 *  (project no. 737459), the free state of Saxony, the German Federal Ministry of Education and
 *  national funding authorities from involved countries.
 */

package eu.arrowhead.common.misc;

import eu.arrowhead.common.exception.ArrowheadRuntimeException;
import eu.arrowhead.common.exception.AuthException;
import eu.arrowhead.common.model.CertificateSigningRequest;
import eu.arrowhead.common.model.CertificateSigningResponse;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyManagementException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.security.Security;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Enumeration;
import java.util.NoSuchElementException;
import java.util.ServiceConfigurationError;
import java.util.regex.Pattern;
import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;

@SuppressWarnings("unused")
public final class SecurityUtils {
    private static final Logger log = LogManager.getLogger(SecurityUtils.class);

    private static final HostnameVerifier allHostsValid = (hostname, session) -> {
        // Decide whether to allow the connection...
        return true;
    };

    private static Provider securityProvider;

    public static synchronized void addSecurityProvider() {
        // Late creation of BouncyCastleProvider since it can be slow to low on a Raspberry Pi
        if (securityProvider == null) securityProvider = new BouncyCastleProvider();
        if (Security.getProvider(securityProvider.getName()) == null)
            Security.addProvider(securityProvider);
    }

    public static KeyStore loadKeyStore(String filePath, String pass) {
        try {
            KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
            InputStream is = new FileInputStream(filePath);
            keystore.load(is, pass.toCharArray());
            is.close();
            return keystore;
        } catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException e) {
            throw new ServiceConfigurationError("Loading the keystore failed...", e);
        }
    }

    public static X509Certificate getFirstCertFromKeyStore(KeyStore keystore) {
        try {
            Enumeration<String> enumeration = keystore.aliases();
            String alias = enumeration.nextElement();
            Certificate certificate = keystore.getCertificate(alias);
            return (X509Certificate) certificate;
        } catch (KeyStoreException | NoSuchElementException e) {
            throw new ServiceConfigurationError("Getting the first cert from keystore failed...", e);
        }
    }

    public static String getCertCNFromSubject(String subjectname) {
        String cn = null;
        try {
            // Subject is in LDAP format, we can use the LdapName object for parsing
            LdapName ldapname = new LdapName(subjectname);
            for (Rdn rdn : ldapname.getRdns()) {
                // Find the data after the CN field
                if (rdn.getType().equalsIgnoreCase("CN")) {
                    cn = (String) rdn.getValue();
                }
            }
        } catch (InvalidNameException e) {
            log.warn("InvalidNameException in getCertCNFromSubject", e);
            return "";
        }

        if (cn == null) {
            return "";
        }

        return cn;
    }

    public static PrivateKey getPrivateKey(KeyStore keystore, String pass) {
        PrivateKey privatekey = null;
        String element;
        try {
            Enumeration<String> enumeration = keystore.aliases();
            while (enumeration.hasMoreElements()) {
                element = enumeration.nextElement();
                privatekey = (PrivateKey) keystore.getKey(element, pass.toCharArray());
                if (privatekey != null) {
                    break;
                }
            }
        } catch (KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException e) {
            throw new ServiceConfigurationError("Getting the private key from keystore failed...", e);
        }

        if (privatekey == null) {
            throw new ServiceConfigurationError("Getting the private key failed, keystore aliases do not identify a key.");
        }
        return privatekey;
    }

    public static KeyStore createKeyStoreFromCert(String filePath) {
        try {
            InputStream is = new FileInputStream(filePath);
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            X509Certificate cert = (X509Certificate) cf.generateCertificate(is);
            String alias = getCertCNFromSubject(cert.getSubjectDN().getName());

            KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
            keystore.load(null); // We don't need the KeyStore instance to come from a file.
            keystore.setCertificateEntry(alias, cert);
            return keystore;
        } catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException e) {
            throw new ServiceConfigurationError("Keystore creation from cert failed...", e);
        }
    }

    public static SSLContext createAcceptAllSSLContext() {
        SSLContext sslContext;
        try {
            sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, createTrustManagers(), null);
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new ServiceConfigurationError("AcceptAll SSLContext creation failed...", e);
        }
        return sslContext;
    }

    public static boolean isKeyStoreCNArrowheadValid(String commonName) {
        String[] cnFields = commonName.split("\\.", 0);
        return cnFields.length == 5 && cnFields[3].equals("arrowhead") && cnFields[4].equals("eu");
    }

    public static boolean isTrustStoreCNArrowheadValid(String commonName) {
        String[] cnFields = commonName.split("\\.", 0);
        return cnFields.length == 4 && cnFields[2].equals("arrowhead") && cnFields[3].equals("eu");
    }

    public static boolean isKeyStoreCNArrowheadValidLegacy(String commonName) {
        String[] cnFields = commonName.split("\\.", 0);
        return cnFields.length == 6 && cnFields[3].equals("arrowhead") && cnFields[4].equals("eu");
    }

    public static X509Certificate getCertFromKeyStore(KeyStore keystore, String name) {
        Enumeration<String> enumeration;
        try {
            enumeration = keystore.aliases();
        } catch (KeyStoreException e) {
            throw new AuthException("Keystore error", e);
        }

        while (enumeration.hasMoreElements()) {
            String alias = enumeration.nextElement();

            X509Certificate clientCert;
            try {
                clientCert = (X509Certificate) keystore.getCertificate(alias);
            } catch (KeyStoreException e) {
                log.warn("KeyStoreException", e);
                continue;
            }
            String clientCertCN = getCertCNFromSubject(clientCert.getSubjectDN().getName());

            if (!clientCertCN.equals(name)) {
                continue;
            }
            return clientCert;
        }

        return null;
    }

    public static String getKeyEncoded(Key key) {
        if (key == null) {
            return "";
        }

        byte[] encpub = key.getEncoded();
        StringBuilder sb = new StringBuilder(encpub.length * 2);
        for (byte b : encpub) {
            sb.append(String.format("%02x", b & 0xff));
        }
        return sb.toString();
    }

    public static String getByteEncoded(byte[] array) {
        StringBuilder sb = new StringBuilder(array.length * 2);
        for (byte b : array) {
            sb.append(String.format("%02X", b & 0xff));
        }
        return sb.toString();
    }

    /**
     * Extract a public key either from a PEM encoded file or directly from the Base64 coded string.
     *
     * @param filePathOrEncodedKey either a file path for the PEM file or the Base64 encoded key
     * @param isFilePath true if the first parameter is a file path
     *
     * @return the PublicKey
     */
    public static PublicKey getPublicKey(String filePathOrEncodedKey, boolean isFilePath) {
        byte[] keyBytes;
        if (isFilePath) {
            keyBytes = loadPEM(filePathOrEncodedKey);
        } else {
            try {
                keyBytes = Base64.getDecoder().decode(filePathOrEncodedKey);
            } catch (IllegalArgumentException | NullPointerException e) {
                throw new AuthException("Public key decoding failed! Caused by: " + e.getMessage(), e);
            }
        }
        try {
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return kf.generatePublic(new X509EncodedKeySpec(keyBytes));
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError("KeyFactory.getInstance(String) throws NoSuchAlgorithmException, code needs to be changed!", e);
        } catch (InvalidKeySpecException e) {
            throw new AuthException("PublicKey decoding failed due wrong input key", e);
        }
    }

    public static PublicKey getPublicKey(String keyPath) {
        if (keyPath.endsWith("crt")) {
            KeyStore authKeyStore = SecurityUtils.createKeyStoreFromCert(keyPath);
            X509Certificate authCert = SecurityUtils.getFirstCertFromKeyStore(authKeyStore);
            return authCert.getPublicKey();
        } else { // This is just a PEM encoded public key
            return SecurityUtils.getPublicKey(keyPath, true);
        }
    }

    public static byte[] loadPEM(String filePath) {
        try (FileInputStream in = new FileInputStream(filePath)) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buf = new byte[1024];
            for (int read = 0; read != -1; read = in.read(buf)) {
                baos.write(buf, 0, read);
            }
            String pem = new String(baos.toByteArray(), StandardCharsets.ISO_8859_1);
            baos.close();
            Pattern parse = Pattern.compile("(?m)(?s)^---*BEGIN.*---*$(.*)^---*END.*---*$.*");
            String encoded = parse.matcher(pem).replaceFirst("$1");
            return Base64.getMimeDecoder().decode(encoded);
        } catch (IOException e) {
            throw new ArrowheadRuntimeException("IOException occurred during PEM file loading from " + filePath, e);
        }
    }

    private static TrustManager[] createTrustManagers() {
        return new TrustManager[]{new X509TrustManager() {

            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[]{};
            }

            public void checkClientTrusted(X509Certificate[] chain, String authType) {
            }

            public void checkServerTrusted(X509Certificate[] chain, String authType) {
            }
        }};
    }

    /**
     * Generate a new 2048 bit RSA key pair
     */
    public static KeyPair generateRSAKeyPair() {
        KeyPairGenerator keyGen;
        try {
            keyGen = KeyPairGenerator.getInstance("RSA");
        } catch (NoSuchAlgorithmException e) {
            throw new ServiceConfigurationError("KeyPairGenerator has no RSA algorithm", e);
        }
        keyGen.initialize(2048);
        return keyGen.generateKeyPair();
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
        if (!(fileName.endsWith(".p12") || fileName.endsWith(".jks"))) {
            throw new ServiceConfigurationError("File name should end with its extension! (p12 or jks)");
        }
        if (saveLocation != null) {
            fileName = saveLocation + File.separator + fileName;
        }
        try (FileOutputStream fos = new FileOutputStream(fileName)) {
            keyStore.store(fos, keyStorePassword);
        } catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException e) {
            throw new ArrowheadRuntimeException("Saving keystore to file " + fileName + " failed!", e);
        }
    }

    /**
     * Convert PEM encoded cert back to an X509Certificate
     */
    public static X509Certificate getCertFromString(String encodedCert) {
        try {
            byte[] rawCert = Base64.getDecoder().decode(encodedCert);
            ByteArrayInputStream bIn = new ByteArrayInputStream(rawCert);
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            return (X509Certificate) cf.generateCertificate(bIn);
        } catch (CertificateException e) {
            throw new AuthException("Encapsulated exceptions...", e);
        }
    }

    /**
     * Gets
     * 1) a KeyStore with an Arrowhead compliant certificate chain for an Application System
     * 2) a KeyStore containing a single LocalCloud level certificate (which is issued my the Arrowhead master certificate), without a private key
     * <p>
     * from the local Certificate authority.
     *
     * @param commonName
     * @param signingResponse
     * @param systemKsPassword password for the application system keystore
     * @return the constructed KeyStores in an array,
     *
     * @see <a href="https://tools.ietf.org/html/rfc5280.html#section-7.1">X.509 certificate specification: distinguished names</a>
     */
    public static KeyStore createKeyStore(String commonName, CertificateSigningResponse signingResponse, char[] systemKsPassword) {
        //Get the reconstructed certs from the CA response
        X509Certificate signedCert = getCertFromString(signingResponse.getEncodedSignedCert());
        X509Certificate cloudCert = getCertFromString(signingResponse.getIntermediateCert());
        X509Certificate rootCert = getCertFromString(signingResponse.getRootCert());

        //Create the System KeyStore
        try {
            KeyStore ks = KeyStore.getInstance("pkcs12");
            ks.load(null, systemKsPassword);
            Certificate[] chain = new Certificate[]{signedCert, cloudCert, rootCert};
            ks.setKeyEntry(commonName, signingResponse.getLocalPrivateKey(), systemKsPassword, chain);
            return ks;
        } catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException e) {
            throw new ArrowheadRuntimeException("System key store creation failed!", e);
        }
    }

    /**
     * Gets
     * 1) a KeyStore with an Arrowhead compliant certificate chain for an Application System
     * 2) a KeyStore containing a single LocalCloud level certificate (which is issued my the Arrowhead master certificate), without a private key
     * <p>
     * from the local Certificate authority.
     *
     * @param cloudCN LocalCloud level common name: &lt;cloud_name&gt;.&lt;operator&gt;.arrowhead.eu
     * @param signingResponse
     * @param cloudKsPassword password for the cloud keystore
     *
     * @return the constructed KeyStores in an array,
     *
     * @see <a href="https://tools.ietf.org/html/rfc5280.html#section-7.1">X.509 certificate specification: distinguished names</a>
     */
    public static KeyStore createTrustStore(String cloudCN, CertificateSigningResponse signingResponse, char[] cloudKsPassword) {
        //Get the reconstructed certs from the CA response
        X509Certificate cloudCert = getCertFromString(signingResponse.getIntermediateCert());

      /*
        Create the Cloud KeyStore (with a different KeyStore Entry type,
        since we do not have the private key for the cloud cert)
       */
        try {
            KeyStore ks = KeyStore.getInstance("pkcs12");
            ks.load(null, cloudKsPassword);
            KeyStore.Entry certEntry = new KeyStore.TrustedCertificateEntry(cloudCert);
            ks.setEntry(cloudCN, certEntry, null);
            return ks;
        } catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException e) {
            throw new ArrowheadRuntimeException("System key store creation failed!", e);
        }
    }

    public static void savePEM(PublicKey publicKey, String filePath) {
        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            OutputStreamWriter osw = new OutputStreamWriter(fos);
            JcaPEMWriter pemWriter = new JcaPEMWriter(osw);
            pemWriter.writeObject(publicKey);
            pemWriter.flush();
            pemWriter.close();
            osw.close();
        } catch (IOException e) {
            throw new ArrowheadRuntimeException("IO exception during Authorization public key save!", e);
        }
    }

    public static CertificateSigningRequest createSigningRequest(String commonName, KeyPair keyPair) {
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
        return new CertificateSigningRequest(encodedCertRequest);
    }

    public static Client createClient(SSLContext context) {
        ClientConfig configuration = new ClientConfig();
        configuration.property(ClientProperties.CONNECT_TIMEOUT, 30000);
        configuration.property(ClientProperties.READ_TIMEOUT, 30000);

        Client client;
        if (context != null) {
            client = ClientBuilder.newBuilder().sslContext(context).withConfig(configuration).hostnameVerifier(allHostsValid).build();
        } else {
            client = ClientBuilder.newClient(configuration);
        }
        // TODO Can we use eu.arrowhead.common.api.ArrowheadConverter here?
        client.register(JacksonJsonProviderAtRest.class);
        return client;
    }
}
