package eu.arrowhead.ArrowheadProvider.common;

import javax.ws.rs.NotAllowedException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

public final class Utility {
	
	private Utility(){
	}

	public static <T> Response sendRequest(String URI, String method, T payload){
		
		Response response = null;
		try{
		    Client client = ClientBuilder.newClient();

		    WebTarget target = client.target(UriBuilder.fromUri(URI).build());
		    switch(method){
		    case "GET": 
		        response = target.request().header("Content-type", "application/json").get();
		        break;
		    case "POST":
		        response = target.request().header("Content-type", "application/json").post(Entity.json(payload));
		        break;
		    case "PUT":
		        response = target.request().header("Content-type", "application/json").put(Entity.json(payload));
		        break;
		    case "DELETE":
		        response = target.request().header("Content-type", "application/json").delete();
		        break;
		    default:
		        throw new NotAllowedException("Invalid method type was given "
		                + "to the Utility.sendRequest() method");
		    }
		    
		    return response;
		}
		catch(Exception e){
		    e.printStackTrace();
		    
		    return Response.status(response.getStatus()).entity(e.getMessage()).build();
		}
	}
	
	
}
