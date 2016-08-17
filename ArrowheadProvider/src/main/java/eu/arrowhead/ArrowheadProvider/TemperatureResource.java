package eu.arrowhead.ArrowheadProvider;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("temperature")
public class TemperatureResource {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response getIt() {
    	String temperature = "21";
        return Response.ok().entity(temperature).build();
    }
}
