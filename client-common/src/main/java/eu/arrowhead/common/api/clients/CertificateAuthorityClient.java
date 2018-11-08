package eu.arrowhead.common.api.clients;

import eu.arrowhead.common.api.ArrowheadSecurityContext;
import eu.arrowhead.common.exception.ArrowheadRuntimeException;
import eu.arrowhead.common.exception.AuthException;
import eu.arrowhead.common.exception.KeystoreException;
import eu.arrowhead.common.misc.ArrowheadProperties;
import eu.arrowhead.common.misc.SecurityUtils;
import eu.arrowhead.common.misc.Utility;
import eu.arrowhead.common.model.CertificateSigningRequest;
import eu.arrowhead.common.model.CertificateSigningResponse;
import org.apache.log4j.Logger;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.ws.rs.core.Response;
import java.io.File;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.PublicKey;
import java.security.Security;

public final class CertificateAuthorityClient extends RestClient {
    private static final Logger LOG = Logger.getLogger(CertificateAuthorityClient.class);
    private String keyPass, truststore, truststorePass, keystorePass;
    private String confDir, certDir;
    private String clientName;

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    public static CertificateAuthorityClient createFromProperties() {
        return createFromProperties(ArrowheadProperties.loadDefault());
    }

    public static CertificateAuthorityClient createFromProperties(ArrowheadProperties props) {
        final boolean isSecure = props.isSecure();
        if (!isSecure)
            LOG.warn("Trying to create CertificateAuthorityClient but secure=false in config file");
        return new CertificateAuthorityClient(isSecure)
                .setAddress(props.getCaAddress())
                .setPort(props.getCaPort())
                .setKeyPass(props.getKeyPass())
                .setTruststore(props.getTruststore())
                .setTruststorePass(props.getTruststorePass())
                .setKeystorePass(props.getKeystorePass())
                .setConfDir(ArrowheadProperties.getConfDir())
                .setCertDir(props.getCertDir())
                .setClientName(props.getSystemName())
                .setServicePath("ca");
    }

    public static CertificateAuthorityClient createDefault(String clientName) {
        final boolean isSecure = ArrowheadProperties.getDefaultIsSecure();
        return new CertificateAuthorityClient(isSecure)
                .setAddress(ArrowheadProperties.getDefaultCaAddress())
                .setPort(ArrowheadProperties.getDefaultCaPort(isSecure))
                .setConfDir(ArrowheadProperties.getConfDir())
                .setCertDir(ArrowheadProperties.getDefaultCertDir())
                .setClientName(clientName)
                .setServicePath("ca");
    }

    public CertificateAuthorityClient(boolean secure) {
        super(secure);
    }

    public String getKeyPass() {
        return keyPass;
    }

    public CertificateAuthorityClient setKeyPass(String keyPass) {
        this.keyPass = keyPass;
        return this;
    }

    public String getTruststore() {
        return truststore;
    }

    public CertificateAuthorityClient setTruststore(String truststore) {
        this.truststore = truststore;
        return this;
    }

    public String getTruststorePass() {
        return truststorePass;
    }

    public CertificateAuthorityClient setTruststorePass(String truststorePass) {
        this.truststorePass = truststorePass;
        return this;
    }

    public String getKeystorePass() {
        return keystorePass;
    }

    public CertificateAuthorityClient setKeystorePass(String keystorePass) {
        this.keystorePass = keystorePass;
        return this;
    }

    @Override
    public CertificateAuthorityClient setAddress(String address) {
        super.setAddress(address);
        return this;
    }

    @Override
    public CertificateAuthorityClient setPort(int port) {
        super.setPort(port);
        return this;
    }

    @Override
    public CertificateAuthorityClient setServicePath(String path) {
        super.setServicePath(path);
        return this;
    }

    public String getConfDir() {
        return confDir;
    }

    public CertificateAuthorityClient setConfDir(String confDir) {
        this.confDir = confDir;
        return this;
    }

    public String getCertDir() {
        return certDir;
    }

    public CertificateAuthorityClient setCertDir(String certDir) {
        this.certDir = certDir;
        return this;
    }

    public String getClientName() {
        return clientName;
    }

    public CertificateAuthorityClient setClientName(String clientName) {
        this.clientName = clientName;
        return this;
    }

    public ArrowheadSecurityContext bootstrap() {
        if (clientName == null) throw new ArrowheadRuntimeException("System name is required to generate " +
                "certificates - have you set \"system_name\" in the config file?");

        // Setting temporary truststore if given (for the secure CA)
        try {
            setSecurityContext(ArrowheadSecurityContext.create(null, null, keyPass, truststore, truststorePass));
        } catch (KeystoreException e) {
            log.error("Failed loading temporary SSL context, are truststore set correctly in your config file?", e);
            setSecurityContext(null);
        }

        // Prepare the data needed to generate the certificate(s)
        String cloudCN = getCloudCN();
        String keyStorePassword = !Utility.isBlank(keystorePass) ? keystorePass : Utility.getRandomPassword();
        String trustStorePassword = !Utility.isBlank(truststorePass) ? truststorePass : Utility.getRandomPassword();
        String commonName = clientName + "." + cloudCN;

        // Obtain signed certificate
        CertificateSigningResponse signingResponse = getSignedCertificate(commonName);

        // Create the key- and truststore
        final KeyStore keyStore = SecurityUtils.createKeyStore(commonName, signingResponse, keyStorePassword.toCharArray());
        final KeyStore trustStore = SecurityUtils.createTrustStore(cloudCN, signingResponse, trustStorePassword.toCharArray());

        // New filenames
        final String newKeystore = clientName + ".p12";
        final String newTruststore = "truststore.p12";

        // Save the keystores to file
        SecurityUtils.saveKeyStoreToFile(keyStore, keyStorePassword.toCharArray(), newKeystore, certDir);
        SecurityUtils.saveKeyStoreToFile(trustStore, trustStorePassword.toCharArray(), newTruststore, certDir);

        final String certPath = certDir != null ? certDir + File.separator : "";

        // Get authorization public key if requested
        final String authFile = "authorization.pub";
        final PublicKey publicKey = getAuthorizationPublicKeyFromCa();
        SecurityUtils.savePEM(publicKey, certPath + authFile);

        // Update app.conf with the new values
        ArrowheadProperties
                .load(confDir + File.separator + "app.conf")
                .setKeystore(newKeystore)
                .setKeystorePass(keyStorePassword)
                .setKeyPass(keyStorePassword)
                .setTruststore(newTruststore)
                .setTruststorePass(trustStorePassword)
                .setAuthKey(authFile)
                .storeToFile(confDir + File.separator + "app.conf");

        try {
            return ArrowheadSecurityContext.create(
                    certPath + newKeystore, keyStorePassword,
                    keyStorePassword,
                    certPath + newTruststore, truststorePass);
        } catch (KeystoreException e) {
            throw new AuthException("Failed to create security context, based on the new key-/truststores.");
        }
    }

    /**
     Gets the Cloud Common Name from the Certificate Authority Core System, proper URL is read from the config file
     */
    private String getCloudCN() {
        Response caResponse = sendRequest(Method.GET);
        return caResponse.readEntity(String.class);
    }

    /**
     Authorization Public Key is used by ArrowheadProviders to verify the signatures by the Authorization Core System in secure mode
     */
    private PublicKey getAuthorizationPublicKeyFromCa() {
        Response caResponse = sendRequest(Method.GET, "auth", null);
        return SecurityUtils.getPublicKey(caResponse.readEntity(String.class), false);
    }

    private CertificateSigningResponse getSignedCertificate(String commonName) {
        //Get a new locally generated public/private key pair
        KeyPair keyPair = SecurityUtils.generateRSAKeyPair();
        final CertificateSigningRequest request = SecurityUtils.createSigningRequest(commonName, keyPair);
        Response caResponse = sendRequest(Method.POST, request);
        CertificateSigningResponse signingResponse = caResponse.readEntity(CertificateSigningResponse.class);
        signingResponse.setLocalPrivateKey(keyPair.getPrivate());
        return signingResponse;
    }

}
