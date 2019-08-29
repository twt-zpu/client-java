package eu.arrowhead.client.provider;

import java.util.HashMap;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;

import eu.arrowhead.client.common.model.ModbusMeasurement;


@Path("/")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)

public class FeldbusCouplerLResource {
	static final String SERVICE_GET_URI = "modbus/GetCoils";
	static final String SERVICE_SET_URI = "modbus/SetCoils";
	private static ModbusData measurement; 

	@GET
	@Path(SERVICE_GET_URI)
	public Response getCoils(@Context SecurityContext context, @QueryParam("token") String token, @QueryParam("signature") String signature) {
	    String providerName = "FeldbusCouplerL";
	    ModbusMeasurement coils = new ModbusMeasurement(providerName, System.currentTimeMillis(), " ", 1);
	    coils.getE().add(measurement.getEntry());
	    return Response.status(Status.OK).entity(coils).build();
	}
	
	@GET
	@Path(SERVICE_SET_URI)
	public Response setCoils(@QueryParam("coil") List<String> coilsList) {
		HashMap<Integer, Boolean> coilsMap = new HashMap<Integer, Boolean>();
		for (String coil : coilsList){
			String[] coil_key_value = coil.split("-");
			int key = Integer.valueOf(coil_key_value[0]);
			boolean value = Boolean.valueOf(coil_key_value[1]);
			coilsMap.put(key, value);
		}
		measurement.getEntry().setCoilsOutput(coilsMap);
		measurement.setExistence(true);
	    return Response.status(Status.ACCEPTED).entity(measurement.getEntry()).build();
	}
}
