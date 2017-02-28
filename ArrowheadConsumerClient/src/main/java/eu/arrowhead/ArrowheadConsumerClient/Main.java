package eu.arrowhead.ArrowheadConsumerClient;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.ws.rs.core.Response;

import eu.arrowhead.ArrowheadConsumerClient.model.ArrowheadService;
import eu.arrowhead.ArrowheadConsumerClient.model.ArrowheadSystem;
import eu.arrowhead.ArrowheadConsumerClient.model.ErrorMessage;
import eu.arrowhead.ArrowheadConsumerClient.model.OrchestrationResponse;
import eu.arrowhead.ArrowheadConsumerClient.model.ServiceRequestForm;

public class Main {
	
	private static Properties prop;
	//public static final String ORCH_URI = getProp().getProperty("orch_uri", "http://localhost:8444/orchestrator/orchestration");
	public static final String ORCH_URI = "http://arrowhead.tmit.bme.hu:8084/orchestrator/orchestration";
	
	public static void main(String[] args) throws Exception {
		//Payload compiling
		System.out.println("Orchestrator URL: " + ORCH_URI + "\n");
		ServiceRequestForm srf = compileSRF();
		
		//Sending request to the orchestrator, parsing the answer
		Response postResponse = Utility.sendRequest(ORCH_URI, "POST", srf);
		if(postResponse.getStatus() != 200){
			ErrorMessage error = postResponse.readEntity(ErrorMessage.class);
			throw new Exception(error.getErrorMessage());
		}
		OrchestrationResponse orchResponse = postResponse.readEntity(OrchestrationResponse.class);
		
		ArrowheadSystem provider = orchResponse.getResponse().get(0).getProvider();
		String serviceURI = orchResponse.getResponse().get(0).getServiceURI();
		String providerURI = "http://" + provider.getAddress() + ":" + provider.getPort() + serviceURI;
		System.out.println("Received provider system URL: " + providerURI + "\n");
		
		//Sending request to the provider, parsing the answer
		Response getResponse = Utility.sendRequest(providerURI, "GET", null);
		String temperature = getResponse.readEntity(String.class);
		System.out.println("The indoor temperature is " + temperature + " degrees celsius.");
	}
	
	public static ServiceRequestForm compileSRF() {
		ArrowheadSystem consumer = new ArrowheadSystem("testGroup", "testSystem", "localhost");
		
		List<String> interfaces = new ArrayList<String>();
        interfaces.add("json");
		ArrowheadService service = new ArrowheadService("Temperature", "IndoorTemperature", interfaces);
		
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
