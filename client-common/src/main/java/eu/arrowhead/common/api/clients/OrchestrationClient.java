package eu.arrowhead.common.api.clients;

import eu.arrowhead.common.api.ArrowheadSecurityContext;
import eu.arrowhead.common.misc.ArrowheadProperties;
import eu.arrowhead.common.misc.Utility;
import eu.arrowhead.common.model.*;

import javax.ws.rs.core.UriBuilder;

public class OrchestrationClient extends StaticRestClient {
    private StaticRestClient orchestrationClient;

    public static OrchestrationClient createFromProperties(ArrowheadSecurityContext securityContext) {
        return createFromProperties(ArrowheadProperties.loadDefault(), securityContext);
    }

    public static OrchestrationClient createFromProperties(ArrowheadProperties props, ArrowheadSecurityContext securityContext) {
        final boolean isSecure = props.isSecure();
        return new OrchestrationClient(isSecure)
                .setAddress(props.getOrchAddress())
                .setPort(props.getOrchPort())
                .setSecurityContext(securityContext)
                .replacePath("orchestrator");
    }

    public static OrchestrationClient createDefault(ArrowheadSecurityContext securityContext) {
        final boolean isSecure = ArrowheadProperties.getDefaultIsSecure();
        return new OrchestrationClient(isSecure)
                .setAddress(ArrowheadProperties.getDefaultOrchAddress())
                .setPort(ArrowheadProperties.getDefaultOrchPort(isSecure))
                .setSecurityContext(securityContext)
                .replacePath("orchestrator");
    }

    private OrchestrationClient(boolean secure) {
        super(secure);
    }

    @Override
    protected OrchestrationClient setAddress(String address) {
        super.setAddress(address);
        return this;
    }

    @Override
    protected OrchestrationClient setPort(int port) {
        super.setPort(port);
        return this;
    }

    @Override
    protected OrchestrationClient setUri(String uri) {
        super.setUri(uri);
        return this;
    }

    @Override
    protected OrchestrationClient setSecure(boolean secure) {
        super.setSecure(secure);
        return this;
    }

    @Override
    protected OrchestrationClient setSecurityContext(ArrowheadSecurityContext securityContext) {
        super.setSecurityContext(securityContext);
        return this;
    }

    public UriBuilder requestUri(ServiceRequestForm srf) {
        OrchestrationResponse orchResponse = orchestrationClient.post()
                .send(srf)
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

        return ub;
    }

    @Override
    protected OrchestrationClient replacePath(String path) {
        super.replacePath(path);
        orchestrationClient = clone("orchestration");
        return this;
    }

    @Override
    protected OrchestrationClient addPath(String path) {
        super.addPath(path);
        orchestrationClient = clone("orchestration");
        return this;
    }

    public RestClient buildClient(ServiceRequestForm serviceRequestForm) {
        return OrchestrationRestClient.create(
                isSecure(),
                this,
                serviceRequestForm,
                getSecurityContext());
    }
}
