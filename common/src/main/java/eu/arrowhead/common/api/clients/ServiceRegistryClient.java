package eu.arrowhead.common.api.clients;

import eu.arrowhead.common.api.ArrowheadSecurityContext;
import eu.arrowhead.common.exception.ArrowheadRuntimeException;
import eu.arrowhead.common.exception.ExceptionType;
import eu.arrowhead.common.misc.ArrowheadProperties;
import eu.arrowhead.common.model.ServiceRegistryEntry;

import java.util.*;

public class ServiceRegistryClient extends RestClient {
    private static final Map<ServiceRegistryClient, Set<ServiceRegistryEntry>> entries = new HashMap<>();

    public static ServiceRegistryClient createFromProperties(ArrowheadSecurityContext securityContext) {
        return createFromProperties(ArrowheadProperties.loadDefault(), securityContext);
    }

    public static ServiceRegistryClient createFromProperties(ArrowheadProperties props, ArrowheadSecurityContext securityContext) {
        return new ServiceRegistryClient()
                .setAddress(props.getSrAddress())
                .setPort(props.getSrPort())
                .setSecurityContext(securityContext)
                .setServicePath("serviceregistry");
    }

    public static ServiceRegistryClient createDefault(ArrowheadSecurityContext securityContext) {
        final boolean isSecure = ArrowheadProperties.getDefaultIsSecure();
        return new ServiceRegistryClient()
                .setAddress(ArrowheadProperties.getDefaultSrAddress())
                .setPort(ArrowheadProperties.getDefaultSrPort(isSecure))
                .setSecurityContext(securityContext)
                .setServicePath("serviceregistry");
    }

    @Override
    public ServiceRegistryClient setAddress(String address) {
        super.setAddress(address);
        return this;
    }

    @Override
    public ServiceRegistryClient setPort(int port) {
        super.setPort(port);
        return this;
    }

    @Override
    public ServiceRegistryClient setSecurityContext(ArrowheadSecurityContext securityContext) {
        super.setSecurityContext(securityContext);
        return this;
    }

    @Override
    public ServiceRegistryClient setServicePath(String path) {
        super.setServicePath(path);
        return this;
    }

    public ServiceRegistryEntry register(ServiceRegistryEntry srEntry) {
        try {
            sendRequest(Method.POST, "register", srEntry);
        } catch (ArrowheadRuntimeException e) {
            if (e.getExceptionType() == ExceptionType.DUPLICATE_ENTRY) {
                log.warn("Received DuplicateEntryException from SR, sending delete request and then " +
                        "registering again.");
                unregister(srEntry);
                sendRequest(Method.POST, "register", srEntry);
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
            sendRequest(Method.PUT, "remove", srEntry);
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