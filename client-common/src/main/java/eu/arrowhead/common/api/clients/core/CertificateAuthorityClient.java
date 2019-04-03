package eu.arrowhead.common.api.clients.core;

import eu.arrowhead.common.api.ArrowheadConverter;
import eu.arrowhead.common.api.ArrowheadSecurityContext;
import eu.arrowhead.common.api.clients.HttpClient;
import eu.arrowhead.common.api.clients.OrchestrationStrategy;
import eu.arrowhead.common.exception.ArrowheadRuntimeException;
import eu.arrowhead.common.exception.AuthException;
import eu.arrowhead.common.exception.KeystoreException;
import eu.arrowhead.common.misc.ArrowheadProperties;
import eu.arrowhead.common.misc.SecurityUtils;
import eu.arrowhead.common.misc.Utility;
import eu.arrowhead.common.model.CertificateSigningRequest;
import eu.arrowhead.common.model.CertificateSigningResponse;
import java.io.File;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.PublicKey;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A client for interacting with the Certificate Authority system. See the static create* methods for how to get an
 * instance of one of these.
 */
public final class CertificateAuthorityClient extends HttpClient {
    private static final Logger LOG = LogManager.getLogger(CertificateAuthorityClient.class);
    private static final UriBuilder AUTH_URI = UriBuilder.fromPath("auth");
    private String keyPass;
    private String truststore;
    private String truststorePass;
    private String keystorePass;
    private String confDir, certDir;
    private String clientName;

    /**
     * Create a new client from the settings in the default properties files.
     * @return your shiny new client.
     */
    public static CertificateAuthorityClient createFromProperties() {
        return createFromProperties(ArrowheadProperties.loadDefault());
    }

    /**
     * Create a new client from given set of properties.
     * @return your shiny new client.
     */
    public static CertificateAuthorityClient createFromProperties(ArrowheadProperties props) {
        final boolean isSecure = props.isSecure();
        if (!isSecure)
            LOG.warn("Trying to create CertificateAuthorityClient but secure=false in config file");
        return new CertificateAuthorityClient(isSecure, props.getCaAddress(), props.getCaPort(), "ca",
                props.getKeyPass(), props.getTruststore(), props.getTruststorePass())
                .setKeystorePass(props.getKeystorePass())
                .setConfDir(ArrowheadProperties.getConfDir())
                .setCertDir(props.getCertDir())
                .setClientName(props.getSystemName());
    }

    /**
     * Create a new client using default values.
     * @return your shiny new client.
     */
    public static CertificateAuthorityClient createDefault(String clientName) {
        final boolean isSecure = ArrowheadProperties.getDefaultIsSecure();
        return new CertificateAuthorityClient(isSecure, ArrowheadProperties.getDefaultCaAddress(),
                ArrowheadProperties.getDefaultCaPort(isSecure), "ca", null, null, null)
                .setConfDir(ArrowheadProperties.getConfDir())
                .setCertDir(ArrowheadProperties.getDefaultCertDir())
                .setClientName(clientName);
    }

    /**
     * Internal method for creating a temporary security context until we can build the right one.
     * @param keyPass the password for the certificates.
     * @param truststore the trust store.
     * @param truststorePass the password for the trust store.
     * @return the security context.
     */
    private static ArrowheadSecurityContext createSecurityContext(String keyPass, String truststore, String truststorePass) {
        // Setting temporary truststore if given (for the secure CA)
        try {
            return ArrowheadSecurityContext.create(null, null, keyPass, truststore, truststorePass, null);
        } catch (KeystoreException e) {
            LOG.error("Failed loading temporary SSL context, are truststore set correctly in your config file?", e);
            try {
                return ArrowheadSecurityContext.create(null, null, null, null, null, null);
            } catch (KeystoreException e1) {
                throw new AuthException("Failed to create temporary SSL context", e1);
            }
        }
    }

    /**
     * Private construct, see the create* methods.
     * @param secure use secure mode?
     * @param host the host.
     * @param port the port.
     * @param path the path.
     * @param keyPass the password for the certificates.
     * @param truststore the trust store.
     * @param truststorePass the password for the trust store.
     */
    private CertificateAuthorityClient(boolean secure, String host, int port, String path, String keyPass, String truststore, String truststorePass) {
        super(new OrchestrationStrategy.Never(secure, host, port, path, ArrowheadConverter.JSON), createSecurityContext(keyPass, truststore, truststorePass));
        this.keyPass = keyPass;
        this.truststore = truststore;
        this.truststorePass = truststorePass;
    }

    public String getKeyPass() {
        return keyPass;
    }

    public String getTruststore() {
        return truststore;
    }

    public String getTruststorePass() {
        return truststorePass;
    }

    public String getKeystorePass() {
        return keystorePass;
    }

    /**
     * Set a new password for the generated keystore.
     * @param keystorePass the password.
     * @return this.
     */
    public CertificateAuthorityClient setKeystorePass(String keystorePass) {
        this.keystorePass = keystorePass;
        return this;
    }

    public String getConfDir() {
        return confDir;
    }

    /**
     * Set the configuration directory for generating the configuration files.
     * @param confDir the directory.
     * @return this.
     */
    public CertificateAuthorityClient setConfDir(String confDir) {
        this.confDir = confDir;
        return this;
    }

    public String getCertDir() {
        return certDir;
    }

    /**
     * Set the certificate directory for storing the certificates.
     * @param certDir
     * @return this.
     */
    public CertificateAuthorityClient setCertDir(String certDir) {
        this.certDir = certDir;
        return this;
    }

    public String getClientName() {
        return clientName;
    }

    /**
     * Set the name of the system to generate certificates for.
     * @param clientName the name.
     * @return this.
     */
    public CertificateAuthorityClient setClientName(String clientName) {
        this.clientName = clientName;
        return this;
    }

    /**
     * Bootstrap a security context with shiny new certificates. Also updates your configuration files to use the new
     * context the next time around.
     * @return the newly generated security context you should use.
     */
    public ArrowheadSecurityContext bootstrap() {
        if (clientName == null) throw new ArrowheadRuntimeException("System name is required to generate " +
                "certificates - have you set \"system_name\" in the config file?");

        SecurityUtils.addSecurityProvider();

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
                .storeAsApp();

        try {
            return ArrowheadSecurityContext.create(
                    certPath + newKeystore, keyStorePassword,
                    keyStorePassword,
                    certPath + newTruststore, truststorePass, certPath + authFile);
        } catch (KeystoreException e) {
            throw new AuthException("Failed to create security context, based on the new key-/truststores.");
        }
    }

    /**
     * Gets the Cloud Common Name from the Certificate Authority Core System
     * @return the CN.
     */
    private String getCloudCN() {
        return request(Method.GET).readEntity(String.class);
    }

    /**
     * Authorization Public Key is used by ArrowheadProviders to verify the signatures by the Authorization Core System
     * in secure mode.
     * @return the public key.
     */
    private PublicKey getAuthorizationPublicKeyFromCa() {
        Response caResponse = request(Method.GET, AUTH_URI);
        return SecurityUtils.getPublicKey(caResponse.readEntity(String.class), false);
    }

    /**
     * Gets a new signed certificate from the Certificate Authority.
     * @param commonName the common name.
     * @return a {@link CertificateSigningResponse}.
     */
    private CertificateSigningResponse getSignedCertificate(String commonName) {
        //Get a new locally generated public/private key pair
        KeyPair keyPair = SecurityUtils.generateRSAKeyPair();
        final CertificateSigningRequest request = SecurityUtils.createSigningRequest(commonName, keyPair);
        Response caResponse = request(Method.POST, request);
        CertificateSigningResponse signingResponse = caResponse.readEntity(CertificateSigningResponse.class);
        signingResponse.setLocalPrivateKey(keyPair.getPrivate());
        return signingResponse;
    }
}
