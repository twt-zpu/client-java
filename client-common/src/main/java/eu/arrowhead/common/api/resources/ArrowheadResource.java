package eu.arrowhead.common.api.resources;

import eu.arrowhead.common.api.ArrowheadHttpServer;
import eu.arrowhead.common.misc.SecurityVerifier;

public abstract class ArrowheadResource {
    protected final SecurityVerifier verifier = SecurityVerifier.createFromProperties();
    protected final ArrowheadHttpServer server;

    public ArrowheadResource(ArrowheadHttpServer server) {
        this.server = server;
    }
}
