package eu.arrowhead.common.api.clients;

import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.ExceptionType;
import eu.arrowhead.common.misc.ArrowheadProperties;
import eu.arrowhead.common.misc.Utility;
import eu.arrowhead.common.model.ArrowheadSystem;
import eu.arrowhead.common.model.ServiceRegistryEntry;

import javax.ws.rs.core.UriBuilder;
import java.util.*;

public class ServiceRegistryClient extends ArrowheadSystem {
    private static final Map<ServiceRegistryClient, Set<ServiceRegistryEntry>> entries = new HashMap<>();
    private String registerUri;
    private String removeUri;
    private boolean isSecure;

    public static ServiceRegistryClient createFromProperties() {
        return createFromProperties(Utility.getProp());
    }

    public static ServiceRegistryClient createFromProperties(ArrowheadProperties props) {
        boolean isSecure = props.isSecure();
        return new ServiceRegistryClient()
                .setSecure(isSecure)
                .setAddress(props.getSrAddress())
                .setPort(props.getSrPort());
    }

    public static ServiceRegistryClient createDefault() {
        final boolean isSecure = ArrowheadProperties.getDefaultIsSecure();
        return new ServiceRegistryClient()
                .setSecure(isSecure)
                .setAddress(ArrowheadProperties.getDefaultSrAddress())
                .setPort(ArrowheadProperties.getDefaultSrPort(isSecure));
    }

    private ServiceRegistryClient() {
        super(null, "0.0.0.0", 80, null);
        isSecure = false;
    }

    public boolean isSecure() {
        return isSecure;
    }

    public ServiceRegistryClient setSecure(boolean secure) {
        isSecure = secure;
        updateUris();
        return this;
    }

    @Override
    public ServiceRegistryClient setAddress(String address) {
        super.setAddress(address);
        updateUris();
        return this;
    }

    @Override
    public ServiceRegistryClient setPort(Integer port) {
        super.setPort(port);
        updateUris();
        return this;
    }

    private void updateUris() {
        String baseUri = Utility.getUri(getAddress(), getPort(), "serviceregistry", isSecure, false);
        registerUri = UriBuilder.fromPath(baseUri).path("register").toString();
        removeUri = UriBuilder.fromPath(baseUri).path("remove").toString();
    }

    public ServiceRegistryEntry register(ServiceRegistryEntry srEntry) {
        try {
            Utility.sendRequest(registerUri, "POST", srEntry);
        } catch (ArrowheadException e) {
            if (e.getExceptionType() == ExceptionType.DUPLICATE_ENTRY) {
                log.warn("Received DuplicateEntryException from SR, sending delete request and then " +
                        "registering again.");
                unregister(srEntry);
                Utility.sendRequest(registerUri, "POST", srEntry);
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
            Utility.sendRequest(removeUri, "PUT", srEntry);
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