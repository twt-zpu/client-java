package eu.arrowhead.common.api;

import eu.arrowhead.common.clients.CertificateAuthorityClient;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.AuthException;
import eu.arrowhead.common.misc.ArrowheadProperties;
import eu.arrowhead.common.misc.SecurityUtils;
import eu.arrowhead.common.misc.Utility;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.ssl.SSLContextConfigurator;
import org.glassfish.grizzly.ssl.SSLEngineConfigurator;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.net.URI;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.ServiceConfigurationError;
import java.util.Set;
import java.util.HashSet;

public class ArrowheadServer {
    private static Set<ArrowheadServer> servers = new HashSet<>();
    private HttpServer server;
    private String base64PublicKey;
    private String baseUri;
    private boolean isSecure;
    private String keystore, keystorePass, keyPass, truststore, truststorePass, systemName, address;
    private int port;

    public static ArrowheadServer createFromProperties() {
        return createFromProperties(Utility.getProp());
    }

    public static ArrowheadServer createFromProperties(ArrowheadProperties props) {
        final boolean isSecure = props.isSecure();
        return new ArrowheadServer()
                .setSecure(isSecure)
                .setKeystore(props.getKeystore())
                .setKeystorePass(props.getKeystorePass())
                .setKeyPass(props.getKeyPass())
                .setTruststore(props.getTruststore())
                .setTruststorePass(props.getTruststorePass())
                .setSystemName(props.getSystemName())
                .setAddress(props.getaddress())
                .setPort(props.getPort());
    }

    public static ArrowheadServer createDefault() {
        final boolean isSecure = ArrowheadProperties.getDefaultIsSecure();
        final String systemName = ArrowheadProperties.createDefaultSystemName();
        return new ArrowheadServer()
                .setSecure(isSecure)
                .setSystemName(systemName)
                .setAddress(ArrowheadProperties.getDefaultAddress())
                .setPort(ArrowheadProperties.getDefaultPort(isSecure));
    }

    public static void stopAll() {
        servers.forEach(ArrowheadServer::stop);
    }

    private ArrowheadServer() {
    }

    public boolean isSecure() {
        return isSecure;
    }

    public ArrowheadServer setSecure(boolean secure) {
        isSecure = secure;
        return this;
    }

    public String getKeystore() {
        return keystore;
    }

    public ArrowheadServer setKeystore(String keystore) {
        this.keystore = keystore;
        return this;
    }

    public String getKeystorePass() {
        return keystorePass;
    }

    public ArrowheadServer setKeystorePass(String keystorePass) {
        this.keystorePass = keystorePass;
        return this;
    }

    public String getKeyPass() {
        return keyPass;
    }

    public ArrowheadServer setKeyPass(String keyPass) {
        this.keyPass = keyPass;
        return this;
    }

    public String getTruststore() {
        return truststore;
    }

    public ArrowheadServer setTruststore(String truststore) {
        this.truststore = truststore;
        return this;
    }

    public String getTruststorePass() {
        return truststorePass;
    }

    public ArrowheadServer setTruststorePass(String truststorePass) {
        this.truststorePass = truststorePass;
        return this;
    }

    public String getSystemName() {
        return systemName;
    }

    public ArrowheadServer setSystemName(String systemName) {
        this.systemName = systemName;
        return this;
    }

    public String getAddress() {
        return address;
    }

    public ArrowheadServer setAddress(String address) {
        this.address = address;
        return this;
    }

    public int getPort() {
        return port;
    }

    public ArrowheadServer setPort(int port) {
        this.port = port;
        return this;
    }

    public ArrowheadServer start(Class<?>[] classes, String[] packages) {
        return start(classes, packages, null);
    }

    public ArrowheadServer start(Class<?>[] classes, String[] packages, CertificateAuthorityClient ca) {
        if (server != null)
            throw new ArrowheadException("Server already started");

        final ResourceConfig config = new ResourceConfig();
        config.registerClasses(classes);
        config.packages(packages);

        SSLEngineConfigurator sslEC = null;
        if (isSecure) {
            SSLContextConfigurator sslCon;
            try {
                sslCon = Utility.setSSLContext(
                        keystore,
                        keystorePass,
                        keyPass,
                        truststore,
                        truststorePass,
                        true);
            } catch (AuthException e) {
                if (ca != null) {
                    try {
                        sslCon = ca.bootstrap(systemName, true);
                        // TODO Reloading props like this (and here only) could cause problems in other classes, Thomas
                        ArrowheadProperties props = Utility.getProp();
                        setKeystore(props.getKeystore());
                        setKeystorePass(props.getKeystorePass());
                        setKeyPass(props.getKeyPass());
                        setTruststore(props.getTruststore());
                        setTruststorePass(props.getTruststorePass());
                    } catch (ArrowheadException e2) {
                        throw new AuthException("Certificate bootstrapping failed with: " + e2.getMessage(), e2);
                    }
                } else {
                    throw new AuthException("No certificates available for secure mode: " + e.getMessage(), e);
                }
            }

            KeyStore keyStore = SecurityUtils.loadKeyStore(
                    keystore,
                    keystorePass);
            X509Certificate serverCert = SecurityUtils.getFirstCertFromKeyStore(keyStore);
            String serverCN = SecurityUtils.getCertCNFromSubject(serverCert.getSubjectDN().getName());
            if (!SecurityUtils.isKeyStoreCNArrowheadValid(serverCN)) {
                throw new AuthException("Server CN ( " + serverCN + ") is not compliant with the Arrowhead cert" +
                        " structure, since it does not have 5 parts, or does not end with \"arrowhead.eu\".");
            }

            config.property("server_common_name", serverCN);

            base64PublicKey = Base64.getEncoder().encodeToString(serverCert.getPublicKey().getEncoded());
            System.out.println("Server PublicKey Base64: " + base64PublicKey);

            sslEC = new SSLEngineConfigurator(sslCon).setClientMode(false).setNeedClientAuth(true);
        }

        baseUri = Utility.getUri(address, port, null, isSecure, true);
        final URI uri = UriBuilder.fromUri(baseUri).build();


        try {
            server = GrizzlyHttpServerFactory.createHttpServer(uri, config, isSecure, sslEC, false);
            server.getServerConfiguration().setAllowPayloadForUndefinedHttpMethods(true);
            server.start();
        } catch (IOException | ProcessingException e) {
            throw new ServiceConfigurationError("Make sure you gave a valid address in the config file! " +
                    "(Assignable to this JVM and not in use already)", e);
        }

        servers.add(this);
        System.out.println("Started " + (isSecure ? "secure" : "insecure") + " server at: " + baseUri);

        return this;
    }

    public ArrowheadServer stop() {
        if (server != null) {
            server.shutdownNow();
            server = null;
            servers.remove(this);
        }

        return this;
    }

    public String getBaseUri() {
        return baseUri;
    }

    public String getBase64PublicKey() {
        return base64PublicKey;
    }
}