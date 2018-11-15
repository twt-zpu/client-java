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

/**
 * Use instances of this to interact with HTTP based Arrowhead services.
 *
 * To interact with Arrowhead Core Services, see {@link eu.arrowhead.common.api.clients.core}
 */
public class HttpClient {
    private static final Client insecureClient = SecurityUtils.createClient(null);
    private static final Map<Interface, Function<Object, Entity<?>>> DEFAULT_ENTITY_CONVERTERS = new HashMap<>();
    private static final Map<String, Interface> DEFAULT_MEDIA_TYPES = new HashMap<>();

    protected final Logger log = Logger.getLogger(getClass());
    private final OrchestrationStrategy strategy;
    private final ArrowheadSecurityContext securityContext;
    private final Client client;
    private final Map<Interface, Function<Object, Entity<?>>> entityConverters = new HashMap<>();
    private final Map<String, Interface> mediaTypes = new HashMap<>();

    static {
        addDefaultEntityConverter(Interface.JSON, Entity::json);
        addDefaultEntityConverter(Interface.XML, Entity::xml);
    }

    /**
     * Add an additional default converter to convert payloads to what is required by the Arrowhead Service. Default
     * converters are shared between all instances of HttpClient, for instance specific converters see
     * {@link HttpClient#addEntityConverter}.
     * @param anInterface the interface to convert to.
     * @param converter a function to do the conversion, can be null to deactivate.
     */
    public static void addDefaultEntityConverter(Interface anInterface, Function<Object, Entity<?>> converter) {
        DEFAULT_ENTITY_CONVERTERS.put(anInterface, converter);
        DEFAULT_MEDIA_TYPES.put(anInterface.getInterface(), anInterface);
    }

    /**
     * Construct a new HttpClient
     * @param strategy the orchestration strategy to use in the client, see {@link OrchestrationStrategy}
     * @param securityContext the security context to use.
     */
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

    /**
     * Add an additional converter to convert payloads to what is required by the Arrowhead Service. These converters
     * override the default converters. To set a default converter for all HttpClient instances, see
     * {@link HttpClient#addDefaultEntityConverter}.
     * @param anInterface the interface to convert to.
     * @param converter a function to do the conversion, can be null to deactivate.
     */
    public void addEntityConverter(Interface anInterface, Function<Object, Entity<?>> converter) {
        entityConverters.put(anInterface, converter);
        mediaTypes.put(anInterface.getInterface(), anInterface);
    }

    /**
     * Send a request to the service covered by this client.
     * @param method HTTP method.
     * @return the response.
     */
    public Response request(Method method) {
        return request(method, null, null);
    }

    /**
     * Send a request to the service covered by this client.
     * @param method HTTP method.
     * @param payload the payload to send along, will be converted to the appropriate format automatically.
     * @return the response.
     */
    public <T> Response request(Method method, T payload) {
        return request(method, null, payload);
    }

    /**
     * Send a request to the service covered by this client.
     * @param method HTTP method.
     * @param appendUri Additional data to append to the URI, e.g. path, query parameters. Do not set scheme, host,
     *                  port and token/signature parameters, these will be set automatically by the orchestration
     *                  strategy.
     * @return the response.
     */
    public Response request(Method method, UriBuilder appendUri) {
        return request(method, appendUri, null);
    }

    /**
     * Send a request to the service covered by this client.
     * @param method HTTP method.
     * @param appendUri Additional data to append to the URI, e.g. path, query parameters. Do not set scheme, host,
     *                  port and token/signature parameters, these will be set automatically by the orchestration
     *                  strategy.
     * @param payload the payload to send along, will be converted to the appropriate format automatically.
     * @return the response.
     */
    public <T> Response request(Method method, UriBuilder appendUri, T payload) {
        appendUri = appendUri == null ? UriBuilder.fromUri("") : appendUri.clone();

        final URI uri = appendUri.build();
        if (uri.getScheme() != null) throw new ArrowheadRuntimeException("Append URI should not contain a scheme");
        if (uri.getHost() != null) throw new ArrowheadRuntimeException("Append URI should not contain a host");
        if (uri.getPort() != -1) throw new ArrowheadRuntimeException("Append URI should not contain a port");

        return strategy.request(this, method, appendUri, payload);
    }

    /**
     * Send raw request. This is intended for use by {@link OrchestrationStrategy} only, see
     * {@link OrchestrationStrategy#send} if you are creating a new strategy.
     * @param uri URI of the server.
     * @param method HTTP method.
     * @param interfaces set of the accepted Arrowhead interfaces.
     * @param payload the payload to send along.
     * @return the response.
     */
    Response send(URI uri, HttpClient.Method method, Set<String> interfaces, Object payload) {
        for (String i : interfaces) {
            Interface anInterface = mediaTypes.containsKey(i) ? mediaTypes.get(i) : DEFAULT_MEDIA_TYPES.get(i);
            if (anInterface != null) return send(uri, method, anInterface, payload);
        }

        throw new ArrowheadRuntimeException("No compatible interface found");
    }

    /**
     * Send raw request. This is intended for use by {@link OrchestrationStrategy} only, see
     * {@link OrchestrationStrategy#send} if you are creating a new strategy.
     * @param uri URI of the server.
     * @param method HTTP method.
     * @param anInterface the accepted Arrowhead interface.
     * @param payload the payload to send along.
     * @return the response.
     */
    Response send(URI uri, HttpClient.Method method, Interface anInterface, Object payload) {
        try {
            final Entity<?> entity = (entityConverters.containsKey(anInterface) && entityConverters.get(anInterface) != null ?
                    entityConverters.get(anInterface) :
                    DEFAULT_ENTITY_CONVERTERS.get(anInterface))
                    .apply(payload);
            log.info(String.format("%s %s", method.toString(), uri.toString()));
            final Invocation.Builder client = this.client
                    .target(uri)
                    .request()
                    .header("Content-type", anInterface.getMediaType());
            return check(entity == null ?
                    client.method(method.toString()) :
                    client.method(method.toString(), entity), uri.toString());
        } catch (ProcessingException e) {
            if (e.getCause().getMessage().contains("PKIX path")) {
                throw new AuthException("The system at " + uri + " is not part of the same certificate chain of trust!",
                        Response.Status.UNAUTHORIZED.getStatusCode(), e);
            } else {
                throw new UnavailableServerException("Could not get any response from: " + uri,
                        Response.Status.SERVICE_UNAVAILABLE.getStatusCode(), e);
            }
        }
    }

    /**
     * Verify the response and throw exceptions if it failed.
     * @param response the response to verify.
     * @param uri the uri that was requested.
     * @return the same response as was given, if successful.
     */
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
     * Arrowhead interface definition.
     */
    public static class Interface {
        public static final Interface JSON = new Interface("JSON", "application/json"); // [RFC8259]
        public static final Interface XML = new Interface("XML", "application/xml");    // [RFC7303]

        private final String anInterface;
        private final String mediaType;

        /**
         * Create a new interface
         * @param anInterface the Arrowhead defined interface.
         * @param mediaType the HTTP media type.
         */
        public Interface(String anInterface, String mediaType) {
            this.anInterface = anInterface;
            this.mediaType = mediaType;
        }

        public String getInterface() {
            return anInterface;
        }

        public String getMediaType() {
            return mediaType;
        }
    }

    /**
     * HTTP methods (from rfc2616 / rfc5789)
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
