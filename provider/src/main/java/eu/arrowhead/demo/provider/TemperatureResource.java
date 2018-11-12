/*
 *  Copyright (c) 2018 AITIA International Inc.
 *
 *  This work is part of the Productive 4.0 innovation project, which receives grants from the
 *  European Commissions H2020 research and innovation programme, ECSEL Joint Undertaking
 *  (project no. 737459), the free state of Saxony, the German Federal Ministry of Education and
 *  national funding authorities from involved countries.
 */

package eu.arrowhead.demo.provider;

import eu.arrowhead.common.api.ArrowheadHttpServer;
import eu.arrowhead.common.api.resources.ArrowheadResource;
import eu.arrowhead.demo.model.MeasurementEntry;
import eu.arrowhead.demo.model.TemperatureReadout;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

@Path("temperature")
@Produces(MediaType.APPLICATION_JSON)
public class TemperatureResource extends ArrowheadResource {

  public TemperatureResource(ArrowheadHttpServer server) {
    super(server);
  }

  @GET
  public Response getIt(@Context SecurityContext context,
                        @QueryParam("token") String token,
                        @QueryParam("signature") String signature) {
    return verifier.verifiedResponse(context, token, signature, () -> {
      MeasurementEntry entry = new MeasurementEntry("Temperature_IndoorTemperature", 21.0, System.currentTimeMillis());
      TemperatureReadout readout = new TemperatureReadout("TemperatureSensors_" + (context.isSecure() ? "" : "in") + "SecureTemperatureSensor", System.currentTimeMillis(), "celsius", 1);
      readout.getE().add(entry);
      return readout;
    });
  }

}
