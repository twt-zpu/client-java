package eu.arrowhead.common.api;

import eu.arrowhead.common.api.clients.core.CertificateAuthorityClient;
import eu.arrowhead.common.exception.AuthException;
import eu.arrowhead.common.exception.KeystoreException;
import eu.arrowhead.common.misc.ArrowheadProperties;
import eu.arrowhead.common.misc.SecurityUtils;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.Base64;
import javax.net.ssl.SSLContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.grizzly.ssl.SSLContextConfigurator;

/**
 * Arrowhead security context. You will probably need one of these if you plan to support secure TLS mode in your
 * services, and thus is taken as parameter by many of the other API classes.
 *
 * To create one, just call one of the static create*() methods in this class.
 */
public class ArrowheadSecurityContext {
    private static final Logger LOG = LogManager.getLogger(ArrowheadSecurityContext.class);
    protected final Logger log = LogManager.getLogger(getClass());
    private String keystoreFile, keystorePass, keyPass, truststoreFile, truststorePass, authKey;
    private SSLContext sslContext;
    private SSLContextConfigurator sslContextConfigurator;
    private KeyStore keyStore;

    /**
     * Create a security context using the contents from the default properties files. No certificate bootstrapping will
     * take place.
     * @return your shiny new security context.
     */
    public static ArrowheadSecurityContext createFromProperties() {
        return createFromProperties(ArrowheadProperties.loadDefault(), false);
    }

    /**
     * Create a security context using the contents from the default properties files.
     * @param bootstrap Attempt to perform certificate bootstrapping if the key stores are not valid?
     * @return your shiny new security context.
     */
    public static ArrowheadSecurityContext createFromProperties(boolean bootstrap) {
        return createFromProperties(ArrowheadProperties.loadDefault(), bootstrap);
    }

    /**
     * Create a security context using the contents from the given properties object.
     * @param props The properties object to use.
     * @return your shiny new security context.
     */
    public static ArrowheadSecurityContext createFromProperties(ArrowheadProperties props) {
        return createFromProperties(props, false);
    }

    /**
     * Create a security context using the contents from the given properties object.
     * @param props The properties object to use.
     * @param bootstrap Attempt to perform certificate bootstrapping if the key stores are not valid?
     * @return your shiny new security context.
     */
    public static ArrowheadSecurityContext createFromProperties(ArrowheadProperties props, boolean bootstrap) {
        final boolean secure = props.isSecure();
        if (!secure) return null;

        try {
            return create(props.getKeystore(), props.getKeystorePass(), props.getKeyPass(), props.getTruststore(),
                    props.getTruststorePass(), props.getAuthKey());
        } catch (KeystoreException e) {
            if (bootstrap) {
                LOG.info("Bootstrapping certificates...", e);
                return CertificateAuthorityClient.createFromProperties(props).bootstrap();
            } else {
                throw new AuthException("Creating security context failed", e);
            }
        }
    }

    /**
     * Create a security context, by manually supplying key stores and passwords.
     * @param keystore The key store.
     * @param keystorePass The password to the key store.
     * @param keyPass The password to the key.
     * @param truststore The trust store.
     * @param truststorePass The password to the trust store.
     * @param authKey
     * @return your shiny new security context.
     * @throws KeystoreException If loading the stores failed. You will have to interact manually with the
     * CertificateAuthorityClient, if you want to perform bootstrapping as this stage.
     */
    public static ArrowheadSecurityContext create(String keystore, String keystorePass, String keyPass,
                                                  String truststore, String truststorePass, String authKey)
            throws KeystoreException {
        return new ArrowheadSecurityContext()
                .setKeystoreFile(keystore)
                .setKeystorePass(keystorePass)
                .setKeyPass(keyPass)
                .setTruststoreFile(truststore)
                .setTruststorePass(truststorePass)
                .setAuthKey(authKey)
                .load();
    }

    /**
     * Internal function for loading everything.
     * @return this.
     * @throws KeystoreException if loading fails.
     */
    private ArrowheadSecurityContext load() throws KeystoreException {
        sslContextConfigurator = new SSLContextConfigurator();
        if (keystoreFile != null) {
            log.info("Using keystore: " + keystoreFile);
            sslContextConfigurator.setKeyStoreFile(keystoreFile);
        }
        if (keystorePass != null) {
            sslContextConfigurator.setKeyStorePass(keystorePass);
        }
        if (keyPass != null) {
            sslContextConfigurator.setKeyPass(keyPass);
        }
        if (truststoreFile != null) {
            log.info("Using truststore: " + truststoreFile);
            sslContextConfigurator.setTrustStoreFile(truststoreFile);
        }
        if (truststorePass != null) {
            sslContextConfigurator.setTrustStorePass(truststorePass);
        }

        try {
            sslContext = sslContextConfigurator.createSSLContext(true);
        } catch (SSLContextConfigurator.GenericStoreException e) {
            throw new KeystoreException("Provided SSLContext is not valid", e);
        }

        if (keystoreFile != null) {
            keyStore = SecurityUtils.loadKeyStore(keystoreFile, keystorePass);
            X509Certificate serverCert = SecurityUtils.getFirstCertFromKeyStore(keyStore);
            String base64PublicKey = Base64.getEncoder().encodeToString(serverCert.getPublicKey().getEncoded());
            log.info("PublicKey Base64: " + base64PublicKey);
        }

        return this;
    }

    /**
     * Private constructor, use one of the static create* methods instead.
     */
    private ArrowheadSecurityContext() {
    }

    public String getKeystoreFile() {
        return keystoreFile;
    }

    public ArrowheadSecurityContext setKeystoreFile(String keystoreFile) {
        this.keystoreFile = keystoreFile;
        return this;
    }

    public String getKeystorePass() {
        return keystorePass;
    }

    public ArrowheadSecurityContext setKeystorePass(String keystorePass) {
        this.keystorePass = keystorePass;
        return this;
    }

    public String getKeyPass() {
        return keyPass;
    }

    public ArrowheadSecurityContext setKeyPass(String keyPass) {
        this.keyPass = keyPass;
        return this;
    }

    public String getTruststoreFile() {
        return truststoreFile;
    }

    public ArrowheadSecurityContext setTruststoreFile(String truststoreFile) {
        this.truststoreFile = truststoreFile;
        return this;
    }

    public String getTruststorePass() {
        return truststorePass;
    }

    public ArrowheadSecurityContext setTruststorePass(String truststorePass) {
        this.truststorePass = truststorePass;
        return this;
    }

    public SSLContext getSslContext() {
        return sslContext;
    }

    public SSLContextConfigurator getSSLContextConfigurator() {
        return sslContextConfigurator;
    }

    public KeyStore getKeyStore() {
        return keyStore;
    }

    public PrivateKey getPrivateKey() {
        return SecurityUtils.getPrivateKey(keyStore, keyPass);
    }

    public String getAuthKey() {
        return authKey;
    }

    public ArrowheadSecurityContext setAuthKey(String authKey) {
        this.authKey = authKey;
        return this;
    }

    public PublicKey getPublicAuthKey() {
        return authKey != null && Files.exists(Paths.get(authKey)) ? SecurityUtils.getPublicKey(authKey) : null;

    }
}
