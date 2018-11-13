package eu.arrowhead.common.api.resources;

import eu.arrowhead.common.api.clients.core.EventHandlerClient;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import java.util.Map;

/**
 * Interface helper for implementing resources for publishers. You should probably also extend
 * {@link ArrowheadResource} if you are implementing this.
 *
 * See {@link EventHandlerClient} for how to publish events.
 */
public interface ArrowheadPublisherResource {

    /**
     * Following each publish, this will be called by the EventHandler with the results.
     * @param results the results of publishing an event.
     * @return A response to the event handler (<code>Response.ok().build()</code> would be a good option in most cases.
     */
    @POST
    @Path("feedback")
    Response receiveEvent(Map<String, Boolean> results);
}
