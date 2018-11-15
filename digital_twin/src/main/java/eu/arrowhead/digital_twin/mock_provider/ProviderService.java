package eu.arrowhead.digital_twin.mock_provider;

import eu.arrowhead.client.common.Utility;
import eu.arrowhead.client.common.exception.DuplicateEntryException;
import eu.arrowhead.client.common.model.ArrowheadService;
import eu.arrowhead.client.common.model.ArrowheadSystem;
import eu.arrowhead.client.common.model.ServiceRegistryEntry;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.ws.rs.core.UriBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ProviderService {

  private AtomicBoolean firstTry = new AtomicBoolean(true);

  private final String serviceRegistryUrl;
  private final ServiceRegistryEntry millingService;
  private final ServiceRegistryEntry assemblyService;
  private final ServiceRegistryEntry storeService;
  private final ServiceRegistryEntry weldingService;
  private final Logger log = LoggerFactory.getLogger(ProviderService.class);

  @Autowired
  public ProviderService(@Value("${service_registry_url}") String serviceRegistryUrl, @Value("${server.address}") String myHost,
                         @Value("${server.port}") int myPort) {
    this.serviceRegistryUrl = serviceRegistryUrl;
    ArrowheadSystem providerSystem = new ArrowheadSystem("GenericProvider", myHost, myPort, null);

    Map<String, String> millingMetadata = new HashMap<>();
    millingMetadata.put("tool", "GR-2MM");
    ArrowheadService milling = new ArrowheadService("milling", Collections.singleton("JSON"), millingMetadata);
    millingService = new ServiceRegistryEntry(milling, providerSystem, ProviderController.MILLING_SUBPATH);

    ArrowheadService assembly = new ArrowheadService("assembly", Collections.singleton("JSON"), null);
    assemblyService = new ServiceRegistryEntry(assembly, providerSystem, ProviderController.ASSEMBLY_SUBPATH);

    Map<String, String> storeMetadata = new HashMap<>();
    storeMetadata.put("location", "C-137");
    ArrowheadService store = new ArrowheadService("store", Collections.singleton("JSON"), storeMetadata);
    storeService = new ServiceRegistryEntry(store, providerSystem, ProviderController.STORE_SUBPATH);

    ArrowheadService welding = new ArrowheadService("welding", Collections.singleton("JSON"), null);
    weldingService = new ServiceRegistryEntry(welding, providerSystem, ProviderController.WELDING_SUBPATH);
  }

  public void registerDemoServices() {
    String registeringURL = UriBuilder.fromPath(serviceRegistryUrl).path("register").build().toString();
    CompletableFuture.runAsync(() -> sendAndLogRequest(registeringURL, "POST", millingService));
    CompletableFuture.runAsync(() -> sendAndLogRequest(registeringURL, "POST", assemblyService));
    CompletableFuture.runAsync(() -> sendAndLogRequest(registeringURL, "POST", storeService));
    CompletableFuture.runAsync(() -> sendAndLogRequest(registeringURL, "POST", weldingService));
  }

  public void unregisterDemoServices() {
    String removeURL = UriBuilder.fromPath(serviceRegistryUrl).path("remove").build().toString();
    CompletableFuture.runAsync(() -> sendAndLogRequest(removeURL, "PUT", millingService));
    CompletableFuture.runAsync(() -> sendAndLogRequest(removeURL, "PUT", assemblyService));
    CompletableFuture.runAsync(() -> sendAndLogRequest(removeURL, "PUT", storeService));
    CompletableFuture.runAsync(() -> sendAndLogRequest(removeURL, "PUT", weldingService));
  }

  private void sendAndLogRequest(String url, String method, ServiceRegistryEntry payload) {
    try {
      Utility.sendRequest(url, method, payload);
    } catch (DuplicateEntryException e) {
      unregisterDemoServices();
      if (firstTry.get()) {
        firstTry.compareAndSet(true, false);
        registerDemoServices();
      } else {
        throw e;
      }
    } catch (Exception e) {
      log.error(payload.getProvidedService().getServiceDefinition() + " (un)registration failed with exception", e);
      return;
    }
    log.info(payload.getProvidedService().getServiceDefinition() + " (un)registered");
  }
}
