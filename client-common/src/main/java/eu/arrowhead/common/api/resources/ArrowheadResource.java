package eu.arrowhead.common.api.resources;

import eu.arrowhead.common.api.server.ArrowheadHttpServer;
import eu.arrowhead.common.misc.SecurityVerifier;
import org.apache.log4j.Logger;

/**
 * Helper class for creating resources for {@link ArrowheadHttpServer}. The verifier object should be used to verify
 * each request before any action is taken, see {@link SecurityVerifier}, and the server object can be used to consume
 * services as part of responding to a request.
 *
 * See {@link eu.arrowhead.common.api.server.ArrowheadGrizzlyHttpServer#addResources}() on how to add resources to the
 * Grizzly implementation of {@link ArrowheadHttpServer}.
 *
 * See {@link ArrowheadPublisherResource} and {@link ArrowheadSubscriberResource} for helper interfaces for creating
 * publisher and subscriber resources.
 */
public abstract class ArrowheadResource {
    protected final Logger log = Logger.getLogger(getClass());
    protected final SecurityVerifier verifier = SecurityVerifier.createFromProperties();
    protected final ArrowheadHttpServer server;

    /**
     * Constructor.
     * @param server the {@link ArrowheadHttpServer} that this resource belongs to.
     */
    public ArrowheadResource(ArrowheadHttpServer server) {
        this.server = server;
    }
}
