package eu.arrowhead.common.api.clients;

import eu.arrowhead.common.api.ArrowheadSecurityContext;
import eu.arrowhead.common.api.clients.core.OrchestrationClient;
import eu.arrowhead.common.exception.DataNotFoundException;
import eu.arrowhead.common.model.*;

import javax.ws.rs.core.UriBuilder;

public class StaticHttpClient extends HttpClient {
    private final UriBuilder uriBuilder;

    protected StaticHttpClient(boolean secure, ArrowheadSecurityContext securityContext, UriBuilder uriBuilder) {
        super(secure, securityContext);
        this.uriBuilder = uriBuilder;
    }

    protected StaticHttpClient(boolean secure, ArrowheadSecurityContext securityContext, String host, int port, String path) {
        super(secure, securityContext);
        this.uriBuilder = UriBuilder.fromUri("")
                .scheme(secure ? "https" : "http")
                .host(host)
                .port(port)
                .path(path);
    }

    @Override
    public String toString() {
        return uriBuilder.toString();
    }

    @Override
    protected UriBuilder onRequest(Method method) {
        return uriBuilder;
    }

    public static class Builder extends HttpClient.Builder {
        @Override
        public HttpClient build(ServiceRequestForm serviceRequestForm, OrchestrationClient orchestrationClient) {
            try {
                final OrchestrationResponse response = orchestrationClient.request(serviceRequestForm);
                final OrchestrationForm entry = response.getFirst();
                final UriBuilder uriBuilder = entry.getUriBuilder();

                log.info("Received provider system URL: " + uriBuilder.toString());
                return new StaticHttpClient(entry.isSecure(), orchestrationClient.getSecurityContext(), uriBuilder);
            } catch (DataNotFoundException e) {
                log.warn("Failed with requester system: " + serviceRequestForm.getRequesterSystem());
                throw e;
            }
        }

    }
}
