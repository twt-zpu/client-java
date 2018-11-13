package eu.arrowhead.common.api.resources;

import eu.arrowhead.common.api.clients.core.EventHandlerClient;
import eu.arrowhead.common.model.Event;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

/**
 * Interface helper for implementing resources for publishers. You should probably also extend
 * {@link ArrowheadResource} if you are implementing this.
 *
 * See {@link EventHandlerClient} for how to subscribe to events.
 */
public interface ArrowheadSubscriberResource {

    /**
     * This will be called when the EventHandler sends us a new event.
     * @param event the event.
     * @return A response to the event handler (<code>Response.ok().build()</code> would be a good option in most cases.
     */
    @POST
    @Path("notify")
    Response receiveEvent(Event event);
}
