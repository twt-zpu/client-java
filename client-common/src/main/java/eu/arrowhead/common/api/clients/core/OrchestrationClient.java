package eu.arrowhead.common.api.clients.core;

import eu.arrowhead.common.api.ArrowheadSecurityContext;
import eu.arrowhead.common.api.clients.HttpClient;
import eu.arrowhead.common.api.clients.StaticHttpClient;
import eu.arrowhead.common.misc.ArrowheadProperties;
import eu.arrowhead.common.model.OrchestrationResponse;
import eu.arrowhead.common.model.ServiceRequestForm;

public class OrchestrationClient extends StaticHttpClient {

    public static OrchestrationClient createFromProperties(ArrowheadSecurityContext securityContext) {
        return createFromProperties(ArrowheadProperties.loadDefault(), securityContext);
    }

    public static OrchestrationClient createFromProperties(ArrowheadProperties props, ArrowheadSecurityContext securityContext) {
        final boolean secure = props.isSecure();
        return new OrchestrationClient(secure, securityContext, props.getOrchAddress(), props.getOrchPort());
    }

    public static OrchestrationClient createDefault(ArrowheadSecurityContext securityContext) {
        final boolean isSecure = ArrowheadProperties.getDefaultIsSecure();
        return new OrchestrationClient(isSecure, securityContext, ArrowheadProperties.getDefaultOrchAddress(),
                ArrowheadProperties.getDefaultOrchPort(isSecure));
    }

    private OrchestrationClient(boolean secure, ArrowheadSecurityContext securityContext, String host, int port) {
        super(secure, securityContext, host, port, "orchestrator");
    }

    public HttpClient buildClient(ServiceRequestForm serviceRequestForm, HttpClient.Builder clientBuilder) {
        return clientBuilder.build(serviceRequestForm, this);
    }

    public OrchestrationResponse request(ServiceRequestForm serviceRequestForm) {
        return post().path("orchestration")
                .send(serviceRequestForm)
                .readEntity(OrchestrationResponse.class);
    }
}
