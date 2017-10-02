package eu.arrowhead.ArrowheadProvider;

import eu.arrowhead.ArrowheadProvider.common.Utility;
import eu.arrowhead.ArrowheadProvider.common.model.MeasurementEntry;
import eu.arrowhead.ArrowheadProvider.common.model.TemperatureReadout;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

@Produces(MediaType.APPLICATION_JSON)
@Path("temperature")
public class TemperatureResource {

  @GET
  public Response getIt(@Context SecurityContext context, @QueryParam("token") String token, @QueryParam("signature") String signature) {
    MeasurementEntry entry = new MeasurementEntry("Temperature_IndoorTemperature", 21.0, System.currentTimeMillis());
    if (context.isSecure()) {
      TemperatureReadout readout = new TemperatureReadout("TemperatureSensors_SecureTemperatureSensor", System.currentTimeMillis(), "celsius", 1);
      readout.getE().add(entry);
      return Utility.verifyRequester(context, token, signature, readout);
    }
    TemperatureReadout readout = new TemperatureReadout("TemperatureSensors_InsecureTemperatureSensor", System.currentTimeMillis(), "celsius", 1);
    readout.getE().add(entry);
    return Response.status(200).entity(readout).build();
  }

}
