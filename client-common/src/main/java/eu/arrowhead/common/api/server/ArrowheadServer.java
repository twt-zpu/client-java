package eu.arrowhead.common.api.server;

import eu.arrowhead.common.api.ArrowheadApplication;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.ArrowheadRuntimeException;
import java.util.HashSet;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Simple server instance that can be started and stopped, all started servers are kept in a collection and can thus
 * be stopped by a call to the static method stopAll(). Note that for inheritors of {@link ArrowheadApplication}, this
 * will happen automatically during shutdown sequence.
 *
 * This class contains very minimal functionality and thus can be used to implement any kind of server, that needs the
 * above functionality.
 *
 * See also {@link ArrowheadHttpServer} and {@link ArrowheadGrizzlyHttpServer}.
 */
public abstract class ArrowheadServer {
    private static final Logger LOG = LogManager.getLogger(ArrowheadServer.class);
    private static final Set<ArrowheadServer> servers = new HashSet<>();
    protected final Logger log = LogManager.getLogger(getClass());

    /**
     * Stop all servers that was ever started. Note this will be called automatically by
     * {@link eu.arrowhead.common.api.ArrowheadApplication} if this class is used.
     */
    public static void stopAll() {
        while (!servers.isEmpty()) {
            final ArrowheadServer server = servers.iterator().next();
            server.stop();
            if (servers.contains(server)) {
                LOG.warn("Should not be here");
                servers.remove(server);
            }
        }
    }

    /**
     * Start the server. Note that inheritors should probably implement the onStart() method instead, which will be
     * called automatically during this.
     * @return this.
     */
    public ArrowheadServer start() throws ArrowheadException {
        if (isStarted())
            throw new ArrowheadRuntimeException("Server already started");

        onStart();
        servers.add(this);
        return this;
    }

    /**
     * Stop the server. Note that inheritors should probably implement the onStop() method instead, which will be
     * called automatically during this.
     * @return this.
     */
    public ArrowheadServer stop() {
        servers.remove(this);
        onStop();
        return this;
    }

    /**
     * Checks whether the service is present in the service collection. Note that this does not say anything about
     * whether the server is actually running - just that it was once started and have not been explicitly stopped
     * since.
     * @return true/false.
     */
    public boolean isStarted() {
        return servers.contains(this);
    }

    /**
     * Implement your start routine here. On errors you should throw an exception to prevent the server to be
     * registered in the collection of running servers.
     */
    protected abstract void onStart() throws ArrowheadException;

    /**
     * Implement your stop routine here. Notice that your server will be removed from the collection before this method
     * is called.
     */
    protected abstract void onStop();
}
