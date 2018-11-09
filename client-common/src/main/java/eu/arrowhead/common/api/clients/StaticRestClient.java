package eu.arrowhead.common.api.clients;

import eu.arrowhead.common.api.ArrowheadSecurityContext;
import eu.arrowhead.common.exception.AuthException;

import javax.ws.rs.core.UriBuilder;

public class StaticRestClient extends RestClient {
    private UriBuilder uriBuilder = UriBuilder.fromPath("");

    public static RestClient create(boolean secure, String uri, ArrowheadSecurityContext securityContext) {
        return new StaticRestClient(secure)
                .setUri(uri)
                .setSecurityContext(securityContext);
    }

    /**
     * @param secure Requires explicitly setting security, to avoid situations where the user expects a secure
     *               connection, but it really isn't due to the lack of security context or wrong URI scheme.
     */
    protected StaticRestClient(boolean secure) {
        super(secure);
        uriBuilder.scheme(secure ? "https" : "http");
    }

    private StaticRestClient(boolean secure, UriBuilder uriBuilder) {
        super(secure);
        this.uriBuilder = uriBuilder;
    }

    protected StaticRestClient setAddress(String address) {
        uriBuilder.host(address);
        return this;
    }

    protected StaticRestClient setPort(int port) {
        uriBuilder.port(port);
        return this;
    }

    protected StaticRestClient replacePath(String path) {
        uriBuilder.replacePath(path);
        return this;
    }

    protected StaticRestClient addPath(String path) {
        uriBuilder.path(path);
        return this;
    }

    @Override
    public StaticRestClient clone() {
        return new StaticRestClient(isSecure(), uriBuilder.clone())
                .setSecurityContext(getSecurityContext());
    }

    public StaticRestClient clone(String path) {
        return new StaticRestClient(isSecure(), uriBuilder.clone().path(path))
                .setSecurityContext(getSecurityContext());
    }

    public StaticRestClient clone(String... paths) {
        final UriBuilder uriBuilder = this.uriBuilder.clone();
        for (String path : paths)
            uriBuilder.path(path);
        return new StaticRestClient(isSecure(), uriBuilder)
                .setSecurityContext(getSecurityContext());
    }

    protected StaticRestClient setUri(String uri) {
        uriBuilder = UriBuilder.fromUri(uri);
        final String scheme = uriBuilder.build().getScheme();
        if (isSecure() ^ scheme.equals("https"))
            throw new AuthException(
                    String.format("URI scheme does not match security setting (secure = %s, scheme = %s",
                            isSecure(), scheme));
        return this;
    }

    @Override
    protected StaticRestClient setSecure(boolean secure) {
        super.setSecure(secure);
        uriBuilder.scheme(secure ? "https" : "http");
        return this;
    }

    @Override
    protected UriBuilder onRequest(Method method) {
        final String scheme = uriBuilder.build().getScheme();
        if (isSecure() ^ scheme.equals("https"))
            throw new AuthException(
                    String.format("URI scheme does not match security setting (secure = %s, scheme = %s",
                            isSecure(), scheme));
        return uriBuilder;
    }

    @Override
    protected StaticRestClient setSecurityContext(ArrowheadSecurityContext securityContext) {
        super.setSecurityContext(securityContext);
        return this;
    }
}
