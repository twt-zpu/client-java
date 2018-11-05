package eu.arrowhead.common.api.clients;

import eu.arrowhead.common.misc.ArrowheadProperties;
import eu.arrowhead.common.misc.Utility;
import eu.arrowhead.common.model.ArrowheadSystem;
import eu.arrowhead.common.model.OrchestrationForm;
import eu.arrowhead.common.model.OrchestrationResponse;
import eu.arrowhead.common.model.ServiceRequestForm;

import javax.ws.rs.core.UriBuilder;

public class OrchestrationClient extends ArrowheadSystem {
    private boolean isSecure;
    private String orchestrationUri;

    public static OrchestrationClient createFromProperties() {
        return createFromProperties(Utility.getProp());
    }

    public static OrchestrationClient createFromProperties(ArrowheadProperties props) {
        boolean isSecure = props.isSecure();
        return new OrchestrationClient()
                .setSecure(isSecure)
                .setAddress(props.getOrchAddress())
                .setPort(props.getOrchPort());
    }

    public static OrchestrationClient createDefault() {
        final boolean isSecure = ArrowheadProperties.getDefaultIsSecure();
        return new OrchestrationClient()
                .setSecure(isSecure)
                .setAddress(ArrowheadProperties.getDefaultOrchAddress())
                .setPort(ArrowheadProperties.getDefaultOrchPort(isSecure));
    }

    private OrchestrationClient() {
        super(null, "0.0.0.0", 80, null);
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

    private void updateUris() {
        String baseUri = Utility.getUri(getAddress(), getPort(), "orchestrator", isSecure, false);
        orchestrationUri = UriBuilder.fromPath(baseUri).path("orchestration").toString();
    }

    public String requestService(ServiceRequestForm srf) {
        OrchestrationResponse orchResponse = Utility
                .sendRequest(orchestrationUri, "POST", srf)
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
        if (entry.getService().getServiceMetadata().containsKey("security")) {
            ub.scheme("https");
            ub.queryParam("token", entry.getAuthorizationToken());
            ub.queryParam("signature", entry.getSignature());
        }

        log.info("Received provider system URL: " + ub.toString());

        return ub.toString();
    }

}
