package eu.arrowhead.common.api;

import eu.arrowhead.common.api.resources.ArrowheadResource;
import eu.arrowhead.common.exception.ArrowheadRuntimeException;
import eu.arrowhead.common.exception.AuthException;
import eu.arrowhead.common.misc.ArrowheadProperties;
import eu.arrowhead.common.misc.SecurityUtils;
import eu.arrowhead.common.misc.Utility;
import org.apache.log4j.Logger;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.ssl.SSLEngineConfigurator;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.ServerSocket;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.*;

public class ArrowheadServer {
    protected final Logger log = Logger.getLogger(getClass());
    private static final Set<ArrowheadServer> servers = new HashSet<>();
    private HttpServer server;
    private String base64PublicKey;
    private String baseUri;
    private boolean isSecure;
    private String keystore, keystorePass, keyPass, truststore, truststorePass, systemName, address;
    private int port;
    private ArrowheadSecurityContext securityContext;
    private Set<Class<? extends ArrowheadResource>> resources = new HashSet<>();
    private Set<String> packages = new HashSet<>();

    public static ArrowheadServer createFromProperties(ArrowheadSecurityContext securityContext) {
        return createFromProperties(ArrowheadProperties.loadDefault(), securityContext);
    }

    public static ArrowheadServer createFromProperties(ArrowheadProperties props, ArrowheadSecurityContext securityContext) {
        final boolean isSecure = props.isSecure();
        if (isSecure ^ securityContext != null)
            throw new ArrowheadRuntimeException("Both or neither of isSecure and securityContext must be set");
        return new ArrowheadServer()
                .setSecure(isSecure)
                .setKeystore(props.getKeystore())
                .setKeystorePass(props.getKeystorePass())
                .setKeyPass(props.getKeyPass())
                .setTruststore(props.getTruststore())
                .setTruststorePass(props.getTruststorePass())
                .setSystemName(props.getSystemName())
                .setAddress(props.getAddress())
                .setPort(props.getPort())
                .setSecurityContext(securityContext);
    }

    public static ArrowheadServer createDefault(ArrowheadSecurityContext securityContext) {
        final boolean isSecure = ArrowheadProperties.getDefaultIsSecure();
        final String systemName = ArrowheadProperties.createDefaultSystemName();
        return new ArrowheadServer()
                .setSecure(isSecure)
                .setSystemName(systemName)
                .setAddress(ArrowheadProperties.getDefaultAddress())
                .setPort(ArrowheadProperties.getDefaultPort(isSecure))
                .setSecurityContext(securityContext);
    }

    public static void stopAll() {
        servers.forEach(ArrowheadServer::stop);
    }

    private ArrowheadServer() {
        packages.add("eu.arrowhead.common");
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

    public ArrowheadSecurityContext getSecurityContext() {
        return securityContext;
    }

    public ArrowheadServer setSecurityContext(ArrowheadSecurityContext securityContext) {
        this.securityContext = securityContext;
        return this;
    }

    public ArrowheadServer addResources(Class<? extends ArrowheadResource> ... resources) {
        return addResources(Arrays.asList(resources));
    }

    public ArrowheadServer addPackages(String ... packages) {
        return addPackages(Arrays.asList(packages));
    }

    public ArrowheadServer addResources(Collection<? extends Class<? extends ArrowheadResource>> resources) {
        this.resources.addAll(resources);
        return this;
    }

    public ArrowheadServer addPackages(Collection<? extends String> packages) {
        this.packages.addAll(packages);
        return this;
    }

    public ArrowheadServer replacePackages(Set<String> packages) {
        this.packages = packages;
        return this;
    }

    public ArrowheadServer start() {
        if (server != null)
            throw new ArrowheadRuntimeException("Server already started");

        if (isSecure ^ securityContext != null)
            throw new ArrowheadRuntimeException("Both or neither of isSecure and securityContext must be set");

        // Port 9803-9874 is the 4-digit largest port range currently marked as unassigned by IANA
        if (port == 0) port = nextFreePort(9803, 9874);
        baseUri = Utility.getUri(address, port, null, isSecure, true);

        final ResourceConfig config = new ResourceConfig();
        config.packages(packages.toArray(new String[]{}));

        for (Class<? extends ArrowheadResource> resource : resources) {
            try {
                config.registerInstances(resource.getConstructor(ArrowheadServer.class).newInstance(this));
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                throw new ArrowheadRuntimeException(String.format("Class %s must inherit and implement default constructor from ArrowheadResource class", resource.getName()), e);
            }
        }

        SSLEngineConfigurator sslEC = null;
        if (isSecure) {
            KeyStore keyStore = SecurityUtils.loadKeyStore(keystore, keystorePass);
            X509Certificate serverCert = SecurityUtils.getFirstCertFromKeyStore(keyStore);
            String serverCN = SecurityUtils.getCertCNFromSubject(serverCert.getSubjectDN().getName());
            if (!SecurityUtils.isKeyStoreCNArrowheadValid(serverCN)) {
                throw new AuthException("Server CN ( " + serverCN + ") is not compliant with the Arrowhead cert" +
                        " structure, since it does not have 5 parts, or does not end with \"arrowhead.eu\".");
            }

            config.property("server_common_name", serverCN);

            base64PublicKey = Base64.getEncoder().encodeToString(serverCert.getPublicKey().getEncoded());

            sslEC = new SSLEngineConfigurator(securityContext.getSSLContextConfigurator()).setClientMode(false).setNeedClientAuth(true);
        }

        config.property("arrowhead_server", this);

        try {
            server = GrizzlyHttpServerFactory.createHttpServer(UriBuilder.fromUri(baseUri).build(), config, isSecure, sslEC, false);
            server.getServerConfiguration().setAllowPayloadForUndefinedHttpMethods(true);
            server.start();
        } catch (IOException | ProcessingException e) {
            throw new ServiceConfigurationError(String.format("Make sure you gave a valid address in the config file! " +
                    "(Assignable to this JVM and not in use already), got %s:%s", address, this.port != 0 ? this.port : "auto"), e);
        }

        servers.add(this);
        log.info("Started " + (isSecure ? "secure" : "insecure") + " server at: " + baseUri);

        return this;
    }

    private int nextFreePort(int from, int to) {
        for (int port = from; port <= to; port++) {
            ServerSocket s = null;
            try {
                s = new ServerSocket(port);
                return port;
            } catch (IOException ignored) {
            } finally {
                try {
                    if (s != null) s.close();
                } catch (IOException e) {
                    throw new ArrowheadRuntimeException("Error occurred during port detection:", e);
                }
            }
        }
        throw new ArrowheadRuntimeException("Unable to find a free port");
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