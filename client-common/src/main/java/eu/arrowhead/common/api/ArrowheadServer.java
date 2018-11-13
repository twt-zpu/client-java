package eu.arrowhead.common.api;

import eu.arrowhead.common.exception.ArrowheadRuntimeException;
import eu.arrowhead.common.exception.NotFoundException;
import org.apache.log4j.Logger;

import java.util.HashSet;
import java.util.Set;

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
    private static final Set<ArrowheadServer> servers = new HashSet<>();
    protected final Logger log = Logger.getLogger(getClass());

    /**
     * Stop all running servers.
     */
    public static void stopAll() {
        servers.forEach(ArrowheadServer::stop);
    }

    /**
     * Start the server. Note that inheritors should probably implement the onStart() method instead, which will be
     * called automatically during this.
     * @return this.
     */
    public ArrowheadServer start() throws NotFoundException {
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
    public abstract void onStart();

    /**
     * Implement your stop routine here. Notice that your server will be removed from the collection before this method
     * is called.
     */
    public abstract void onStop();
}
