package eu.arrowhead.common.api.clients;

import eu.arrowhead.common.exception.*;
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

public class RestRequest {
    protected final Logger log = Logger.getLogger(getClass());
    private final Client client;
    private final RestClient.Method method;
    private final UriBuilder uriBuilder;

    public RestRequest(Client client, RestClient.Method method, UriBuilder uriBuilder) {
        this.client = client;
        this.method = method;
        this.uriBuilder = uriBuilder;
    }

    public RestRequest queryParam(String s, Object... objects) {
        uriBuilder.queryParam(s, objects);
        return this;
    }

    public Response send() {
        return send(null);
    }

    public <T> Response send(T payload) {
        final URI uri = uriBuilder.build();
        Invocation.Builder request = client
                .target(uri)
                .request()
                .header("Content-type", "application/json");

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
