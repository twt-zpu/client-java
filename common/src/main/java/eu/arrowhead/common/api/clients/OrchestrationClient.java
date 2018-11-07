package eu.arrowhead.common.api.clients;

import eu.arrowhead.common.api.ArrowheadSecurityContext;
import eu.arrowhead.common.misc.ArrowheadProperties;
import eu.arrowhead.common.misc.Utility;
import eu.arrowhead.common.model.*;

import javax.ws.rs.core.UriBuilder;

public class OrchestrationClient extends RestClient {
    public static OrchestrationClient createFromProperties(ArrowheadSecurityContext securityContext) {
        return createFromProperties(Utility.getProp(), securityContext);
    }

    public static OrchestrationClient createFromProperties(ArrowheadProperties props, ArrowheadSecurityContext securityContext) {
        return new OrchestrationClient()
                .setAddress(props.getOrchAddress())
                .setPort(props.getOrchPort())
                .setSecurityContext(securityContext)
                .setServicePath("orchestrator");
    }

    public static OrchestrationClient createDefault(ArrowheadSecurityContext securityContext) {
        final boolean isSecure = ArrowheadProperties.getDefaultIsSecure();
        return new OrchestrationClient()
                .setAddress(ArrowheadProperties.getDefaultOrchAddress())
                .setPort(ArrowheadProperties.getDefaultOrchPort(isSecure))
                .setSecurityContext(securityContext)
                .setServicePath("orchestrator");
    }

    @Override
    public OrchestrationClient setAddress(String address) {
        super.setAddress(address);
        return this;
    }

    @Override
    public OrchestrationClient setPort(int port) {
        super.setPort(port);
        return this;
    }

    @Override
    public OrchestrationClient setSecurityContext(ArrowheadSecurityContext securityContext) {
        super.setSecurityContext(securityContext);
        return this;
    }

    @Override
    public OrchestrationClient setServicePath(String path) {
        super.setServicePath(path);
        return this;
    }

    public String requestService(ServiceRequestForm srf) {
        OrchestrationResponse orchResponse = sendRequest(Method.POST, "orchestration", srf)
                .readEntity(OrchestrationResponse.class);

        log.info("Orchestration Response payload: " + Utility.toPrettyJson(null, orchResponse));

        final OrchestrationForm entry = orchResponse.getFirst();
        ArrowheadSystem provider = entry.getProvider();

        String serviceURI = entry.getServiceURI();
        UriBuilder ub = UriBuilder.fromPath("").host(provider.getAddress()).scheme("http");
        if (serviceURI != null) {
            ub.path(serviceURI);
        }
        if (provider.getPort() != null && provider.getPort() > 0) {
            ub.port(provider.getPort());
        }
        if (entry.getService().getServiceMetadata().containsKey(ServiceMetadata.Keys.SECURITY)) {
            ub.scheme("https");
            ub.queryParam("token", entry.getAuthorizationToken());
            ub.queryParam("signature", entry.getSignature());
        }

        log.info("Received provider system URL: " + ub.toString());

        return ub.toString();
    }

}
