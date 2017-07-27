package eu.arrowhead.ArrowheadConsumer;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import com.google.gson.Gson;

import eu.arrowhead.ArrowheadConsumer.model.ArrowheadService;
import eu.arrowhead.ArrowheadConsumer.model.ArrowheadSystem;
import eu.arrowhead.ArrowheadConsumer.model.ErrorMessage;
import eu.arrowhead.ArrowheadConsumer.model.OrchestrationResponse;
import eu.arrowhead.ArrowheadConsumer.model.ServiceMetadata;
import eu.arrowhead.ArrowheadConsumer.model.ServiceRequestForm;

public class ConsumerMain {
	//TODO need to modify to receive token+signature with orchResp
	
	
	private static Properties prop;
	//public static final String ORCH_URI = getProp().getProperty("orch_uri", "http://localhost:8444/orchestrator/orchestration");
	public static final String ORCH_URI = "https://127.0.0.1:8445/orchestrator/orchestration";
	
	public static void main(String[] args) throws Exception {
		//Payload compiling
		System.out.println("Orchestrator URL: " + ORCH_URI + "\n");
		ServiceRequestForm srf = compileSRF();
		
		//Sending request to the orchestrator, parsing the answer
		Response postResponse = Utility.sendRequest(ORCH_URI, "POST", srf);
		Gson gson = new Gson();
		String payload = gson.toJson(srf);
		System.out.println(payload);
		System.out.println(postResponse.getStatus());
		if(postResponse.getStatus() != 200){
			ErrorMessage error = postResponse.readEntity(ErrorMessage.class);
			throw new Exception(error.getErrorMessage());
		}
		OrchestrationResponse orchResponse = postResponse.readEntity(OrchestrationResponse.class);
		
		ArrowheadSystem provider = orchResponse.getResponse().get(0).getProvider();
		String token = orchResponse.getResponse().get(0).getAuthorizationToken();
		String signature = orchResponse.getResponse().get(0).getSignature();
		String serviceURI = orchResponse.getResponse().get(0).getServiceURI();
		
		UriBuilder providerUriBuilder = UriBuilder
	            .fromPath("//" + provider.getAddress() + ":" + provider.getPort())
	            .scheme("https")
	            .path(serviceURI)
	            .queryParam("token", token)
	            .queryParam("signature", signature);
        //.port(Integer.parseInt(provider.getPort()))
		
		System.out.println("Received provider system URL: " + providerUriBuilder.toString() + "\n");
		
		//Sending request to the provider, parsing the answer
		Response getResponse = Utility.sendRequest(providerUriBuilder.toString(), "GET", null);
		
		if (getResponse.getStatus() != 200) {
			ErrorMessage error = getResponse.readEntity(ErrorMessage.class);
			throw new Exception(error.getErrorMessage());
		} else {
			String temperature = getResponse.readEntity(String.class);
			System.out.println("The indoor temperature is " + temperature + " degrees celsius.");
		}
	}
	
	public static ServiceRequestForm compileSRF() {
		ArrowheadSystem consumer = new ArrowheadSystem("group1", "client2", "localhost");
		
		List<String> interfaces = new ArrayList<String>();
        interfaces.add("json");
        List<ServiceMetadata> serviceMetadata = new ArrayList<ServiceMetadata>();
        serviceMetadata.add(new ServiceMetadata("security", "token"));
        serviceMetadata.add(new ServiceMetadata("unit", "celsius"));
		ArrowheadService service = new ArrowheadService("Temperature", "IndoorTemperature", interfaces, serviceMetadata);
		
		Map<String, Boolean> orchestrationFlags = new HashMap<>();
		orchestrationFlags.put("overrideStore", true);
		orchestrationFlags.put("matchmaking", true);
		
		ServiceRequestForm srf = new ServiceRequestForm(consumer, service, orchestrationFlags);
        return srf;
	}

	public synchronized static Properties getProp() {
		try {
			if (prop == null) {
				prop = new Properties();
				File file = new File("config" + File.separator + "app.properties");
				FileInputStream inputStream = new FileInputStream(file);
				if (inputStream != null) {
					prop.load(inputStream);
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		
		return prop;
	}
}
