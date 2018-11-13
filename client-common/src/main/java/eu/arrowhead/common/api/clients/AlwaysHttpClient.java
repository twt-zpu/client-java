package eu.arrowhead.common.api.clients;

import eu.arrowhead.common.api.clients.core.OrchestrationClient;
import eu.arrowhead.common.exception.DataNotFoundException;
import eu.arrowhead.common.model.OrchestrationForm;
import eu.arrowhead.common.model.OrchestrationResponse;
import eu.arrowhead.common.model.ServiceRequestForm;

import javax.ws.rs.core.UriBuilder;

public class AlwaysHttpClient extends HttpClient {
    private final ServiceRequestForm serviceRequestForm;
    private final OrchestrationClient orchestrationClient;

    protected AlwaysHttpClient(ServiceRequestForm serviceRequestForm, OrchestrationClient orchestrationClient) {
        super(orchestrationClient.isSecure(), orchestrationClient.getSecurityContext());
        this.serviceRequestForm = serviceRequestForm;
        this.orchestrationClient = orchestrationClient;
    }

    @Override
    protected UriBuilder onRequest(Method method) {
        try {
            final OrchestrationResponse response = orchestrationClient.request(serviceRequestForm);
            final OrchestrationForm entry = response.getFirst();
            final UriBuilder uriBuilder = entry.getUriBuilder();

            log.info("Received provider system URL: " + uriBuilder.toString());
            return uriBuilder;
        } catch (DataNotFoundException e) {
            log.warn("Failed with requester system: " + serviceRequestForm.getRequesterSystem());
            throw e;
        }
    }

    public static class Builder extends HttpClient.Builder {
        @Override
        public HttpClient build(ServiceRequestForm serviceRequestForm, OrchestrationClient orchestrationClient) {
            return new AlwaysHttpClient(serviceRequestForm, orchestrationClient);
        }
    }
}
