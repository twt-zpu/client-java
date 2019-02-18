package eu.arrowhead.common.api.server;

import eu.arrowhead.common.api.ArrowheadSecurityContext;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.ArrowheadRuntimeException;
import eu.arrowhead.common.exception.AuthException;
import eu.arrowhead.common.exception.InvalidStateException;
import eu.arrowhead.common.exception.NotFoundException;
import eu.arrowhead.common.misc.SecurityUtils;
import eu.arrowhead.common.misc.Utility;
import java.io.IOException;
import java.net.ServerSocket;
import java.security.cert.X509Certificate;
import java.util.Base64;

/**
 * Basic functionality, required for most HTTP servers. For a concrete implementation see
 * {@link ArrowheadGrizzlyHttpServer}
 */
public abstract class ArrowheadHttpServer extends ArrowheadServer {
    private String base64PublicKey;
    private String baseUri;
    private boolean isSecure;
    private String address;
    private int port;
    private ArrowheadSecurityContext securityContext;
    private String cn;

    /**
     * Constructor for subclasses.
     * @param secure Secure HTTPS server?
     * @param address Address of server.
     * @param port Port of server, use 0 for automatic selection.
     * @param securityContext Security context. Note that this must be given if and only if secure=true.
     */
    protected ArrowheadHttpServer(boolean secure, String address, int port, ArrowheadSecurityContext securityContext) {
        if (secure ^ securityContext != null)
            throw new ArrowheadRuntimeException("Both or neither of secure and securityContext must be set");

        this.isSecure = secure;
        this.address = address;
        this.port = port;
        this.securityContext = securityContext;
    }

    public boolean isSecure() {
        return isSecure;
    }

    public String getAddress() {
        return address;
    }

    public int getPort() {
        return port;
    }

    public ArrowheadSecurityContext getSecurityContext() {
        return securityContext;
    }

    public ArrowheadHttpServer setSecure(boolean secure) {
        isSecure = secure;
        return this;
    }

    public ArrowheadHttpServer setAddress(String address) {
        this.address = address;
        return this;
    }

    public ArrowheadHttpServer setPort(int port) {
        this.port = port;
        return this;
    }

    /**
     * Search through a range to find a free port.
     * @param from lowest port number (inclusive).
     * @param to highest port number (inclusive).
     * @return a free port number,
     * @throws NotFoundException if no available port could be found.
     */
    private int nextFreePort(int from, int to) throws NotFoundException {
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
        throw new NotFoundException("Unable to find a free port");
    }

    /**
     * Get the baseUri of the server. Note that this is available only after the server is started.
     * @return the uri.
     */
    public String getBaseUri() {
        if (baseUri == null) throw new InvalidStateException("Server not started yet");
        return baseUri;
    }

    /**
     * Get the public key of the server in base64 format. Note that this is available only after the server is started.
     * @return the key.
     */
    public String getBase64PublicKey() {
        if (base64PublicKey == null) throw new InvalidStateException("Server not started yet");
        return base64PublicKey;
    }

    /**
     * Get the CN of the server. Note that this is available only after the server is started.
     * @return the CN.
     */
    public String getCN() {
        if (cn == null) throw new InvalidStateException("Server not started yet");
        return cn;
    }

    /**
     * Start the server. Note that implementors should probably implement the onStart() method and not override this.
     * @return this.
     * @throws NotFoundException if automatic port detection was chosen, but no free port could be found.
     */
    @Override
    public ArrowheadHttpServer start() throws ArrowheadException {
        if (isSecure ^ securityContext != null)
            throw new ArrowheadRuntimeException("Both or neither of isSecure and securityContext must be set");

        // Port 9803-9874 is the 4-digit largest port range currently marked as unassigned by IANA
        if (port == 0) port = nextFreePort(9803, 9874);
        baseUri = Utility.getUri(address, port, null, isSecure, true);

        if (isSecure) {
            X509Certificate serverCert = SecurityUtils.getFirstCertFromKeyStore(securityContext.getKeyStore());
            base64PublicKey = Base64.getEncoder().encodeToString(serverCert.getPublicKey().getEncoded());

            cn = SecurityUtils.getCertCNFromSubject(serverCert.getSubjectDN().getName());
            if (!SecurityUtils.isKeyStoreCNArrowheadValid(cn)) {
                throw new AuthException("Server CN ( " + cn + ") is not compliant with the Arrowhead cert" +
                        " structure, since it does not have 5 parts, or does not end with \"arrowhead.eu\".");
            }
        }

        super.start();

        log.info("Started " + (isSecure ? "secure" : "insecure") + " server at: " + baseUri);
        return this;
    }

    /**
     * Stop the server. Note that implementors should probably implement the onStop() method and not override this.
     * @return this.
     */
    @Override
    public ArrowheadHttpServer stop() {
        super.stop();
        baseUri = null;
        base64PublicKey = null;
        cn = null;
        return this;
    }
}
