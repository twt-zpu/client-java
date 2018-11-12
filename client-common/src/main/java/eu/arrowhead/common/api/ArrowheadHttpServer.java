package eu.arrowhead.common.api;

import eu.arrowhead.common.exception.ArrowheadRuntimeException;
import eu.arrowhead.common.exception.AuthException;
import eu.arrowhead.common.misc.SecurityUtils;
import eu.arrowhead.common.misc.Utility;

import java.io.IOException;
import java.net.ServerSocket;
import java.security.cert.X509Certificate;
import java.util.Base64;

public abstract class ArrowheadHttpServer extends ArrowheadServer {
    private String base64PublicKey;
    private String baseUri;
    private boolean isSecure;
    private String address;
    private int port;
    private ArrowheadSecurityContext securityContext;
    private String serverCN;

    public boolean isSecure() {
        return isSecure;
    }

    public ArrowheadHttpServer setSecure(boolean secure) {
        isSecure = secure;
        return this;
    }

    public String getAddress() {
        return address;
    }

    public ArrowheadHttpServer setAddress(String address) {
        this.address = address;
        return this;
    }

    public int getPort() {
        return port;
    }

    public ArrowheadHttpServer setPort(int port) {
        this.port = port;
        return this;
    }

    public ArrowheadSecurityContext getSecurityContext() {
        return securityContext;
    }

    public ArrowheadHttpServer setSecurityContext(ArrowheadSecurityContext securityContext) {
        this.securityContext = securityContext;
        return this;
    }

    protected int nextFreePort(int from, int to) {
        for (int port = from; port <= to; port++) {
            ServerSocket s = null;
            try {
                s = new ServerSocket(port);
                return port;
            } catch (IOException ignored) {
            } finally {
                try {
                    if (s != null) s.close();
                } catch (IOException e) {
                    throw new ArrowheadRuntimeException("Error occurred during port detection:", e);
                }
            }
        }
        throw new ArrowheadRuntimeException("Unable to find a free port");
    }

    public String getBaseUri() {
        return baseUri;
    }

    public String getBase64PublicKey() {
        return base64PublicKey;
    }

    public String getServerCN() {
        return serverCN;
    }

    @Override
    public ArrowheadHttpServer start() {
        if (isSecure ^ securityContext != null)
            throw new ArrowheadRuntimeException("Both or neither of isSecure and securityContext must be set");

        // Port 9803-9874 is the 4-digit largest port range currently marked as unassigned by IANA
        if (port == 0) port = nextFreePort(9803, 9874);
        baseUri = Utility.getUri(address, port, null, isSecure, true);

        X509Certificate serverCert = SecurityUtils.getFirstCertFromKeyStore(securityContext.getKeyStore());
        base64PublicKey = Base64.getEncoder().encodeToString(serverCert.getPublicKey().getEncoded());

        serverCN = SecurityUtils.getCertCNFromSubject(serverCert.getSubjectDN().getName());
        if (!SecurityUtils.isKeyStoreCNArrowheadValid(serverCN)) {
            throw new AuthException("Server CN ( " + serverCN + ") is not compliant with the Arrowhead cert" +
                    " structure, since it does not have 5 parts, or does not end with \"arrowhead.eu\".");
        }

        super.start();

        log.info("Started " + (isSecure ? "secure" : "insecure") + " server at: " + baseUri);
        return this;
    }

    @Override
    public ArrowheadHttpServer stop() {
        super.stop();
        return this;
    }
}
