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
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public class HttpClient {
    private static final Client insecureClient = SecurityUtils.createClient(null);
    // TODO Add per instance converters also
    private static final Map<MediaType, Function<Object, Entity<?>>> DEFAULT_ENTITY_CONVERTERS = new HashMap<>();
    protected final Logger log = Logger.getLogger(getClass());

    private final OrchestrationStrategy strategy;
    private final ArrowheadSecurityContext securityContext;
    private final Client client;

    static {
        DEFAULT_ENTITY_CONVERTERS.put(MediaType.JSON, Entity::json);
        DEFAULT_ENTITY_CONVERTERS.put(MediaType.XML, Entity::xml);
    }

    /**
     * TODO Basing this on an enum is very limiting!
     * @param mediaType
     * @param converter
     */
    public static void addDefaultEntityConverter(MediaType mediaType, Function<Object, Entity<?>> converter) {
        DEFAULT_ENTITY_CONVERTERS.put(mediaType, converter);
    }

    public HttpClient(OrchestrationStrategy strategy, ArrowheadSecurityContext securityContext) {
        final boolean secure = strategy.isSecure();
        this.strategy = strategy;
        this.securityContext = securityContext;

        if (secure ^ securityContext != null)
            throw new AuthException(String.format("Client is %s, but trying to set security context to %s)",
                    secure ? "secure" : "insecure", securityContext == null ? "null" : "not null"));

        client = secure ?
                SecurityUtils.createClient(securityContext.getSslContext()) :
                insecureClient;
    }

    public boolean isSecure() {
        return strategy.isSecure();
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

        return strategy.request(this, method, appendUri, payload);
    }

    Response send(URI uri, HttpClient.Method method, Set<String> interfaces, Object payload) {
        for (String i : interfaces) {
            try {
                HttpClient.MediaType mediaType = HttpClient.MediaType.fromInterface(i);
                return send(uri, method, mediaType, payload);
            } catch (ArrowheadException e) {
                log.warn("Unknown interface", e);
            }
        }

        throw new ArrowheadRuntimeException("No compatible interface found");
    }

    Response send(URI uri, HttpClient.Method method, MediaType mediaType, Object payload) {
        try {
            final Entity<?> entity = DEFAULT_ENTITY_CONVERTERS.get(mediaType).apply(payload);
            log.info(String.format("%s %s", method.toString(), uri.toString()));
            final Invocation.Builder client = this.client
                    .target(uri)
                    .request()
                    .header("Content-type", mediaType.toString());
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
     * Subset of those registered with IANA
     */
    public enum MediaType {
        JSON("application/json"), // [RFC8259]
        XML("application/xml"),   // [RFC7303]
        ;

        private final String type;

        // TODO Could use a better way - use MediaType in SR?
        public static HttpClient.MediaType fromInterface(final String s) throws ArrowheadException {
            if (s.equalsIgnoreCase("JSON"))
                return HttpClient.MediaType.JSON;
            if (s.equalsIgnoreCase("XML"))
                return HttpClient.MediaType.XML;
            throw new ArrowheadException("Unknown interface");
        }

        MediaType(final String type) {
            this.type = type;
        }

        @Override
        public String toString() {
            return type;
        }
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
