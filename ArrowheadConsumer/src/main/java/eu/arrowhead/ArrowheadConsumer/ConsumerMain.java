package eu.arrowhead.ArrowheadConsumer;

import eu.arrowhead.ArrowheadConsumer.model.ArrowheadService;
import eu.arrowhead.ArrowheadConsumer.model.ArrowheadSystem;
import eu.arrowhead.ArrowheadConsumer.model.OrchestrationResponse;
import eu.arrowhead.ArrowheadConsumer.model.ServiceMetadata;
import eu.arrowhead.ArrowheadConsumer.model.ServiceRequestForm;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

public class ConsumerMain {

  private static final String ORCH_URI = Utility.getProp().getProperty("orch_uri", "http://0.0.0.0:8440/orchestrator//orchestration");
  private static final boolean IS_SECURE = ORCH_URI.startsWith("https");

  public static void main(String[] args) {
    //Payload compiling
    ServiceRequestForm srf = compileSRF();

    //Sending request to the orchestrator, parsing the response
    Response postResponse = Utility.sendRequest(ORCH_URI, "POST", srf);
    OrchestrationResponse orchResponse = postResponse.readEntity(OrchestrationResponse.class);
    ArrowheadSystem provider = orchResponse.getResponse().get(0).getProvider();
    String serviceURI = orchResponse.getResponse().get(0).getServiceURI();
    UriBuilder ub = UriBuilder.fromPath("").host(provider.getAddress()).path(serviceURI).scheme
        ("http");
    if (provider.getPort() > 0) {
      ub.port(provider.getPort());
    }
    if (IS_SECURE) {
      ub.scheme("https");
      ub.queryParam("token", orchResponse.getResponse().get(0).getAuthorizationToken());
      ub.queryParam("signature", orchResponse.getResponse().get(0).getSignature());
    }

    System.out.println("Received provider system URL: " + ub.toString() + "\n");

    //Sending request to the provider, parsing the answer
    Response getResponse = Utility.sendRequest(ub.toString(), "GET", null);
    String temperature = getResponse.readEntity(String.class);
    System.out.println("The indoor temperature is " + temperature + " degrees celsius.");
  }

  private static ServiceRequestForm compileSRF() {
    ArrowheadSystem consumer = new ArrowheadSystem("group1", "client2", "localhost", 0, "null");

    List<String> interfaces = new ArrayList<>();
    interfaces.add("json");
    List<ServiceMetadata> serviceMetadata = new ArrayList<>();
    serviceMetadata.add(new ServiceMetadata("unit", "celsius"));
    if (IS_SECURE) {
      serviceMetadata.add(new ServiceMetadata("security", "token"));
    }
    ArrowheadService service = new ArrowheadService("Temperature", "IndoorTemperature", interfaces, serviceMetadata);

    Map<String, Boolean> orchestrationFlags = new HashMap<>();
    orchestrationFlags.put("overrideStore", true);
    orchestrationFlags.put("matchmaking", true);

    return new ServiceRequestForm.Builder(consumer).requestedService(service).orchestrationFlags(orchestrationFlags).build();
  }
}
