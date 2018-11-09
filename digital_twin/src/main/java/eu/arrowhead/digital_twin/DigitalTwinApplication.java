package eu.arrowhead.digital_twin;

import java.util.concurrent.CompletableFuture;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class DigitalTwinApplication {

  private final DigitalTwinService digitalTwinService;

  @Autowired
  public DigitalTwinApplication(DigitalTwinService dtService) {
    this.digitalTwinService = dtService;
  }

  public static void main(String[] args) {
    ConfigurableApplicationContext ctx = SpringApplication.run(DigitalTwinApplication.class, args);
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      System.out.println("Received TERM signal, shutting down...");
      ctx.close();
    }));
  }

  @PostConstruct
  void onStart() {
    digitalTwinService.registerPurchaseService();
    digitalTwinService.subscribeToSmartProductEvents();
  }

  //TODO deregister from SR (should be done in parallel with the unsubscribe)
  @PreDestroy
  void onShutdown() {
    CompletableFuture<Void> eventHandler = CompletableFuture.runAsync(digitalTwinService::unsubscribeFromEvents);
    CompletableFuture<Void> serviceRegistry = CompletableFuture.runAsync(digitalTwinService::unregisterPurchaseService);
    //TODO collect the futures and join them

  }
}
