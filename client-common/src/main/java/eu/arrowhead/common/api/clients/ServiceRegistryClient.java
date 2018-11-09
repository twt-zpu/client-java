package eu.arrowhead.common.api.clients;

import eu.arrowhead.common.api.ArrowheadSecurityContext;
import eu.arrowhead.common.exception.ArrowheadRuntimeException;
import eu.arrowhead.common.exception.ExceptionType;
import eu.arrowhead.common.misc.ArrowheadProperties;
import eu.arrowhead.common.model.ServiceRegistryEntry;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ServiceRegistryClient extends StaticRestClient {
    private static final Map<ServiceRegistryClient, Set<ServiceRegistryEntry>> entries = new HashMap<>();
    private StaticRestClient registerClient;
    private StaticRestClient removeClient;

    public static ServiceRegistryClient createFromProperties(ArrowheadSecurityContext securityContext) {
        return createFromProperties(ArrowheadProperties.loadDefault(), securityContext);
    }

    public static ServiceRegistryClient createFromProperties(ArrowheadProperties props, ArrowheadSecurityContext securityContext) {
        final boolean isSecure = props.isSecure();
        return new ServiceRegistryClient(isSecure)
                .setAddress(props.getSrAddress())
                .setPort(props.getSrPort())
                .setSecurityContext(securityContext)
                .replacePath("serviceregistry");
    }

    public static ServiceRegistryClient createDefault(ArrowheadSecurityContext securityContext) {
        final boolean isSecure = ArrowheadProperties.getDefaultIsSecure();
        return new ServiceRegistryClient(isSecure)
                .setAddress(ArrowheadProperties.getDefaultSrAddress())
                .setPort(ArrowheadProperties.getDefaultSrPort(isSecure))
                .setSecurityContext(securityContext)
                .replacePath("serviceregistry");
    }

    private ServiceRegistryClient(boolean secure) {
        super(secure);
    }

    @Override
    protected ServiceRegistryClient setAddress(String address) {
        super.setAddress(address);
        return this;
    }

    @Override
    protected ServiceRegistryClient setPort(int port) {
        super.setPort(port);
        return this;
    }

    @Override
    protected ServiceRegistryClient setUri(String uri) {
        super.setUri(uri);
        return this;
    }

    @Override
    protected ServiceRegistryClient setSecure(boolean secure) {
        super.setSecure(secure);
        return this;
    }

    @Override
    protected ServiceRegistryClient setSecurityContext(ArrowheadSecurityContext securityContext) {
        super.setSecurityContext(securityContext);
        return this;
    }

    public ServiceRegistryEntry register(ServiceRegistryEntry srEntry) {
        try {
            registerClient.post().send(srEntry);
        } catch (ArrowheadRuntimeException e) {
            if (e.getExceptionType() == ExceptionType.DUPLICATE_ENTRY) {
                log.warn("Received DuplicateEntryException from SR, sending delete request and then " +
                        "registering again.");
                unregister(srEntry);
                registerClient.post().send(srEntry);
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
            removeClient.put().send(srEntry);
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

    @Override
    protected ServiceRegistryClient replacePath(String path) {
        super.replacePath(path);
        registerClient = clone("register");
        removeClient = clone("remove");
        return this;
    }

    @Override
    protected ServiceRegistryClient addPath(String path) {
        super.addPath(path);
        registerClient = clone("register");
        removeClient = clone("remove");
        return this;
    }
}