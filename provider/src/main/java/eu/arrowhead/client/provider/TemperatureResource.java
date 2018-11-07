/*
 *  Copyright (c) 2018 AITIA International Inc.
 *
 *  This work is part of the Productive 4.0 innovation project, which receives grants from the
 *  European Commissions H2020 research and innovation programme, ECSEL Joint Undertaking
 *  (project no. 737459), the free state of Saxony, the German Federal Ministry of Education and
 *  national funding authorities from involved countries.
 */

package eu.arrowhead.client.provider;

import eu.arrowhead.client.common.exception.ErrorMessage;
import eu.arrowhead.client.common.exception.ExceptionType;
import eu.arrowhead.client.common.model.MeasurementEntry;
import eu.arrowhead.client.common.model.TemperatureReadout;
import eu.arrowhead.client.provider.ProviderService.TokenValidationResult;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

@Path("/")
@Produces(MediaType.APPLICATION_JSON)
//REST service example
public class TemperatureResource {

  static final String SERVICE_URI = "temperature";

  @GET
  @Path(SERVICE_URI)
  public Response getIt(@Context SecurityContext context, @QueryParam("token") String token, @QueryParam("signature") String signature) {
    String providerName;
    if (context.isSecure()) {
      TokenValidationResult validationResult = ProviderService.verifyRequester(context, token, signature);
      switch (validationResult) {
        case SIGNATURE:
          ErrorMessage signatureError = new ErrorMessage("Authorization core system signature verification failed!", 401, ExceptionType.AUTH,
                                                         SERVICE_URI);
          return Response.status(401).entity(signatureError).build();
        case EXPIRED:
          ErrorMessage expiredError = new ErrorMessage("Given token has expired!", 401, ExceptionType.AUTH, SERVICE_URI);
          return Response.status(401).entity(expiredError).build();
        case MISMATCH:
          ErrorMessage mismatchError = new ErrorMessage("Cert common name and token information are mismatched!", 401, ExceptionType.AUTH,
                                                        SERVICE_URI);
          return Response.status(401).entity(mismatchError).build();
        case SERVER_ERROR:
          ErrorMessage serverError = new ErrorMessage("Internal Server Error during token validation!", 500, ExceptionType.AUTH, SERVICE_URI);
          return Response.status(500).entity(serverError).build();
        case OK:
          break;
      }
      providerName = "TemperatureSensors_SecureTemperatureSensor";
    } else {
      providerName = "TemperatureSensors_InsecureTemperatureSensor";
    }

    if (FullProviderMain.customResponsePayload != null) {
      return Response.status(200).entity(FullProviderMain.customResponsePayload).build();
    } else {
      MeasurementEntry entry = new MeasurementEntry("Temperature_IndoorTemperature", 21.0, System.currentTimeMillis());
      TemperatureReadout readout = new TemperatureReadout(providerName, System.currentTimeMillis(), "celsius", 1);
      readout.getE().add(entry);
      return Response.status(200).entity(readout).build();
    }
  }

}
