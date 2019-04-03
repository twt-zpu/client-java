package eu.arrowhead.common.api.clients;

import eu.arrowhead.common.api.clients.core.OrchestrationClient;
import eu.arrowhead.common.exception.DataNotFoundException;
import eu.arrowhead.common.misc.Utility;
import eu.arrowhead.common.model.OrchestrationForm;
import eu.arrowhead.common.model.OrchestrationResponse;
import eu.arrowhead.common.model.ServiceMetadata;
import eu.arrowhead.common.model.ServiceRequestForm;
import java.net.URI;
import java.util.Set;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Orchestration strategies define how and when orchestration happens when using {@link HttpClient}.
 *
 * You can either use of the default strategies({@link Never}, {@link Once}, or {@link Always}) or inherit from this
 * class to create your own.
 */
public abstract class OrchestrationStrategy {
    protected final Logger log = LogManager.getLogger(getClass());

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
            final URI uri = Utility.buildUri(appendUri, secure, address, port, serviceUri).build();
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
            final URI uri = Utility.buildUri(appendUri, entry).build();
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
                final URI uri = Utility.buildUri(appendUri, entry).build();
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
