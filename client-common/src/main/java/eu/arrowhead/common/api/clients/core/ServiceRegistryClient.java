package eu.arrowhead.common.api.clients.core;

import eu.arrowhead.common.api.ArrowheadSecurityContext;
import eu.arrowhead.common.api.clients.HttpClient;
import eu.arrowhead.common.api.clients.OrchestrationStrategy;
import eu.arrowhead.common.exception.ArrowheadRuntimeException;
import eu.arrowhead.common.exception.ExceptionType;
import eu.arrowhead.common.misc.ArrowheadProperties;
import eu.arrowhead.common.model.ServiceRegistryEntry;

import javax.ws.rs.core.UriBuilder;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ServiceRegistryClient extends HttpClient {
    private static final Map<ServiceRegistryClient, Set<ServiceRegistryEntry>> entries = new HashMap<>();
    private static final UriBuilder REGISTER_URI = UriBuilder.fromPath("register");
    private static final UriBuilder REMOVE_URI = UriBuilder.fromPath("remove");

    public static ServiceRegistryClient createFromProperties(ArrowheadSecurityContext securityContext) {
        return createFromProperties(ArrowheadProperties.loadDefault(), securityContext);
    }

    public static ServiceRegistryClient createFromProperties(ArrowheadProperties props, ArrowheadSecurityContext securityContext) {
        final boolean secure = props.isSecure();
        return new ServiceRegistryClient(secure, securityContext, props.getSrAddress(), props.getSrPort());
    }

    public static ServiceRegistryClient createDefault(ArrowheadSecurityContext securityContext) {
        final boolean isSecure = ArrowheadProperties.getDefaultIsSecure();
        return new ServiceRegistryClient(isSecure, securityContext, ArrowheadProperties.getDefaultSrAddress(),
                ArrowheadProperties.getDefaultSrPort(isSecure));
    }

    private ServiceRegistryClient(boolean secure, ArrowheadSecurityContext securityContext, String host, int port) {
        super(new OrchestrationStrategy.Never(secure, host, port, "serviceregistry"), secure, securityContext);
    }

    public ServiceRegistryEntry register(ServiceRegistryEntry srEntry) {
        try {
            request(Method.POST, REGISTER_URI, srEntry);
        } catch (ArrowheadRuntimeException e) {
            if (e.getExceptionType() == ExceptionType.DUPLICATE_ENTRY) {
                log.warn("Received DuplicateEntryException from SR, sending delete request and then " +
                        "registering again.");
                unregister(srEntry);
                request(Method.POST, REGISTER_URI, srEntry);
            } else {
                throw e;
            }
        }

        if (!entries.containsKey(this)) entries.put(this, new HashSet<>());
        entries.get(this).add(srEntry);

        log.info("Registering service is successful!");

        return srEntry;
    }

    public void unregister(ServiceRegistryEntry srEntry) {
        if (srEntry != null) {
            request(Method.PUT, REMOVE_URI, srEntry);
            if (entries.containsKey(this))
                entries.get(this).remove(srEntry);
            log.info("Removing service is successful!");
        }
    }

    private void unregister(Set<ServiceRegistryEntry> entries) {
        entries.forEach(this::unregister);
    }

    public static void unregisterAll() {
        entries.forEach(ServiceRegistryClient::unregister);
    }
}