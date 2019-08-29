package eu.arrowhead.client.provider;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;

import eu.arrowhead.client.common.model.FeldbusCouplerL;


@Path("/")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)

public class FeldbusCouplerLResource {
	static final String SERVICE_GET_URI = "modbus/GetCoils";
	static final String SERVICE_SET_URI = "modbus/SetCoils";
	private static FeldbusCouplerLMeasurement measurement; 

	@GET
	@Path(SERVICE_GET_URI)
	public Response getCoils(@Context SecurityContext context, @QueryParam("token") String token, @QueryParam("signature") String signature) {
	    String providerName = "FeldbusCouplerL";
	    FeldbusCouplerL coils = new FeldbusCouplerL(providerName, System.currentTimeMillis(), " ", 1);
	    coils.getE().add(measurement.entry);
	    return Response.status(Status.OK).entity(coils).build();
	}
	
	@GET
	@Path(SERVICE_SET_URI)
	public Response setCoils(@QueryParam("coil") List<String> coilsList) {
		boolean[] coilsArray = new boolean[coilsList.size()];
		int index = 0;
		for (String coil : coilsList){
			coilsArray[index++] = Boolean.valueOf(coil);
		}
		if (measurement.entry.setOutput(coilsArray)){
			measurement.setExistence(true);
		}
	    return Response.status(Status.ACCEPTED).entity(measurement.entry).build();
	}
}
