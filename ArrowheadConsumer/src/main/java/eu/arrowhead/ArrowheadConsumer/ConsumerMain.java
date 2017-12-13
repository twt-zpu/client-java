package eu.arrowhead.ArrowheadConsumer;

import eu.arrowhead.ArrowheadConsumer.model.ArrowheadService;
import eu.arrowhead.ArrowheadConsumer.model.ArrowheadSystem;
import eu.arrowhead.ArrowheadConsumer.model.OrchestrationResponse;
import eu.arrowhead.ArrowheadConsumer.model.ServiceRequestForm;
import eu.arrowhead.ArrowheadConsumer.model.TemperatureReadout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

public class ConsumerMain {

  private static final String ORCH_URI = Utility.getProp().getProperty("orch_uri", "http://0.0.0.0:8440/orchestrator//orchestration");
  private static boolean isSecure = false;
  public static void main(String[] args) {

    argLoop:
    for (int i = 0; i < args.length; ++i) {
      if (args[i].equals("-m")) {
        ++i;
        switch (args[i]) {
          case "insecure":
            isSecure = false;
            break argLoop;
          case "secure":
            isSecure = true;
            break argLoop;
          default:
            throw new AssertionError("Unknown security level: " + args[i]);
        }
      }
    }

    System.out.println("Working directory: " + System.getProperty("user.dir"));
    long startTime = System.currentTimeMillis();

    //Payload compiling
    ServiceRequestForm srf = compileSRF();
    System.out.println("Service Request payload: " + Utility.toPrettyJson(null, srf));

    //Sending request to the orche rator, parsing the response
    Response postResponse = Utility.sendRequest(ORCH_URI, "POST", srf);
    OrchestrationResponse orchResponse = postResponse.readEntity(OrchestrationResponse.class);
    System.out.println("Orchestration Response payload: " + Utility.toPrettyJson(null, orchResponse));

    ArrowheadSystem provider = orchResponse.getResponse().get(0).getProvider();
    String serviceURI = orchResponse.getResponse().get(0).getServiceURI();
    UriBuilder ub = UriBuilder.fromPath("").host(provider.getAddress()).path(serviceURI).scheme("http");
    if (provider.getPort() > 0) {
      ub.port(provider.getPort());
    }
    if (orchResponse.getResponse().get(0).getService().getServiceMetadata().containsKey("security")) {
      ub.scheme("https");
      ub.queryParam("token", orchResponse.getResponse().get(0).getAuthorizationToken());
      ub.queryParam("signature", orchResponse.getResponse().get(0).getSignature());
    } else {
      ub.scheme("http");
    }

    System.out.println("Received provider system URL: " + ub.toString() + "\n");

    //Sending request to the provider, parsing the answer
    Response getResponse = Utility.sendRequest(ub.toString(), "GET", null);
    TemperatureReadout readout = new TemperatureReadout();
    try {
      readout = getResponse.readEntity(TemperatureReadout.class);
    } catch (RuntimeException e) {
      e.printStackTrace();
      System.out.println("Provider did not send the temperature readout in SenML format.");
    }
    if(readout.getE().get(0) == null){
      System.out.println("Provider did not send any MeasurementEntry.");
    }
    else{
      long endTime = System.currentTimeMillis();
      System.out.println("The indoor temperature is " + readout.getE().get(0).getV() + " degrees celsius.");
      System.out.println("Orchestration and Service consumption response time:" + Long.toString(endTime-startTime));
    }
  }

  private static ServiceRequestForm compileSRF() {
    ArrowheadSystem consumer = new ArrowheadSystem("group1", "client2", "localhost", 0, "null");

    List<String> interfaces = new ArrayList<>();
    interfaces.add("json");
    Map<String, String> serviceMetadata = new HashMap<>();
    serviceMetadata.put("unit", "celsius");
    if (isSecure) {
      serviceMetadata.put("security", "token");
    }
    ArrowheadService service = new ArrowheadService("Temperature", "IndoorTemperature", interfaces, serviceMetadata);

    Map<String, Boolean> orchestrationFlags = new HashMap<>();
    orchestrationFlags.put("overrideStore", true);
    orchestrationFlags.put("pingProviders", false);
    orchestrationFlags.put("metadataSearch", false);
    orchestrationFlags.put("enableInterCloud", true);

    return new ServiceRequestForm.Builder(consumer).requestedService(service).orchestrationFlags(orchestrationFlags).build();
  }
}
