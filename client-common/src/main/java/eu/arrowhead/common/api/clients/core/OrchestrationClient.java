package eu.arrowhead.common.api.clients.core;

import eu.arrowhead.common.api.ArrowheadSecurityContext;
import eu.arrowhead.common.api.clients.HttpClient;
import eu.arrowhead.common.api.clients.OrchestrationStrategy;
import eu.arrowhead.common.misc.ArrowheadProperties;
import eu.arrowhead.common.model.OrchestrationResponse;
import eu.arrowhead.common.model.ServiceRequestForm;

import javax.ws.rs.core.UriBuilder;

public class OrchestrationClient extends HttpClient {
    private static final UriBuilder ORCHESTRATION_URI = UriBuilder.fromPath("orchestration");

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
        super(new OrchestrationStrategy.Never(secure, host, port, "orchestrator", MediaType.JSON), securityContext);
    }

    public OrchestrationResponse request(ServiceRequestForm serviceRequestForm) {
        return request(Method.POST, ORCHESTRATION_URI, serviceRequestForm)
                .readEntity(OrchestrationResponse.class);
    }
}
