package eu.arrowhead.common.api.server;

import eu.arrowhead.common.exception.ArrowheadException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Helper class for creating resources for {@link ArrowheadHttpServer}. The server object can be used to consume
 * services as part of responding to a request.
 *
 * See {@link eu.arrowhead.common.api.server.ArrowheadGrizzlyHttpServer#addResources}() on how to add resources to the
 * Grizzly implementation of {@link ArrowheadHttpServer}.
 */
public abstract class ArrowheadResource {
    protected final Logger log = LogManager.getLogger(getClass());
    protected final ArrowheadHttpServer server;

    /**
     * Constructor.
     * @param server the {@link ArrowheadHttpServer} that this resource belongs to.
     */
    public ArrowheadResource(ArrowheadHttpServer server) throws ArrowheadException {
        this.server = server;
    }

}
