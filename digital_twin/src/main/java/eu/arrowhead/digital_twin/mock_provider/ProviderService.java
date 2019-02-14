package eu.arrowhead.digital_twin.mock_provider;

import eu.arrowhead.client.common.Utility;
import eu.arrowhead.client.common.model.ArrowheadService;
import eu.arrowhead.client.common.model.ArrowheadSystem;
import eu.arrowhead.client.common.model.ServiceRegistryEntry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import javax.ws.rs.core.UriBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ProviderService {

  private final String serviceRegistryUrl;
  private final List<ServiceRegistryEntry> providedServices = new ArrayList<>();
  private final Logger log = LoggerFactory.getLogger(ProviderService.class);

  @Autowired
  public ProviderService(@Value("${service_registry_url}") String serviceRegistryUrl,
                         @Value("${server.address}") String myHost, @Value("${server.port}") int myPort) {
    this.serviceRegistryUrl = serviceRegistryUrl;
    ArrowheadSystem providerSystem = new ArrowheadSystem("GenericProvider", myHost, myPort, null);

    Map<String, String> millingMetadata = new HashMap<>();
    millingMetadata.put("tool", "GR-2MM");
    ArrowheadService milling = new ArrowheadService("milling", Collections.singleton("JSON"), millingMetadata);
    providedServices.add(new ServiceRegistryEntry(milling, providerSystem, ProviderController.MILLING_SUBPATH));

    ArrowheadService assembly = new ArrowheadService("assembly", Collections.singleton("JSON"), null);
    providedServices.add(new ServiceRegistryEntry(assembly, providerSystem, ProviderController.ASSEMBLY_SUBPATH));

    Map<String, String> storeMetadata = new HashMap<>();
    storeMetadata.put("location", "C-137");
    ArrowheadService store = new ArrowheadService("store", Collections.singleton("JSON"), storeMetadata);
    providedServices.add(new ServiceRegistryEntry(store, providerSystem, ProviderController.STORE_SUBPATH));

    ArrowheadService welding = new ArrowheadService("welding", Collections.singleton("JSON"), null);
    providedServices.add(new ServiceRegistryEntry(welding, providerSystem, ProviderController.WELDING_SUBPATH));
  }

  public void registerDemoServices() {
    //Just to be safe, in case the app was not shutdown gracefully
    unregisterDemoServices();
    String registeringURL = UriBuilder.fromPath(serviceRegistryUrl).path("register").build().toString();
    for (ServiceRegistryEntry entry : providedServices) {
      CompletableFuture.runAsync(() -> sendAndLogRequest(registeringURL, "POST", entry));
    }
  }

  public void unregisterDemoServices() {
    String removeURL = UriBuilder.fromPath(serviceRegistryUrl).path("remove").build().toString();
    Stream<CompletableFuture> futures = providedServices.stream().map(
        entry -> CompletableFuture.runAsync(() -> sendAndLogRequest(removeURL, "PUT", entry)));
    //join blocks and waits for all the futures to finish
    CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
  }

  private void sendAndLogRequest(String url, String method, ServiceRegistryEntry payload) {
    String logText = method.equals("POST") ? " registration" : " unregistration";
    try {
      Utility.sendRequest(url, method, payload);
    } catch (Exception e) {
      log.error(payload.getProvidedService().getServiceDefinition() + logText + " failed with exception", e);
      return;
    }
    log.info(payload.getProvidedService().getServiceDefinition() + logText);
  }
}
