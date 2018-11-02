package eu.arrowhead.common.resources;

import eu.arrowhead.common.model.Event;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

public interface ArrowheadSubscriberResource {
    @POST
    @Path("notify")
    Response receiveEvent(Event event);
}
