package eu.arrowhead.digital_twin;

import java.util.concurrent.CompletableFuture;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class DigitalTwinApplication {

  private final DigitalTwinService digitalTwinService;

  @Autowired
  public DigitalTwinApplication(DigitalTwinService dtService) {
    this.digitalTwinService = dtService;
  }

  public static void main(String[] args) {
    SpringApplication.run(DigitalTwinApplication.class, args);
  }

  @PostConstruct
  void onStart() {
    digitalTwinService.registerPurchaseService();
    digitalTwinService.subscribeToSmartProductEvents();
    digitalTwinService.loadSmartProductStatesFromFile();
  }

  @PreDestroy
  void onShutdown() {
    CompletableFuture<Void> eventHandler = CompletableFuture.runAsync(digitalTwinService::unsubscribeFromEvents);
    CompletableFuture<Void> serviceRegistry = CompletableFuture.runAsync(digitalTwinService::unregisterPurchaseService);
    CompletableFuture<Void> saveStateToFile = CompletableFuture.runAsync(digitalTwinService::saveSmartProductStatesToFile);
    CompletableFuture.allOf(eventHandler, serviceRegistry, saveStateToFile).join();
  }
}
