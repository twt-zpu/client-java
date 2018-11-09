package eu.arrowhead.common.api.resources;

import eu.arrowhead.common.api.ArrowheadServer;
import eu.arrowhead.common.misc.SecurityVerifier;

public abstract class ArrowheadResource {
    protected final SecurityVerifier verifier = SecurityVerifier.createFromProperties();
    protected final ArrowheadServer server;

    public ArrowheadResource(ArrowheadServer server) {
        this.server = server;
    }
}
