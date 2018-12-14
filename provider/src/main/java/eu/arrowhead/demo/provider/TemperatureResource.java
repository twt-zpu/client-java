/*
 *  Copyright (c) 2018 AITIA International Inc.
 *
 *  This work is part of the Productive 4.0 innovation project, which receives grants from the
 *  European Commissions H2020 research and innovation programme, ECSEL Joint Undertaking
 *  (project no. 737459), the free state of Saxony, the German Federal Ministry of Education and
 *  national funding authorities from involved countries.
 */

package eu.arrowhead.demo.provider;

import eu.arrowhead.common.api.server.ArrowheadHttpServer;
import eu.arrowhead.common.api.server.ArrowheadResource;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.demo.model.MeasurementEntry;
import eu.arrowhead.demo.model.TemperatureReadout;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("temperature")
@Produces(MediaType.APPLICATION_JSON)
public class TemperatureResource extends ArrowheadResource {

  public TemperatureResource(ArrowheadHttpServer server) throws ArrowheadException {
    super(server);
  }

  @GET
  public Response getIt() {
    if (ProviderMain.customResponsePayload != null) {
      return Response.status(200).entity(ProviderMain.customResponsePayload).build();
    } else {
      MeasurementEntry entry = new MeasurementEntry("Temperature_IndoorTemperature", 21.0, System.currentTimeMillis());
      TemperatureReadout readout = new TemperatureReadout("TemperatureSensor", System.currentTimeMillis(), "celsius", 1);
      readout.getE().add(entry);
      return Response.status(200).entity(readout).build();
    }

  }

}
