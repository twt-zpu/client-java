package eu.arrowhead.ArrowheadProvider;

import eu.arrowhead.ArrowheadProvider.common.Utility;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

@Path("temperature")
public class TemperatureResource {

  @GET
  @Produces(MediaType.TEXT_PLAIN)
  public Response getIt(@Context SecurityContext context, @QueryParam("token") String token, @QueryParam("signature") String signature) {
    String temperature = "21";
    if (context.isSecure()) {
      return Utility.verifyRequester(context, token, signature, temperature);
    }
    return Response.status(200).entity(temperature).build();
  }

}
