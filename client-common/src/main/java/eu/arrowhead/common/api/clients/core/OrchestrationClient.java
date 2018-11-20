package eu.arrowhead.common.api.clients.core;

import eu.arrowhead.common.api.ArrowheadConverter;
import eu.arrowhead.common.api.ArrowheadSecurityContext;
import eu.arrowhead.common.api.clients.HttpClient;
import eu.arrowhead.common.api.clients.OrchestrationStrategy;
import eu.arrowhead.common.misc.ArrowheadProperties;
import eu.arrowhead.common.model.OrchestrationResponse;
import eu.arrowhead.common.model.ServiceRequestForm;

import javax.ws.rs.core.UriBuilder;

/**
 * A client for interacting with the Orchestration system. See the static create* methods for how to get an instance of
 * one of these.
 */
public class OrchestrationClient extends HttpClient {
    private static final UriBuilder ORCHESTRATION_URI = UriBuilder.fromPath("orchestration");

    /**
     * Create a new client from the settings in the default properties files.
     * @param securityContext the security context to use.
     * @return your shiny new client.
     */
    public static OrchestrationClient createFromProperties(ArrowheadSecurityContext securityContext) {
        return createFromProperties(ArrowheadProperties.loadDefault(), securityContext);
    }

    /**
     * Create a new client from given set of properties.
     * @param securityContext the security context to use.
     * @return your shiny new client.
     */
    public static OrchestrationClient createFromProperties(ArrowheadProperties props, ArrowheadSecurityContext securityContext) {
        final boolean secure = props.isSecure();
        return new OrchestrationClient(secure, securityContext, props.getOrchAddress(), props.getOrchPort());
    }

    /**
     * Create a new client using default values.
     * @param securityContext the security context to use.
     * @return your shiny new client.
     */
    public static OrchestrationClient createDefault(ArrowheadSecurityContext securityContext) {
        final boolean isSecure = ArrowheadProperties.getDefaultIsSecure();
        return new OrchestrationClient(isSecure, securityContext, ArrowheadProperties.getDefaultOrchAddress(),
                ArrowheadProperties.getDefaultOrchPort(isSecure));
    }

    /**
     * Private construct, see the create* methods.
     * @param secure use secure mode?
     * @param securityContext the security context to use.
     * @param host the host.
     * @param port the port.
     */
    private OrchestrationClient(boolean secure, ArrowheadSecurityContext securityContext, String host, int port) {
        super(new OrchestrationStrategy.Never(secure, host, port, "orchestrator", ArrowheadConverter.JSON), securityContext);
    }

    /**
     * Request a new service from the orchestration system. You should probably consider using {@link HttpClient} over
     * calling this method directly.
     * @param serviceRequestForm the service request form describing the service.
     * @return the orchestration response.
     */
    public OrchestrationResponse request(ServiceRequestForm serviceRequestForm) {
        return request(Method.POST, ORCHESTRATION_URI, serviceRequestForm)
                .readEntity(OrchestrationResponse.class);
    }
}
