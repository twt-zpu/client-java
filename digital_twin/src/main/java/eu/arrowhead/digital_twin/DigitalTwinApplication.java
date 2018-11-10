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

  //TODO check if there is a file with state information to use
  @PostConstruct
  void onStart() {
    digitalTwinService.registerPurchaseService();
    digitalTwinService.subscribeToSmartProductEvents();
  }

  //TODO in case of shutdown save the internal state to file + also do it periodically like every minute
  @PreDestroy
  void onShutdown() {
    CompletableFuture<Void> eventHandler = CompletableFuture.runAsync(digitalTwinService::unsubscribeFromEvents);
    CompletableFuture<Void> serviceRegistry = CompletableFuture.runAsync(digitalTwinService::unregisterPurchaseService);
    CompletableFuture.allOf(eventHandler, serviceRegistry).join();
  }
}
