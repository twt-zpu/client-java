package eu.arrowhead.common.api.clients;

import eu.arrowhead.common.api.ArrowheadSecurityContext;
import eu.arrowhead.common.exception.*;
import eu.arrowhead.common.misc.SecurityUtils;
import eu.arrowhead.common.misc.Utility;
import org.apache.log4j.Logger;

import javax.ws.rs.NotAllowedException;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;

public class RestClient {
    public enum Method {
        GET, PUT, POST, DELETE;
    }

    private static final Client insecureClient = SecurityUtils.createClient(null);
    protected final Logger log = Logger.getLogger(getClass());
    private ArrowheadSecurityContext securityContext;
    private Client secureClient;
    private UriBuilder uriBuilder = UriBuilder.fromPath("");
    private boolean isSecure;

    public static RestClient create(boolean secure, String uri, ArrowheadSecurityContext securityContext) {
        return new RestClient(secure)
                .setUri(uri)
                .setSecurityContext(securityContext);
    }

    /**
     *
     * @param secure Requires explicitly setting security, to avoid situations where the user expects a secure
     *               connection, but it really isn't due to the lack of security context or wrong URI scheme.
     */
    protected RestClient(boolean secure) {
        isSecure = secure;
        uriBuilder.scheme(secure ? "https" : "http");
    }

    public RestClient setAddress(String address) {
        uriBuilder.host(address);
        return this;
    }

    public RestClient setPort(int port) {
        uriBuilder.port(port);
        return this;
    }

    public RestClient setSecurityContext(ArrowheadSecurityContext securityContext) {
        this.securityContext = securityContext;
        if (isSecure ^ securityContext != null)
            throw new AuthException(String.format("Client is %s, but trying to set security context to %s)",
                    isSecure ? "secure" : "insecure", securityContext == null ? "null" : "not null"));
        if (securityContext != null)
            secureClient = SecurityUtils.createClient(securityContext.getSslContext());
        return this;
    }

    public RestClient setServicePath(String path) {
        uriBuilder.replacePath(path);
        return this;
    }

    public RestClient setUri(String uri) {
        uriBuilder = UriBuilder.fromUri(uri);
        final String scheme = uriBuilder.build().getScheme();
        if (isSecure ^ scheme.equals("https"))
            throw new AuthException(
                    String.format("URI scheme does not match security setting (secure = %s, scheme = %s",
                            isSecure, scheme));
        return this;
    }

    public ArrowheadSecurityContext getSecurityContext() {
        return securityContext;
    }

    public boolean isSecure() {
        return isSecure;
    }

    public RestClient setSecure(boolean secure) {
        isSecure = secure;
        uriBuilder.scheme(secure ? "https" : "http");
        return this;
    }

    public <T> Response sendRequest(Method method, T payload) {
        return sendRequest(method, null, payload);
    }

    public <T> Response sendRequest(Method method) {
        return sendRequest(method, null, null);
    }

    /**
     * Sends a HTTP request to the given url, with the given HTTP method type and given payload
     */
    public <T> Response sendRequest(Method method, String path, T payload) {
        if (isSecure ^ securityContext != null)
            throw new AuthException(String.format("Client is %s, but security context is %s)",
                    isSecure ? "secure" : "insecure", securityContext == null ? "null" : "not null"));

        URI uri = path != null ?
                uriBuilder.clone().path(path).build() :
                uriBuilder.build();

        final String scheme = uri.getScheme();
        if (isSecure ^ scheme.equals("https"))
            throw new AuthException(
                    String.format("URI scheme does not match security setting (secure = %s, scheme = %s",
                            isSecure, scheme));

        Client client = isSecure ? secureClient : insecureClient;

        Invocation.Builder request = client.target(UriBuilder.fromUri(uri).build()).request().header("Content-type", "application/json");
        Response response; // will not be null after the switch-case
        try {
            switch (method) {
                case GET:
                    response = request.get();
                    break;
                case POST:
                    response = request.post(Entity.json(payload));
                    break;
                case PUT:
                    response = request.put(Entity.json(payload));
                    break;
                case DELETE:
                    response = request.delete();
                    break;
                default:
                    throw new NotAllowedException("Invalid method type was given to the Utility.sendRequest() method");
            }
        } catch (ProcessingException e) {
            if (e.getCause().getMessage().contains("PKIX path")) {
                throw new AuthException("The system at " + uri + " is not part of the same certificate chain of trust!", Response.Status.UNAUTHORIZED.getStatusCode(),
                        e);
            } else {
                throw new UnavailableServerException("Could not get any response from: " + uri, Response.Status.SERVICE_UNAVAILABLE.getStatusCode(), e);
            }
        }

        // If the response status code does not start with 2 the request was not successful
        if (!(response.getStatusInfo().getFamily() == Response.Status.Family.SUCCESSFUL)) {
            handleException(response, uri.toString());
        }

        return response;
    }

    private void handleException(Response response, String uri) {
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
}
