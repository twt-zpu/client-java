package eu.arrowhead.common.api;

import eu.arrowhead.common.api.clients.CertificateAuthorityClient;
import eu.arrowhead.common.exception.AuthException;
import eu.arrowhead.common.exception.KeystoreException;
import eu.arrowhead.common.misc.ArrowheadProperties;
import eu.arrowhead.common.misc.SecurityUtils;
import org.apache.log4j.Logger;
import org.glassfish.grizzly.ssl.SSLContextConfigurator;

import javax.net.ssl.SSLContext;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.Base64;

public class ArrowheadSecurityContext {
    private static final Logger LOG = Logger.getLogger(ArrowheadSecurityContext.class);
    protected final Logger log = Logger.getLogger(getClass());
    private String keystore, keystorePass, keyPass, truststore, truststorePass;
    private SSLContext sslContext;
    private SSLContextConfigurator sslContextConfigurator;

    public static ArrowheadSecurityContext createFromProperties() {
        return createFromProperties(ArrowheadProperties.loadDefault(), false);
    }

    public static ArrowheadSecurityContext createFromProperties(boolean bootstrap) {
        return createFromProperties(ArrowheadProperties.loadDefault(), bootstrap);
    }

    public static ArrowheadSecurityContext createFromProperties(ArrowheadProperties props) {
        return createFromProperties(props, false);
    }

    public static ArrowheadSecurityContext createFromProperties(ArrowheadProperties props, boolean bootstrap) {
        final boolean secure = props.isSecure();
        if (!secure) LOG.warn("Trying to create a Security Context, but secure=false in config file");

        try {
            return new ArrowheadSecurityContext()
                    .setKeystore(props.getKeystore())
                    .setKeystorePass(props.getKeystorePass())
                    .setKeyPass(props.getKeyPass())
                    .setTruststore(props.getTruststore())
                    .setTruststorePass(props.getTruststorePass())
                    .load();
        } catch (KeystoreException e) {
            if (secure && bootstrap) {
                LOG.info("Bootstrapping certificates...");
                return CertificateAuthorityClient.createFromProperties(props).bootstrap();
            } else {
                throw new AuthException("Creating security context failed", e);
            }
        }
    }

    public static ArrowheadSecurityContext create(String keystore, String keystorePass, String keyPass, String truststore, String truststorePass) throws KeystoreException {
        return new ArrowheadSecurityContext()
                .setKeystore(keystore)
                .setKeystorePass(keystorePass)
                .setKeyPass(keyPass)
                .setTruststore(truststore)
                .setTruststorePass(truststorePass)
                .load();
    }

    private ArrowheadSecurityContext load() throws KeystoreException {
        sslContextConfigurator = new SSLContextConfigurator();
        if (keystore != null) sslContextConfigurator.setKeyStoreFile(keystore);
        if (keystorePass != null) sslContextConfigurator.setKeyStorePass(keystorePass);
        if (keyPass != null) sslContextConfigurator.setKeyPass(keyPass);
        if (truststore != null) sslContextConfigurator.setTrustStoreFile(truststore);
        if (truststorePass != null) sslContextConfigurator.setTrustStorePass(truststorePass);

        try {
            sslContext = sslContextConfigurator.createSSLContext(true);
        } catch (SSLContextConfigurator.GenericStoreException e) {
            throw new KeystoreException("Provided SSLContext is not valid", e);
        }

        if (keystore != null) {
            KeyStore keyStore = SecurityUtils.loadKeyStore(keystore, keystorePass);
            X509Certificate serverCert = SecurityUtils.getFirstCertFromKeyStore(keyStore);
            String base64PublicKey = Base64.getEncoder().encodeToString(serverCert.getPublicKey().getEncoded());
            log.info("PublicKey Base64: " + base64PublicKey);
        }

        return this;
    }

    private ArrowheadSecurityContext() {
    }

    public String getKeystore() {
        return keystore;
    }

    public ArrowheadSecurityContext setKeystore(String keystore) {
        this.keystore = keystore;
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

    public String getTruststore() {
        return truststore;
    }

    public ArrowheadSecurityContext setTruststore(String truststore) {
        this.truststore = truststore;
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
}
