package eu.arrowhead.common.api.clients;

import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.misc.ArrowheadProperties;
import eu.arrowhead.common.misc.SecurityUtils;
import eu.arrowhead.common.misc.Utility;
import eu.arrowhead.common.model.ArrowheadSystem;
import eu.arrowhead.common.model.CertificateSigningRequest;
import eu.arrowhead.common.model.CertificateSigningResponse;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.glassfish.grizzly.ssl.SSLContextConfigurator;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.io.*;
import java.security.*;
import java.util.HashMap;
import java.util.Map;

public final class CertificateAuthorityClient extends ArrowheadSystem {
    private String keyPass, truststore, truststorePass, keystorePass;
    private String cloudCnUri, authPubKeyUri, certSignUri;
    private boolean isSecure;

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    public static CertificateAuthorityClient createFromProperties() {
        return createFromProperties(Utility.getProp());
    }

    public static CertificateAuthorityClient createFromProperties(ArrowheadProperties props) {
        final boolean isSecure = ArrowheadProperties.getDefaultIsSecure();
        return new CertificateAuthorityClient()
                .setSecure(isSecure)
                .setAddress(props.getCaAddress())
                .setPort(props.getCaPort())
                .setKeyPass(props.getKeyPass())
                .setTruststore(props.getTruststore())
                .setTruststorePass(props.getTruststorePass())
                .setKeystorePass(props.getKeystorePass());
    }

    public static CertificateAuthorityClient createDefault() {
        final boolean isSecure = ArrowheadProperties.getDefaultIsSecure();
        return new CertificateAuthorityClient()
                .setSecure(isSecure)
                .setAddress(ArrowheadProperties.getDefaultCaAddress())
                .setPort(ArrowheadProperties.getDefaultCaPort(isSecure));
    }

    private CertificateAuthorityClient() {
        super(null, "0.0.0.0", 80, null);
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

    private void updateUris() {
        String baseUri = Utility.getUri(getAddress(), getPort(), "ca", isSecure, false);
        cloudCnUri = baseUri;
        certSignUri = baseUri;
        authPubKeyUri = UriBuilder.fromPath(baseUri).path("auth").toString();
    }

    public SSLContextConfigurator bootstrap(String systemName, boolean needAuth) {
        if (!Utility.isHostAvailable(getAddress(), getPort(), 3000)) {
            throw new ArrowheadException("CA Core System is unavailable at " + getAddress() + ":" + getPort());
        }

        if (systemName == null) throw new ArrowheadException("System name is required to generate " +
                "certificates - have you set \"system_name\" in the config file?");

        String certPathPrefix = "config"; // TODO This should be more flexible, Thomas

        // Setting temporary truststore if given (for the secure CA)
        Utility.setSSLContext(null, null, keyPass, truststore, truststorePass, false);

        // Prepare the data needed to generate the certificate(s)
        String cloudCN = getCloudCN();
        String keyStorePassword = !Utility.isBlank(keystorePass) ? keystorePass : Utility.getRandomPassword();
        String trustStorePassword = !Utility.isBlank(truststorePass) ? truststorePass : Utility.getRandomPassword();
        String commonName = systemName + "." + cloudCN;

        // Obtain signed certificate
        CertificateSigningResponse signingResponse = getSignedCertificate(commonName);

        // Create the key- and truststore
        final KeyStore keyStore = SecurityUtils.createKeyStore(commonName, signingResponse, keyStorePassword.toCharArray());
        final KeyStore trustStore = SecurityUtils.createTrustStore(cloudCN, signingResponse, trustStorePassword.toCharArray());

        // Save the keystores to file
        SecurityUtils.saveKeyStoreToFile(keyStore, keyStorePassword.toCharArray(), systemName + ".p12", certPathPrefix);
        SecurityUtils.saveKeyStoreToFile(trustStore, trustStorePassword.toCharArray(), "truststore.p12", certPathPrefix);

        // Get authorization public key if requested
        final String authFile = certPathPrefix + File.separator + "authorization.pub";
        if (needAuth) {
            final PublicKey publicKey = getAuthorizationPublicKeyFromCa();
            SecurityUtils.savePEM(publicKey, authFile);
        }

        // Update app.conf with the new values
        Map<String, String> secureParameters = new HashMap<>();
        secureParameters.put("keystore", certPathPrefix + File.separator + systemName + ".p12");
        secureParameters.put("keystorepass", keyStorePassword);
        secureParameters.put("keypass", keyStorePassword);
        secureParameters.put("truststore", certPathPrefix + File.separator + "truststore.p12");
        secureParameters.put("truststorepass", trustStorePassword);
        if (needAuth) {
            secureParameters.put("auth_pub", authFile);
        }
        Utility.updateConfigurationFiles("config" + File.separator + "app.conf", secureParameters);

        return Utility.setSSLContext(
                certPathPrefix + File.separator + systemName + ".p12",
                keyStorePassword,
                keyStorePassword,
                certPathPrefix + File.separator + "truststore.p12",
                trustStorePassword,
                true);
    }

    /**
     Gets the Cloud Common Name from the Certificate Authority Core System, proper URL is read from the config file
     */
    private String getCloudCN() {
        Response caResponse = Utility.sendRequest(cloudCnUri, "GET", null);
        return caResponse.readEntity(String.class);
    }

    /**
     Authorization Public Key is used by ArrowheadProviders to verify the signatures by the Authorization Core System in secure mode
     */
    private PublicKey getAuthorizationPublicKeyFromCa() {
        Response caResponse = Utility.sendRequest(authPubKeyUri, "GET", null);
        return SecurityUtils.getPublicKey(caResponse.readEntity(String.class), false);
    }

    private CertificateSigningResponse getSignedCertificate(String commonName) {
        //Get a new locally generated public/private key pair
        KeyPair keyPair = SecurityUtils.generateRSAKeyPair();
        final CertificateSigningRequest request = SecurityUtils.createSigningRequest(commonName, keyPair);
        Response caResponse = Utility.sendRequest(certSignUri, "POST", request);
        CertificateSigningResponse signingResponse = caResponse.readEntity(CertificateSigningResponse.class);
        signingResponse.setLocalPrivateKey(keyPair.getPrivate());
        return signingResponse;
    }

}
