package eu.arrowhead.common.api.clients;

import eu.arrowhead.common.api.clients.core.OrchestrationClient;
import eu.arrowhead.common.exception.DataNotFoundException;
import eu.arrowhead.common.model.*;
import org.apache.log4j.Logger;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.Set;

/**
 * Orchestration strategies define how and when orchestration happens when using {@link HttpClient}.
 *
 * You can either use of the default strategies({@link Never}, {@link Once}, or {@link Always}) or inherit from this
 * class to create your own.
 */
public abstract class OrchestrationStrategy {
    protected final Logger log = Logger.getLogger(getClass());

    /**
     * Send a HTTP request to a server. See the other methods in this class for helpers to perform the intermediate
     * steps.
     * @param client the {@link HttpClient} the request came from.
     * @param method the HTTP method.
     * @param appendUri the append URI, either the one given to {@link HttpClient#request} or an empty builder. You
     *                  should set scheme, host, port, service path (remember to preserve what is already there), and
     *                  query parameters (like token and signature). This would already have been cloned by
     *                  {@link HttpClient}, so you are free to change it. Helper methods in this class can help you
     *                  with this task.
     * @param payload the payload to send.
     * @return the response.
     */
    public abstract Response request(HttpClient client, HttpClient.Method method, UriBuilder appendUri, Object payload);

    /**
     * Build an URI on the append URI, given the raw elements. This does not add security token.
     * @param appendUri the append URI.
     * @param secure secure mode (e.g. use HTTP or HTTPS)?
     * @param address the host.
     * @param port the port.
     * @param serviceURI the URI of the service (will be added before any path already present in the append URI.
     * @return the append URI.
     */
    protected UriBuilder buildUri(UriBuilder appendUri, boolean secure, String address, Integer port, String serviceURI) {
        appendUri.scheme(secure ? "https" : "http")
                .host(address);
        if (serviceURI != null) {
            final String path = appendUri.build().getPath();
            appendUri.replacePath(serviceURI).path(path);
        }
        if (port != null) appendUri.port(port);
        return appendUri;
    }

    /**
     * Add a token and signature elements to the uri builder.
     * @param uriBuilder the uri builder.
     * @param authorizationToken the token.
     * @param signature the signature.
     * @return the uri builder.
     */
    protected UriBuilder addToken(UriBuilder uriBuilder, String authorizationToken, String signature) {
        uriBuilder
                .queryParam("token", authorizationToken)
                .queryParam("signature", signature);
        return uriBuilder;
    }

    /**
     * Build an URI on the append URI, given an orchestration entry. This does add security token if the entry requires
     * secure mode.
     * @param appendUri the append URI.
     * @param entry the orchestration entry, and given by the {@link OrchestrationClient}.
     * @return the append URI.
     */
    protected UriBuilder buildUri(UriBuilder appendUri, OrchestrationForm entry) {
        final ArrowheadSystem provider = entry.getProvider();
        final boolean secure = entry.isSecure();
        final String address = provider.getAddress();
        final Integer port = provider.getPort();
        final String serviceURI = entry.getServiceURI();

        appendUri = buildUri(appendUri, secure, address, port, serviceURI);
        if (secure) appendUri = addToken(appendUri, entry.getAuthorizationToken(), entry.getSignature());
        return appendUri;
    }

    /**
     * Should return true if the strategy runs requests in secure mode.
     * @return true/false.
     */
    public abstract boolean isSecure();

    /**
     * Send a request through the client.
     * @param client the {@link HttpClient} the request came from.
     * @param uri the URI.
     * @param method the HTTP method.
     * @param interfaces the set of accepted Arrowhead interfaces. The client will use the first one it has a valid
     *                   converter for.
     * @param payload the payload to send along.
     * @return the response.
     */
    protected Response send(HttpClient client, URI uri, HttpClient.Method method, Set<String> interfaces, Object payload) {
        return client.send(uri, method, interfaces, payload);
    }

    /**
     * Send a request through the client.
     * @param client the {@link HttpClient} the request came from.
     * @param uri the URI.
     * @param method the HTTP method.
     * @param anInterface the Arrowhead interface to use. Not that the client must have a valid converter for this.
     * @param payload the payload to send along.
     * @return the response.
     */
    protected Response send(HttpClient client, URI uri, HttpClient.Method method, String anInterface, Object payload) {
        return client.send(uri, method, anInterface, payload);
    }

    /**
     * The never strategy. This will not send any orchestration requests.
     */
    public static class Never extends OrchestrationStrategy {
        private final boolean secure;
        private final String address;
        private final int port;
        private final String serviceUri;
        private final String anInterface;

        /**
         * Construct a new never strategy. Since it does not poll the orchestration system for the data, this needs to
         * be supplied manually here.
         * @param secure send secure requests?
         * @param address the host.
         * @param port the port.
         * @param serviceUri the URI of the service.
         * @param anInterface the accepted Arrowhead interface.
         */
        public Never(boolean secure, String address, int port, String serviceUri, String anInterface) {
            this.secure = secure;
            this.address = address;
            this.port = port;
            this.serviceUri = serviceUri;
            this.anInterface = anInterface;
        }

        @Override
        public Response request(HttpClient client, HttpClient.Method method, UriBuilder appendUri, Object payload) {
            final URI uri = buildUri(appendUri, secure, address, port, serviceUri).build();
            return send(client, uri, method, anInterface, payload);
        }

        @Override
        public boolean isSecure() {
            return secure;
        }
    }

    /**
     * The once strategy. This will perform orchestration once during the construction of the strategy. If the token
     * expires or the Arrowhead system stops to reply, this strategy will no longer work.
     */
    public static class Once extends OrchestrationStrategy {
        private final OrchestrationForm entry;

        /**
         * Construct a new once strategy.
         * @param orchestrationClient the orchestration client to use for sending the request.
         * @param serviceRequestForm the service request form describing the service that is needed.
         */
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
        public Response request(HttpClient client, HttpClient.Method method, UriBuilder appendUri, Object payload) {
            final URI uri = buildUri(appendUri, entry).build();
            final Set<String> interfaces = entry.getService().getInterfaces();
            return send(client, uri, method, interfaces, payload);
        }

        @Override
        public boolean isSecure() {
            return entry.isSecure();
        }
    }

    /**
     * The always strategy. This will send orchestration requests on every single request create on the client.
     */
    public static class Always extends OrchestrationStrategy {
        private final OrchestrationClient orchestrationClient;
        private final ServiceRequestForm serviceRequestForm;

        /**
         * Construct a new always strategy.
         * @param orchestrationClient the orchestration client to use for sending orchestration requests.
         * @param serviceRequestForm the service request form describing the service that is needed.
         */
        public Always(OrchestrationClient orchestrationClient, ServiceRequestForm serviceRequestForm) {
            this.orchestrationClient = orchestrationClient;
            this.serviceRequestForm = serviceRequestForm;
        }

        @Override
        public Response request(HttpClient client, HttpClient.Method method, UriBuilder appendUri, Object payload) {
            try {
                // TODO Should add a loop here !!!
                final OrchestrationResponse response1 = orchestrationClient.request(serviceRequestForm);
                final OrchestrationForm entry = response1.getFirst();
                final URI uri = buildUri(appendUri, entry).build();
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
