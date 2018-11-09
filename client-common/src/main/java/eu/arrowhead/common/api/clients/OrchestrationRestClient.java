package eu.arrowhead.common.api.clients;

import eu.arrowhead.common.api.ArrowheadSecurityContext;
import eu.arrowhead.common.model.ServiceRequestForm;

import javax.ws.rs.core.UriBuilder;

public class OrchestrationRestClient extends RestClient {
    private OrchestrationClient orchestrationClient;
    private ServiceRequestForm serviceRequestForm;

    /**
     * @param secure Requires explicitly setting security, to avoid situations where the user expects a secure
     *               connection, but it really isn't due to the lack of security context or wrong URI scheme.
     */
    protected OrchestrationRestClient(boolean secure) {
        super(secure);
    }

    public static RestClient create(boolean secure,
                                    OrchestrationClient orchestrationClient,
                                    ServiceRequestForm serviceRequestForm,
                                    ArrowheadSecurityContext securityContext) {
        return new OrchestrationRestClient(secure)
                .setOrchestrationClient(orchestrationClient)
                .setServiceRequestForm(serviceRequestForm)
                .setSecurityContext(securityContext);
    }

    private OrchestrationRestClient setOrchestrationClient(OrchestrationClient orchestrationClient) {
        this.orchestrationClient = orchestrationClient;
        return this;
    }

    private OrchestrationRestClient setServiceRequestForm(ServiceRequestForm serviceRequestForm) {
        this.serviceRequestForm = serviceRequestForm;
        return this;
    }

    @Override
    protected UriBuilder onRequest(Method method) {
        return orchestrationClient.requestUri(serviceRequestForm);
    }
}
