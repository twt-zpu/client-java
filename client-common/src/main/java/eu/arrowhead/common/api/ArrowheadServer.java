package eu.arrowhead.common.api;

import eu.arrowhead.common.exception.ArrowheadRuntimeException;
import org.apache.log4j.Logger;

import java.util.HashSet;
import java.util.Set;

public abstract class ArrowheadServer {
    private static final Set<ArrowheadServer> servers = new HashSet<>();
    protected final Logger log = Logger.getLogger(getClass());

    public static void stopAll() {
        servers.forEach(ArrowheadServer::stop);
    }

    public ArrowheadServer start() {
        if (isStarted())
            throw new ArrowheadRuntimeException("Server already started");

        onStart();
        servers.add(this);
        return this;
    }

    public ArrowheadServer stop() {
        servers.remove(this);
        onStop();
        return this;
    }

    public boolean isStarted() {
        return servers.contains(this);
    }

    public abstract void onStart();

    public abstract void onStop();
}
