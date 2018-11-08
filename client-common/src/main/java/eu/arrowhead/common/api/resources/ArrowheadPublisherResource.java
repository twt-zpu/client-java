package eu.arrowhead.common.api.resources;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import java.util.Map;

public interface ArrowheadPublisherResource {
    @POST
    @Path("feedback")
    Response receiveEvent(Map<String, Boolean> results);
}
