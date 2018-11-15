package eu.arrowhead.common.api.clients;

import eu.arrowhead.common.api.clients.core.OrchestrationClient;
import eu.arrowhead.common.exception.*;
import eu.arrowhead.common.model.*;
import org.apache.log4j.Logger;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.Set;

public abstract class OrchestrationStrategy {
    protected final Logger log = Logger.getLogger(getClass());

    public abstract Response request(HttpClient client, HttpClient.Method method, UriBuilder baseUri, Object payload);

    protected UriBuilder buildUri(UriBuilder uriBuilder, boolean secure, String address, Integer port, String serviceURI) {
        uriBuilder.scheme(secure ? "https" : "http")
                .host(address);
        if (serviceURI != null) {
            final String path = uriBuilder.build().getPath();
            uriBuilder.replacePath(serviceURI).path(path);
        }
        if (port != null) uriBuilder.port(port);
        return uriBuilder;
    }

    protected UriBuilder addToken(UriBuilder uriBuilder, String authorizationToken, String signature) {
        uriBuilder
                .queryParam("token", authorizationToken)
                .queryParam("signature", signature);
        return uriBuilder;
    }

    protected UriBuilder buildUri(UriBuilder uriBuilder, OrchestrationForm entry) {
        final ArrowheadSystem provider = entry.getProvider();
        final boolean secure = entry.isSecure();
        final String address = provider.getAddress();
        final Integer port = provider.getPort();
        final String serviceURI = entry.getServiceURI();

        uriBuilder = buildUri(uriBuilder, secure, address, port, serviceURI);
        if (secure) uriBuilder = addToken(uriBuilder, entry.getAuthorizationToken(), entry.getSignature());
        return uriBuilder;
    }

    public abstract boolean isSecure();

    protected Response send(HttpClient client, URI uri, HttpClient.Method method, Set<String> interfaces, Object payload) {
        return client.send(uri, method, interfaces, payload);
    }

    protected Response send(HttpClient client, URI uri, HttpClient.Method method, HttpClient.Interface anInterface, Object payload) {
        return client.send(uri, method, anInterface, payload);
    }

    public static class Never extends OrchestrationStrategy {
        private final boolean secure;
        private final String address;
        private final int port;
        private final String serviceUri;
        private final HttpClient.Interface anInterface;

        public Never(boolean secure, String address, int port, String serviceUri, HttpClient.Interface anInterface) {
            this.secure = secure;
            this.address = address;
            this.port = port;
            this.serviceUri = serviceUri;
            this.anInterface = anInterface;
        }

        @Override
        public Response request(HttpClient client, HttpClient.Method method, UriBuilder baseUri, Object payload) {
            final URI uri = buildUri(baseUri, secure, address, port, serviceUri).build();
            return send(client, uri, method, anInterface, payload);
        }

        @Override
        public boolean isSecure() {
            return secure;
        }
    }

    public static class Once extends OrchestrationStrategy {
        private final OrchestrationForm entry;

        public Once(OrchestrationClient orchestrationClient, ServiceRequestForm serviceRequestForm) {
            try {
                final OrchestrationResponse response1 = orchestrationClient.request(serviceRequestForm);
                entry = response1.getFirst();
            } catch (DataNotFoundException e) {
                log.warn("Failed with requester system: " + serviceRequestForm.getRequesterSystem());
                throw e;
            }
        }

        @Override
        public Response request(HttpClient client, HttpClient.Method method, UriBuilder baseUri, Object payload) {
            final URI uri = buildUri(baseUri, entry).build();
            final Set<String> interfaces = entry.getService().getInterfaces();
            return send(client, uri, method, interfaces, payload);
        }

        @Override
        public boolean isSecure() {
            return entry.isSecure();
        }
    }

    public static class Always extends OrchestrationStrategy {
        private final OrchestrationClient orchestrationClient;
        private final ServiceRequestForm serviceRequestForm;

        public Always(OrchestrationClient orchestrationClient, ServiceRequestForm serviceRequestForm) {
            this.orchestrationClient = orchestrationClient;
            this.serviceRequestForm = serviceRequestForm;
        }

        @Override
        public Response request(HttpClient client, HttpClient.Method method, UriBuilder baseUri, Object payload) {
            try {
                final OrchestrationResponse response1 = orchestrationClient.request(serviceRequestForm);
                final OrchestrationForm entry = response1.getFirst();
                final URI uri = buildUri(baseUri, entry).build();
                final Set<String> interfaces = entry.getService().getInterfaces();
                return send(client, uri, method, interfaces, payload);
            } catch (DataNotFoundException e) {
                log.warn("Failed with requester system: " + serviceRequestForm.getRequesterSystem());
                throw e;
            }
        }

        @Override
        public boolean isSecure() {
            return serviceRequestForm.getRequestedService().getServiceMetadata().containsKey(ServiceMetadata.Keys.SECURITY);
        }

    }
}
