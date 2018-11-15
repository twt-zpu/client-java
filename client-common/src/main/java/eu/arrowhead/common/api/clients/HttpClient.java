package eu.arrowhead.common.api.clients;

import eu.arrowhead.common.api.ArrowheadSecurityContext;
import eu.arrowhead.common.exception.*;
import eu.arrowhead.common.misc.SecurityUtils;
import eu.arrowhead.common.misc.Utility;
import org.apache.log4j.Logger;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;

public class HttpClient {
    private static final Client insecureClient = SecurityUtils.createClient(null);

    protected final Logger log = Logger.getLogger(getClass());
    private final OrchestrationStrategy strategy;
    private final boolean secure;
    private final ArrowheadSecurityContext securityContext;
    private final Client client;

    public HttpClient(OrchestrationStrategy strategy, boolean secure, ArrowheadSecurityContext securityContext) {
        this.strategy = strategy;
        this.secure = secure;
        this.securityContext = securityContext;
        if (secure ^ securityContext != null)
            throw new AuthException(String.format("Client is %s, but trying to set security context to %s)",
                    secure ? "secure" : "insecure", securityContext == null ? "null" : "not null"));
        client = secure ?
                SecurityUtils.createClient(securityContext.getSslContext()) :
                insecureClient;
    }

    public boolean isSecure() {
        return secure;
    }

    public ArrowheadSecurityContext getSecurityContext() {
        return securityContext;
    }

    public Response request(Method method) {
        return request(method, null, null);
    }

    public <T> Response request(Method method, T payload) {
        return request(method, null, payload);
    }

    public Response request(Method method, UriBuilder appendUri) {
        return request(method, appendUri, null);
    }

    public <T> Response request(Method method, UriBuilder appendUri, T payload) {
        appendUri = appendUri == null ? UriBuilder.fromUri("") : appendUri.clone();

        final URI uri = appendUri.build();
        if (uri.getScheme() != null) throw new ArrowheadRuntimeException("Append URI should not contain a scheme");
        if (uri.getHost() != null) throw new ArrowheadRuntimeException("Append URI should not contain a host");
        if (uri.getPort() != -1) throw new ArrowheadRuntimeException("Append URI should not contain a port");

        // TODO Remove JSON
        return strategy.request(this, method, appendUri, Entity.json(payload));
    }

    Response send(URI uri, HttpClient.Method method, Entity<?> entity) {
        try {
            log.info(String.format("%s %s", method.toString(), uri.toString()));
            final Invocation.Builder client = this.client
                    .target(uri)
                    .request()
                    // TODO Remove JSON
                    .header("Content-type", "application/json");
            return check(entity == null ?
                    client.method(method.toString()) :
                    client.method(method.toString(), entity), uri.toString());
        } catch (ProcessingException e) {
            throw handleProcessingException(uri, e);
        }
    }

    private ArrowheadRuntimeException handleProcessingException(URI uri, ProcessingException e) {
        if (e.getCause().getMessage().contains("PKIX path")) {
            return new AuthException("The system at " + uri + " is not part of the same certificate chain of trust!", Response.Status.UNAUTHORIZED.getStatusCode(),
                    e);
        } else {
            return new UnavailableServerException("Could not get any response from: " + uri, Response.Status.SERVICE_UNAVAILABLE.getStatusCode(), e);
        }
    }

    private Response check(Response response, String uri) {
        // If the response status code does not start with 2 the request was not successful
        if (!(response.getStatusInfo().getFamily() == Response.Status.Family.SUCCESSFUL)) {
            //The response body has to be extracted before the stream closes
            String errorMessageBody = Utility.toPrettyJson(null, response.getEntity());
            if (errorMessageBody == null || errorMessageBody.equals("null")) {
                response.bufferEntity();
                errorMessageBody = response.readEntity(String.class);
            }

            ErrorMessage errorMessage;
            try {
                errorMessage = response.readEntity(ErrorMessage.class);
            } catch (RuntimeException e) {
                throw new ArrowheadRuntimeException("Unknown error occurred at " + uri, e);
            }
            if (errorMessage == null || errorMessage.getExceptionType() == null) {
                log.warn("Request failed, response status code: " + response.getStatus());
                log.warn("Request failed, response body: " + errorMessageBody);
                throw new ArrowheadRuntimeException("Unknown error occurred at " + uri);
            } else {
                log.warn(Utility.toPrettyJson(null, errorMessage));
                switch (errorMessage.getExceptionType()) {
                    case ARROWHEAD:
                        throw new ArrowheadRuntimeException(errorMessage.getErrorMessage(), errorMessage.getErrorCode());
                    case AUTH:
                        throw new AuthException(errorMessage.getErrorMessage(), errorMessage.getErrorCode());
                    case BAD_METHOD:
                        throw new ArrowheadRuntimeException(errorMessage.getErrorMessage(), errorMessage.getErrorCode());
                    case BAD_PAYLOAD:
                        throw new BadPayloadException(errorMessage.getErrorMessage(), errorMessage.getErrorCode());
                    case BAD_URI:
                        throw new ArrowheadRuntimeException(errorMessage.getErrorMessage(), errorMessage.getErrorCode());
                    case DATA_NOT_FOUND:
                        throw new DataNotFoundException(errorMessage.getErrorMessage(), errorMessage.getErrorCode());
                    case DNSSD:
                        throw new DnsException(errorMessage.getErrorMessage(), errorMessage.getErrorCode(), errorMessage.getOrigin());
                    case DUPLICATE_ENTRY:
                        throw new DuplicateEntryException(errorMessage.getErrorMessage(), errorMessage.getErrorCode());
                    case GENERIC:
                        throw new ArrowheadRuntimeException(errorMessage.getErrorMessage(), errorMessage.getErrorCode());
                    case JSON_PROCESSING:
                        throw new ArrowheadRuntimeException(errorMessage.getErrorMessage(), errorMessage.getErrorCode());
                    case UNAVAILABLE:
                        throw new UnavailableServerException(errorMessage.getErrorMessage(), errorMessage.getErrorCode());
                }
            }
        }

        return response;
    }

    /**
     * From: rfc2616 / rfc5789
     */
    public enum Method {
        OPTIONS("OPTIONS"),
        GET("GET"),
        HEAD("HEAD"),
        POST("POST"),
        PUT("PUT"),
        DELETE("DELETE"),
        TRACE("TRACE"),
        CONNECT("CONNECT"),
        PATCH("PATCH"),
        ;

        private final String method;

        Method(final String method) {
            this.method = method;
        }

        @Override
        public String toString() {
            return method;
        }
    }
}
