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

public class RestClient {
    private static final Client insecureClient = SecurityUtils.createClient(null);
    protected final Logger log = Logger.getLogger(getClass());
    private String address;
    private Integer port;
    private ArrowheadSecurityContext securityContext;
    private Client secureClient;

    // TODO Address and port are currently used very weirdly by sendRequest, Thomas
    public static RestClient create(ArrowheadSecurityContext securityContext) {
        return new RestClient(null, null)
                .setSecurityContext(securityContext);
    }

    // TODO Remove parameters and use set functions?, Thomas
    protected RestClient(String address, Integer port) {
        this.address = address;
        this.port = port;
    }

    public String getAddress() {
        return address;
    }

    public RestClient setAddress(String address) {
        this.address = address;
        return this;
    }

    public Integer getPort() {
        return port;
    }

    public RestClient setPort(Integer port) {
        this.port = port;
        return this;
    }

    public ArrowheadSecurityContext getSecurityContext() {
        return securityContext;
    }

    public RestClient setSecurityContext(ArrowheadSecurityContext securityContext) {
        this.securityContext = securityContext;
        secureClient = SecurityUtils.createClient(securityContext.getSslContext());
        return this;
    }

    /**
     * Sends a HTTP request to the given url, with the given HTTP method type and given payload
     */
    public <T> Response sendRequest(String uri, String method, T payload) {
        boolean isSecure = false;
        if (uri == null) {
            throw new NullPointerException("send (HTTP) request method received null URL");
        }
        if (uri.startsWith("https")) {
            isSecure = true;
        }

        if (isSecure && (securityContext == null || securityContext.getSslContext() == null)) {
            throw new AuthException(
                    "SSL Context is not set, but secure request sending was invoked. An insecure module can not send requests to secure modules.",
                    Response.Status.UNAUTHORIZED.getStatusCode());
        }
        Client client = isSecure ? secureClient : insecureClient;

        Invocation.Builder request = client.target(UriBuilder.fromUri(uri).build()).request().header("Content-type", "application/json");
        Response response; // will not be null after the switch-case
        try {
            switch (method) {
                case "GET":
                    response = request.get();
                    break;
                case "POST":
                    response = request.post(Entity.json(payload));
                    break;
                case "PUT":
                    response = request.put(Entity.json(payload));
                    break;
                case "DELETE":
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
            handleException(response, uri);
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
