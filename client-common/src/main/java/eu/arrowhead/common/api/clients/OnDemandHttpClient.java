//package eu.arrowhead.common.api.clients;
//
//import eu.arrowhead.common.api.ArrowheadSecurityContext;
//import eu.arrowhead.common.api.clients.core.OrchestrationClient;
//import eu.arrowhead.common.model.ServiceRequestForm;
//
//import javax.ws.rs.core.UriBuilder;
//
//public class OnDemandHttpClient extends HttpClient {
//    protected OnDemandHttpClient(boolean secure, ArrowheadSecurityContext securityContext) {
//        super(secure, securityContext);
//    }
//
//    @Override
//    protected UriBuilder onRequest(Method method) {
//        // TODO Implement this
//        return null;
//    }
//
//    public static class Builder extends HttpClient.Builder {
//        @Override
//        public HttpClient build(ServiceRequestForm serviceRequestForm, OrchestrationClient orchestrationClient) {
//            return new OnDemandHttpClient(orchestrationClient.isSecure(), orchestrationClient.getSecurityContext());
//        }
//    }
//}
