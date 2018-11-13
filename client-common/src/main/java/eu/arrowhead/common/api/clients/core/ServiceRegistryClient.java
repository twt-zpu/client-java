package eu.arrowhead.common.api.clients.core;

import eu.arrowhead.common.api.ArrowheadSecurityContext;
import eu.arrowhead.common.api.clients.StaticHttpClient;
import eu.arrowhead.common.exception.ArrowheadRuntimeException;
import eu.arrowhead.common.exception.ExceptionType;
import eu.arrowhead.common.misc.ArrowheadProperties;
import eu.arrowhead.common.model.ServiceRegistryEntry;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ServiceRegistryClient extends StaticHttpClient {
    private static final Map<ServiceRegistryClient, Set<ServiceRegistryEntry>> entries = new HashMap<>();

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
        super(secure, securityContext, host, port, "serviceregistry");
    }

    public ServiceRegistryEntry register(ServiceRegistryEntry srEntry) {
        try {
            post().path("register").send(srEntry);
        } catch (ArrowheadRuntimeException e) {
            if (e.getExceptionType() == ExceptionType.DUPLICATE_ENTRY) {
                log.warn("Received DuplicateEntryException from SR, sending delete request and then " +
                        "registering again.");
                unregister(srEntry);
                post().path("register").send(srEntry);
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
            put().path("remove").send(srEntry);
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