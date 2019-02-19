package eu.arrowhead.digital_twin;

import de.codecentric.boot.admin.server.config.EnableAdminServer;
import eu.arrowhead.digital_twin.mock_provider.ProviderService;
import java.util.concurrent.CompletableFuture;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableAdminServer
@EnableScheduling
@EnableAutoConfiguration
@SpringBootApplication
public class DigitalTwinApplication {

  private final DigitalTwinService digitalTwinService;
  private final ProviderService providerService;

  @Autowired
  public DigitalTwinApplication(DigitalTwinService dtService, ProviderService providerService) {
    this.digitalTwinService = dtService;
    this.providerService = providerService;
  }

  public static void main(String[] args) {
    SpringApplication.run(DigitalTwinApplication.class, args);
  }

  @PostConstruct
  void onStart() {
    digitalTwinService.registerPurchaseService();
    digitalTwinService.subscribeToSmartProductEvents();
    digitalTwinService.loadSmartProductStatesFromFile();
    providerService.registerDemoServices();
  }

  @PreDestroy
  void onShutdown() {
    CompletableFuture<Void> eventHandler = CompletableFuture.runAsync(digitalTwinService::unsubscribeFromEvents);
    CompletableFuture<Void> serviceRegistry = CompletableFuture.runAsync(digitalTwinService::unregisterPurchaseService);
    CompletableFuture<Void> saveStateToFile = CompletableFuture.runAsync(digitalTwinService::saveSmartProductStatesToFile);
    CompletableFuture<Void> demoServices = CompletableFuture.runAsync(providerService::unregisterDemoServices);
    CompletableFuture.allOf(eventHandler, serviceRegistry, saveStateToFile, demoServices).join();
  }
}
