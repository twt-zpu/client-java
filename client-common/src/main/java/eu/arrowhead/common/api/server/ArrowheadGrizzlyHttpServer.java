package eu.arrowhead.common.api.server;

import eu.arrowhead.common.api.ArrowheadSecurityContext;
import eu.arrowhead.common.exception.ArrowheadRuntimeException;
import eu.arrowhead.common.misc.ArrowheadProperties;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.ServiceConfigurationError;
import java.util.Set;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.UriBuilder;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.ssl.SSLEngineConfigurator;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.logging.LoggingFeature;
import org.glassfish.jersey.server.ResourceConfig;

/**
 * An implementation of {@link ArrowheadHttpServer} using Grizzly
 */
public class ArrowheadGrizzlyHttpServer extends ArrowheadHttpServer {

    private ContainerRequestFilter securityFilter;
    private HttpServer server;
    private Set<Class<? extends ArrowheadResource>> resources = new HashSet<>();
    private Set<String> packages = new HashSet<>();
    private Set<Object> instances = new HashSet<>();
    private boolean needClientAuth = true;

    /**
     * Create an instance using the default properties files.
     * @param securityContext the security context to use, see {@link ArrowheadSecurityContext} for how to create one.
     * @return your shiny new http server.
     */
    public static ArrowheadGrizzlyHttpServer createFromProperties(ArrowheadSecurityContext securityContext) {
        return createFromProperties(ArrowheadProperties.loadDefault(), securityContext);
    }

    /**
     * Create an instance using the given properties object.
     * @param props the properties to use.
     * @param securityContext the security context to use, see {@link ArrowheadSecurityContext} for how to create one.
     * @return your shiny new http server.
     */
    public static ArrowheadGrizzlyHttpServer createFromProperties(ArrowheadProperties props,
                                                                  ArrowheadSecurityContext securityContext) {
        final boolean isSecure = props.isSecure();
        return new ArrowheadGrizzlyHttpServer(isSecure, props.getAddress(), props.getPort(), securityContext);
    }

    /**
     * Create an instance using default settings.
     * @param securityContext the security context to use, see {@link ArrowheadSecurityContext} for how to create one.
     * @return your shiny new http server.
     */
    public static ArrowheadGrizzlyHttpServer createDefault(ArrowheadSecurityContext securityContext) {
        final boolean isSecure = ArrowheadProperties.getDefaultIsSecure();
        return new ArrowheadGrizzlyHttpServer(
                isSecure,
                ArrowheadProperties.getDefaultAddress(),
                ArrowheadProperties.getDefaultPort(isSecure),
                securityContext);
    }

    /**
     * Private constructor, use one of the static create* methods instead.
     * @param isSecure create a secure instance?
     * @param address address of the server.
     * @param port port of the server (0 means auto detection).
     * @param securityContext security context for the server.
     */
    private ArrowheadGrizzlyHttpServer(boolean isSecure, String address, int port,
                                       ArrowheadSecurityContext securityContext) {
        super(isSecure, address, port, securityContext);
        if (isSecure)
            securityFilter = new ArrowheadTokenSignatureSecurityFilter(securityContext);
        packages.add("eu.arrowhead.common");
    }

    /**
     * Add resource classes to the server. Resources are currently treated as singleton classes and only initialised
     * once before the server is started and added to the server as pure objects.
     * @param resources resource classes to add.
     * @return this.
     */
    @SafeVarargs
    public final ArrowheadGrizzlyHttpServer addResources(Class<? extends ArrowheadResource>... resources) {
        return addResources(Arrays.asList(resources));
    }

    /**
     * Add resource classes to the server. Resources are currently treated as singleton classes and only initialised
     * once before the server is started and added to the server as pure objects.
     * @param resources resource classes to add.
     * @return this.
     */
    public ArrowheadGrizzlyHttpServer addResources(Collection<? extends Class<? extends ArrowheadResource>> resources) {
        this.resources.addAll(resources);
        return this;
    }

    /**
     * Add packages to the server. The package "eu.arrowhead.common" is added by default, see replacePackages() on how
     * to change that.
     * @param packages packages to add.
     * @return this.
     */
    public ArrowheadGrizzlyHttpServer addPackages(String ... packages) {
        return addPackages(Arrays.asList(packages));
    }

    /**
     * Add packages to the server. The package "eu.arrowhead.common" is added by default, see replacePackages() on how
     * to change that.
     * @param packages packages to add.
     * @return this.
     */
    public ArrowheadGrizzlyHttpServer addPackages(Collection<? extends String> packages) {
        this.packages.addAll(packages);
        return this;
    }

    /**
     * Replace packages in the server.
     * @param packages packages to replace with.
     * @return this.
     */
    public ArrowheadGrizzlyHttpServer replacePackages(Set<String> packages) {
        this.packages = packages;
        return this;
    }

    /**
     * Replace packages in the server.
     * @param packages packages to replace with.
     * @return this.
     */
    public ArrowheadGrizzlyHttpServer replacePackages(String ... packages) {
        this.packages.clear();
        addPackages(Arrays.asList(packages));
        return this;
    }

    /**
     * Add instances to the server. The package "eu.arrowhead.common" is added by default, see replaceInstances() on how
     * to change that.
     * @param instances instances to add.
     * @return this.
     */
    public ArrowheadGrizzlyHttpServer addInstances(Object ... instances) {
        return addInstances(Arrays.asList(instances));
    }

    /**
     * Add instances to the server. The package "eu.arrowhead.common" is added by default, see replaceInstances() on how
     * to change that.
     * @param instances instances to add.
     * @return this.
     */
    public ArrowheadGrizzlyHttpServer addInstances(Collection<Object> instances) {
        this.instances.addAll(instances);
        return this;
    }

    /**
     * Replace instances in the server.
     * @param instances instances to replace with.
     * @return this.
     */
    public ArrowheadGrizzlyHttpServer replaceInstances(Set<Object> instances) {
        this.instances = instances;
        return this;
    }

    /**
     * Replace instances in the server.
     * @param instances instances to replace with.
     * @return this.
     */
    public ArrowheadGrizzlyHttpServer replaceInstances(Object ... instances) {
        this.instances.clear();
        addInstances(Arrays.asList(instances));
        return this;
    }

    public ContainerRequestFilter getSecurityFilter() {
        return securityFilter;
    }

    public ArrowheadGrizzlyHttpServer setSecurityFilter(ContainerRequestFilter securityFilter) {
        this.securityFilter = securityFilter;
        return this;
    }

    public boolean isNeedClientAuth() {
        return needClientAuth;
    }

    public ArrowheadGrizzlyHttpServer setNeedClientAuth(boolean needClientAuth) {
        this.needClientAuth = needClientAuth;
        return this;
    }

    /**
     * Implemented start routine
     */
    @Override
    protected void onStart() {
        final ResourceConfig config = new ResourceConfig();
        config.packages(packages.toArray(new String[]{}));
        config.register(new LoggingFeature(
            org.apache.logging.log4j.jul.LogManager.getLogManager().getLogger(this.getClass().getName()),
            LoggingFeature.Verbosity.PAYLOAD_ANY
        ));

        if (securityFilter != null) instances.add(securityFilter);
        config.registerInstances(instances);

        for (Class<? extends ArrowheadResource> resource : resources) {
            try {
                config.registerInstances(resource.getConstructor(ArrowheadHttpServer.class).newInstance(this));
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                    NoSuchMethodException e) {
                throw new ArrowheadRuntimeException(String.format("Class %s must inherit and implement default " +
                        "constructor from ArrowheadResource class", resource.getName()), e);
            }
        }

        SSLEngineConfigurator sslEC = null;
        if (isSecure()) {
            config.property("server_common_name", getCN());

            sslEC = new SSLEngineConfigurator(getSecurityContext().getSSLContextConfigurator())
                    .setClientMode(false)
                    .setNeedClientAuth(needClientAuth);
        }

        config.property("arrowhead_server", this);

        try {
            server = GrizzlyHttpServerFactory.createHttpServer(
                    UriBuilder.fromUri(getBaseUri()).build(),
                    config, isSecure(), sslEC, false);
            server.getServerConfiguration().setAllowPayloadForUndefinedHttpMethods(true);
            server.start();
        } catch (IOException | ProcessingException e) {
            throw new ServiceConfigurationError(String.format("Make sure you gave a valid address in the config file!" +
                            " (Assignable to this JVM and not in use already), got %s:%s",
                    getAddress(), getPort() != 0 ? getPort() : "auto"), e);
        }
    }

    /**
     * Implemented stop routine
     */
    @Override
    protected void onStop() {
        if (server != null) {
            server.shutdownNow();
            server = null;
        }
    }

}