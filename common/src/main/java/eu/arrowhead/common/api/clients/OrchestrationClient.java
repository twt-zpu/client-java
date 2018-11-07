package eu.arrowhead.common.api.clients;

import eu.arrowhead.common.api.ArrowheadSecurityContext;
import eu.arrowhead.common.misc.ArrowheadProperties;
import eu.arrowhead.common.misc.Utility;
import eu.arrowhead.common.model.*;

import javax.ws.rs.core.UriBuilder;

public class OrchestrationClient extends RestClient {
    private boolean isSecure;
    private String orchestrationUri;

    public static OrchestrationClient createFromProperties(ArrowheadSecurityContext securityContext) {
        return createFromProperties(Utility.getProp(), securityContext);
    }

    public static OrchestrationClient createFromProperties(ArrowheadProperties props, ArrowheadSecurityContext securityContext) {
        boolean isSecure = props.isSecure();
        return new OrchestrationClient()
                .setSecure(isSecure)
                .setAddress(props.getOrchAddress())
                .setPort(props.getOrchPort())
                .setSecurityContext(securityContext);
    }

    public static OrchestrationClient createDefault(ArrowheadSecurityContext securityContext) {
        final boolean isSecure = ArrowheadProperties.getDefaultIsSecure();
        return new OrchestrationClient()
                .setSecure(isSecure)
                .setAddress(ArrowheadProperties.getDefaultOrchAddress())
                .setPort(ArrowheadProperties.getDefaultOrchPort(isSecure))
                .setSecurityContext(securityContext);
    }

    private OrchestrationClient() {
        super("0.0.0.0", 80);
        isSecure = false;
    }

    public boolean isSecure() {
        return isSecure;
    }

    public OrchestrationClient setSecure(boolean secure) {
        isSecure = secure;
        updateUris();
        return this;
    }

    @Override
    public OrchestrationClient setAddress(String address) {
        super.setAddress(address);
        updateUris();
        return this;
    }

    @Override
    public OrchestrationClient setPort(Integer port) {
        super.setPort(port);
        updateUris();
        return this;
    }

    @Override
    public OrchestrationClient setSecurityContext(ArrowheadSecurityContext securityContext) {
        super.setSecurityContext(securityContext);
        return this;
    }

    private void updateUris() {
        String baseUri = Utility.getUri(getAddress(), getPort(), "orchestrator", isSecure, false);
        orchestrationUri = UriBuilder.fromPath(baseUri).path("orchestration").toString();
    }

    public String requestService(ServiceRequestForm srf) {
        OrchestrationResponse orchResponse = sendRequest(orchestrationUri, "POST", srf)
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
