package eu.arrowhead.common.api;

import eu.arrowhead.common.api.resources.ArrowheadResource;
import eu.arrowhead.common.exception.ArrowheadRuntimeException;
import eu.arrowhead.common.misc.ArrowheadProperties;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.ssl.SSLEngineConfigurator;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class ArrowheadGrizzlyHttpServer extends ArrowheadHttpServer {
    private HttpServer server;
    private Set<Class<? extends ArrowheadResource>> resources = new HashSet<>();
    private Set<String> packages = new HashSet<>();

    public static ArrowheadGrizzlyHttpServer createFromProperties(ArrowheadSecurityContext securityContext) {
        return createFromProperties(ArrowheadProperties.loadDefault(), securityContext);
    }

    public static ArrowheadGrizzlyHttpServer createFromProperties(ArrowheadProperties props,
                                                                  ArrowheadSecurityContext securityContext) {
        final boolean isSecure = props.isSecure();
        if (isSecure ^ securityContext != null)
            throw new ArrowheadRuntimeException("Both or neither of isSecure and securityContext must be set");
        final ArrowheadGrizzlyHttpServer server = new ArrowheadGrizzlyHttpServer();
        server
                .setSecure(isSecure)
                .setAddress(props.getAddress())
                .setPort(props.getPort())
                .setSecurityContext(securityContext);
        return server;
    }

    public static ArrowheadGrizzlyHttpServer createDefault(ArrowheadSecurityContext securityContext) {
        final boolean isSecure = ArrowheadProperties.getDefaultIsSecure();
        final ArrowheadGrizzlyHttpServer server = new ArrowheadGrizzlyHttpServer();
        server
                .setSecure(isSecure)
                .setAddress(ArrowheadProperties.getDefaultAddress())
                .setPort(ArrowheadProperties.getDefaultPort(isSecure))
                .setSecurityContext(securityContext);
        return server;
    }

    private ArrowheadGrizzlyHttpServer() {
        packages.add("eu.arrowhead.common");
    }

    public ArrowheadGrizzlyHttpServer addResources(Class<? extends ArrowheadResource> ... resources) {
        return addResources(Arrays.asList(resources));
    }

    public ArrowheadGrizzlyHttpServer addPackages(String ... packages) {
        return addPackages(Arrays.asList(packages));
    }

    public ArrowheadGrizzlyHttpServer addResources(Collection<? extends Class<? extends ArrowheadResource>> resources) {
        this.resources.addAll(resources);
        return this;
    }

    public ArrowheadGrizzlyHttpServer addPackages(Collection<? extends String> packages) {
        this.packages.addAll(packages);
        return this;
    }

    public ArrowheadGrizzlyHttpServer replacePackages(Set<String> packages) {
        this.packages = packages;
        return this;
    }

    @Override
    public void onStart() {
        final ResourceConfig config = new ResourceConfig();
        config.packages(packages.toArray(new String[]{}));

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
            config.property("server_common_name", getServerCN());

            sslEC = new SSLEngineConfigurator(getSecurityContext().getSSLContextConfigurator())
                    .setClientMode(false)
                    .setNeedClientAuth(true);
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

    @Override
    public void onStop() {
        if (server != null) {
            server.shutdownNow();
            server = null;
        }
    }

}