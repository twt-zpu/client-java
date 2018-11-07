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
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.net.ssl.SSLContext;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.io.File;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.PublicKey;
import java.security.Security;
import java.util.HashMap;
import java.util.Map;

public final class CertificateAuthorityClient extends RestClient {
    private String keyPass, truststore, truststorePass, keystorePass;
    private String cloudCnUri, authPubKeyUri, certSignUri;
    private String confDir, certDir;
    private String clientName;
    private boolean isSecure;

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    public static CertificateAuthorityClient createFromProperties() {
        return createFromProperties(Utility.getProp());
    }

    public static CertificateAuthorityClient createFromProperties(ArrowheadProperties props) {
        final boolean isSecure = props.isSecure();
        return new CertificateAuthorityClient()
                .setSecure(isSecure)
                .setAddress(props.getCaAddress())
                .setPort(props.getCaPort())
                .setKeyPass(props.getKeyPass())
                .setTruststore(props.getTruststore())
                .setTruststorePass(props.getTruststorePass())
                .setKeystorePass(props.getKeystorePass())
                .setConfDir(ArrowheadProperties.getConfDir())
                .setCertDir(props.getCertDir())
                .setClientName(props.getSystemName());
    }

    public static CertificateAuthorityClient createDefault(String clientName) {
        final boolean isSecure = ArrowheadProperties.getDefaultIsSecure();
        return new CertificateAuthorityClient()
                .setSecure(isSecure)
                .setAddress(ArrowheadProperties.getDefaultCaAddress())
                .setPort(ArrowheadProperties.getDefaultCaPort(isSecure))
                .setConfDir(ArrowheadProperties.getConfDir())
                .setCertDir(ArrowheadProperties.getDefaultCertDir())
                .setClientName(clientName);
    }

    private CertificateAuthorityClient() {
        super("0.0.0.0", 80);
        isSecure = false;
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

    public boolean isSecure() {
        return isSecure;
    }

    public CertificateAuthorityClient setSecure(boolean secure) {
        isSecure = secure;
        updateUris();
        return this;
    }

    @Override
    public CertificateAuthorityClient setAddress(String address) {
        super.setAddress(address);
        updateUris();
        return this;
    }

    @Override
    public CertificateAuthorityClient setPort(Integer port) {
        super.setPort(port);
        updateUris();
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

    private void updateUris() {
        String baseUri = Utility.getUri(getAddress(), getPort(), "ca", isSecure, false);
        cloudCnUri = baseUri;
        certSignUri = baseUri;
        authPubKeyUri = UriBuilder.fromPath(baseUri).path("auth").toString();
    }

    // TODO Can we skip the needAuth parameter?, Thomas
    public ArrowheadSecurityContext bootstrap(boolean needAuth) {
        if (!Utility.isHostAvailable(getAddress(), getPort(), 3000)) {
            throw new ArrowheadRuntimeException("CA Core System is unavailable at " + getAddress() + ":" + getPort());
        }

        if (clientName == null) throw new ArrowheadRuntimeException("System name is required to generate " +
                "certificates - have you set \"system_name\" in the config file?");

        // Setting temporary truststore if given (for the secure CA)
        try {
            setSecurityContext(ArrowheadSecurityContext.create(null, null, keyPass, truststore, truststorePass));
        } catch (KeystoreException e) {
            log.error("Failed loading temporary SSL context, are truststore set correctly in your config file?", e);
            setSecurityContext(null);
        }
        SSLContext sslContext = getSecurityContext() != null ? getSecurityContext().getSslContext() : null;

        // Prepare the data needed to generate the certificate(s)
        String cloudCN = getCloudCN(sslContext);
        String keyStorePassword = !Utility.isBlank(keystorePass) ? keystorePass : Utility.getRandomPassword();
        String trustStorePassword = !Utility.isBlank(truststorePass) ? truststorePass : Utility.getRandomPassword();
        String commonName = clientName + "." + cloudCN;

        // Obtain signed certificate
        CertificateSigningResponse signingResponse = getSignedCertificate(commonName, sslContext);

        // Create the key- and truststore
        final KeyStore keyStore = SecurityUtils.createKeyStore(commonName, signingResponse, keyStorePassword.toCharArray());
        final KeyStore trustStore = SecurityUtils.createTrustStore(cloudCN, signingResponse, trustStorePassword.toCharArray());

        // New filenames
        final String newKeystore = clientName + ".p12";
        final String newTruststore = "truststore.p12";

        // Save the keystores to file
        SecurityUtils.saveKeyStoreToFile(keyStore, keyStorePassword.toCharArray(), newKeystore, certDir);
        SecurityUtils.saveKeyStoreToFile(trustStore, trustStorePassword.toCharArray(), newTruststore, certDir);

        // Get authorization public key if requested
        final String authFile = "authorization.pub";
        if (needAuth) {
            final PublicKey publicKey = getAuthorizationPublicKeyFromCa(sslContext);
            SecurityUtils.savePEM(publicKey, (certDir != null ? certDir + File.separator : "") + authFile);
        }

        // Update app.conf with the new values
        Map<String, String> secureParameters = new HashMap<>();
        secureParameters.put("keystore", newKeystore);
        secureParameters.put("keystorepass", keyStorePassword);
        secureParameters.put("keypass", keyStorePassword);
        secureParameters.put("truststore", newTruststore);
        secureParameters.put("truststorepass", trustStorePassword);
        if (needAuth) {
            secureParameters.put("auth_pub", authFile);
        }
        Utility.updateConfigurationFiles(confDir + File.separator + "app.conf", secureParameters);

        try {
            return ArrowheadSecurityContext.create(newKeystore, keyStorePassword, keyStorePassword, newTruststore, truststorePass);
        } catch (KeystoreException e) {
            throw new AuthException("Failed to create security context, based on the new key-/truststores.");
        }
    }

    /**
     Gets the Cloud Common Name from the Certificate Authority Core System, proper URL is read from the config file
     * @param sslContext
     */
    private String getCloudCN(SSLContext sslContext) {
        Response caResponse = sendRequest(cloudCnUri, "GET", null);
        return caResponse.readEntity(String.class);
    }

    /**
     Authorization Public Key is used by ArrowheadProviders to verify the signatures by the Authorization Core System in secure mode
     * @param sslContext
     */
    private PublicKey getAuthorizationPublicKeyFromCa(SSLContext sslContext) {
        Response caResponse = sendRequest(authPubKeyUri, "GET", null);
        return SecurityUtils.getPublicKey(caResponse.readEntity(String.class), false);
    }

    private CertificateSigningResponse getSignedCertificate(String commonName, SSLContext sslContext) {
        //Get a new locally generated public/private key pair
        KeyPair keyPair = SecurityUtils.generateRSAKeyPair();
        final CertificateSigningRequest request = SecurityUtils.createSigningRequest(commonName, keyPair);
        Response caResponse = sendRequest(certSignUri, "POST", request);
        CertificateSigningResponse signingResponse = caResponse.readEntity(CertificateSigningResponse.class);
        signingResponse.setLocalPrivateKey(keyPair.getPrivate());
        return signingResponse;
    }

}
