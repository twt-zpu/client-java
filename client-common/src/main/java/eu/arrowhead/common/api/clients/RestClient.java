package eu.arrowhead.common.api.clients;

import eu.arrowhead.common.api.ArrowheadSecurityContext;
import eu.arrowhead.common.exception.*;
import eu.arrowhead.common.misc.SecurityUtils;
import org.apache.log4j.Logger;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.UriBuilder;

public abstract class RestClient {
    // TODO RestClient is very limited, both in the number of methods but also restricted to JSON only at the moment, Thomas

    protected enum Method {
        GET, PUT, POST, DELETE
    }

    private static final Client insecureClient = SecurityUtils.createClient(null);
    protected final Logger log = Logger.getLogger(getClass());
    private ArrowheadSecurityContext securityContext;
    private Client secureClient;
    private boolean isSecure;

    /**
     *
     * @param secure Requires explicitly setting security, to avoid situations where the user expects a secure
     *               connection, but it really isn't due to the lack of security context or wrong URI scheme.
     */
    protected RestClient(boolean secure) {
        isSecure = secure;
    }

    protected RestClient setSecurityContext(ArrowheadSecurityContext securityContext) {
        this.securityContext = securityContext;
        if (isSecure ^ securityContext != null)
            throw new AuthException(String.format("Client is %s, but trying to set security context to %s)",
                    isSecure ? "secure" : "insecure", securityContext == null ? "null" : "not null"));
        if (securityContext != null)
            secureClient = SecurityUtils.createClient(securityContext.getSslContext());
        return this;
    }

    public ArrowheadSecurityContext getSecurityContext() {
        return securityContext;
    }

    public boolean isSecure() {
        return isSecure;
    }

    protected RestClient setSecure(boolean secure) {
        isSecure = secure;
        return this;
    }

    protected abstract UriBuilder onRequest(Method method);

    protected RestRequest request(Method method) {
        if (isSecure ^ securityContext != null)
            throw new AuthException(String.format("Client is %s, but security context is %s)",
                    isSecure ? "secure" : "insecure", securityContext == null ? "null" : "not null"));

        return new RestRequest(isSecure ? secureClient : insecureClient, method, onRequest(method).clone());
    }

    public RestRequest get() {
        return request(Method.GET);
    }

    public RestRequest put() {
        return request(Method.PUT);
    }

    public RestRequest post() {
        return request(Method.POST);
    }

    public RestRequest delete() {
        return request(Method.DELETE);
    }

}
