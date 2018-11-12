package eu.arrowhead.digital_twin;

import eu.arrowhead.client.common.Utility;
import eu.arrowhead.client.common.model.ArrowheadService;
import eu.arrowhead.client.common.model.ArrowheadSystem;
import eu.arrowhead.client.common.model.Event;
import eu.arrowhead.client.common.model.EventFilter;
import eu.arrowhead.client.common.model.OrchestrationResponse;
import eu.arrowhead.client.common.model.ServiceRegistryEntry;
import eu.arrowhead.client.common.model.ServiceRequestForm;
import eu.arrowhead.digital_twin.model.SmartProduct;
import eu.arrowhead.digital_twin.model.SmartProductLifeCycle;
import eu.arrowhead.digital_twin.model.SmartProductPosition;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class DigitalTwinService {

  private static final String CONSUMER_NAME = "IPS_DIGITAL_TWIN";
  private static final Map<SmartProductLifeCycle, SmartProductPosition> nextStepForProduct = new HashMap<>();
  private static final List<SmartProduct> smartProducts = new ArrayList<>();

  private final String serviceRegistryUrl;
  private final String eventHandlerUrl;
  private final String orchestratorUrl;
  private final Logger log = LoggerFactory.getLogger(DigitalTwinService.class);
  private final ArrowheadSystem digitalTwin;

  private enum EventsToListenFor {area_entered, area_left}

  @Autowired
  public DigitalTwinService(@Value("${service_registry_url}") String serviceRegistryUrl, @Value("${event_handler_url}") String eventHandlerUrl,
                            @Value("${orchestrator_url}") String orchestratorUrl, @Value("${server.address}") String myHost,
                            @Value("${server.port}") int myPort) {
    this.serviceRegistryUrl = serviceRegistryUrl;
    //TODO make the digital twin acquire these URLs from the SR?
    this.eventHandlerUrl = eventHandlerUrl;
    this.orchestratorUrl = orchestratorUrl;

    digitalTwin = new ArrowheadSystem(CONSUMER_NAME, myHost, myPort, null);

    nextStepForProduct.put(SmartProductLifeCycle.CREATED, SmartProductPosition.MILLING_MACHINE);
    nextStepForProduct.put(SmartProductLifeCycle.MILLED, SmartProductPosition.ASSEMBLY_STATION);
    nextStepForProduct.put(SmartProductLifeCycle.ASSEMBLED, SmartProductPosition.WAREHOUSE_1);
    nextStepForProduct.put(SmartProductLifeCycle.STORED, SmartProductPosition.WAREHOUSE_2);
    nextStepForProduct.put(SmartProductLifeCycle.PURCHASED, SmartProductPosition.WELDING_MACHINE);
  }

  void registerPurchaseService() {
    ArrowheadService purchaseProduct = new ArrowheadService("PurchaseSmartProduct", Collections.singleton("JSON"), null);
    ServiceRegistryEntry srEntry = new ServiceRegistryEntry(purchaseProduct, digitalTwin, "purchase");
    String registerUrl = UriBuilder.fromPath(serviceRegistryUrl).path("register").toString();
    Utility.sendRequest(registerUrl, "POST", srEntry);
    log.info("PuchaseSmartProduct service registered with the Service Registry");
  }

  void unregisterPurchaseService() {
    ArrowheadService purchaseProduct = new ArrowheadService("PurchaseSmartProduct", Collections.singleton("JSON"), null);
    ServiceRegistryEntry srEntry = new ServiceRegistryEntry(purchaseProduct, digitalTwin, "purchase");
    String unregisterUrl = UriBuilder.fromPath(serviceRegistryUrl).path("remove").toString();
    try {
      Utility.sendRequest(unregisterUrl, "PUT", srEntry);
    } catch (Exception e) {
      log.error("Removing PurchaseSmartProduct service from Service Registry failed", e);
      return;
    }
    log.info("PuchaseSmartProduct service removed from the Service Registry");
  }

  void subscribeToSmartProductEvents() {
    //Create the EventFilter request payload, send the subscribe request to the Event Handler
    for (EventsToListenFor eventType : EventsToListenFor.values()) {
      EventFilter filter = new EventFilter(eventType.toString(), digitalTwin, DigitalTwinController.RECEIVE_EVENT_SUBPATH);
      Utility.sendRequest(eventHandlerUrl, "POST", filter);
      log.info("Subscribed to " + eventType.name() + " events");
    }
  }

  void unsubscribeFromEvents() {
    for (EventsToListenFor eventType : EventsToListenFor.values()) {
      String url = UriBuilder.fromPath(eventHandlerUrl).path("type").path(eventType.name()).path("consumer").path(CONSUMER_NAME).toString();
      try {
        Utility.sendRequest(url, "DELETE", null);
      } catch (Exception e) {
        log.error("Unsubscribing from " + eventType.name() + " events failed", e);
        return;
      }
      log.info("Unsubscribed from " + eventType.name() + " events");
    }
  }

  @Scheduled(fixedRate = 1000 * 600)
    //run this method every 10 minutes
  void saveSmartProductStatesToFile() {
    //TODO
  }

  void loadSmartProductStatesFromFile() {
    //TODO
  }

  void handleArrowheadEvent(Event event) {
    String rfidData = event.getEventMetadata().get("rfid");
    if (rfidData != null) {
      String[] rfidTags = rfidData.split("_");
      String smartProductId = rfidTags[0];

      //Check if this a new product, or it existed before
      SmartProduct productWithStateChange = null;
      for (SmartProduct smartProduct : smartProducts) {
        if (smartProductId.equals(smartProduct.getRfidParts().get(0))) {
          productWithStateChange = smartProduct;
        }
      }
      if (productWithStateChange == null) {
        productWithStateChange = new SmartProduct(Arrays.asList(rfidTags));
        smartProducts.add(productWithStateChange);
        log.info("Smart product with id " + smartProductId + " created");
      }
      int smartProductIndex = smartProducts.indexOf(productWithStateChange);

      if (EventsToListenFor.area_entered.name().equals(event.getType())) {
        log.info("Smart product " + smartProductId + " entered into area: " + event.getPayload());

        //Check if the product reached the next production step in the line
        //1) If the product is already in a finished state, then there is nothing more to do
        if (SmartProductLifeCycle.FINISHED.equals(productWithStateChange.getLifeCycle())) {
          return;
        }
        //2) If the product is not finished, check if its reported position matches the expected position for the next production step
        SmartProductPosition nextProductionStep = nextStepForProduct.get(productWithStateChange.getLifeCycle());
        //3) If it matches, ask for the service through the Orchestrator Core System
        if (nextProductionStep.equals(SmartProductPosition.valueOf(event.getPayload().toUpperCase()))) {
          String serviceDefinition = event.getEventMetadata().get("extra");
          ArrowheadService nextServiceToConsume = new ArrowheadService(serviceDefinition, Collections.singleton("JSON"), null);
          Map<String, Boolean> orchestrationFlags = new HashMap<>();
          orchestrationFlags.put("enableInterCloud", true);
          orchestrationFlags.put("overrideStore", true);
          ServiceRequestForm srf = new ServiceRequestForm.Builder(digitalTwin).requestedService(nextServiceToConsume)
                                                                              .orchestrationFlags(orchestrationFlags).build();

          Optional<String> providerURL = requestOrchestration(srf);
          providerURL.ifPresent(url -> consumeArrowheadService(serviceDefinition, url));

          //Update RFID and lifecycle information on the product
          productWithStateChange.setRfidParts(Arrays.asList(rfidTags));
          productWithStateChange.setLifeCycle(productWithStateChange.getLifeCycle().next());
          log.debug("Production lifecycle for product " + smartProductId + " updated to " + productWithStateChange.getLifeCycle());

        } else {
          log.debug("The next production step for product " + smartProductId + " is at " + nextProductionStep + ", but it entered into " + event
              .getPayload().toUpperCase());
        }

        productWithStateChange.setLastKnownPosition(SmartProductPosition.valueOf(event.getPayload().toUpperCase()));
        smartProducts.set(smartProductIndex, productWithStateChange);

      } else if (EventsToListenFor.area_left.name().equals(event.getType())) {
        productWithStateChange.setLastKnownPosition(SmartProductPosition.OUTSIDE_OF_GEOFENCED_AREA);
        log.info("Smart product " + smartProductId + " left the " + event.getPayload() + " area");
      } else {
        log.error("Received unknown event type from Event Handler. Type: " + event.getType());
      }
    } else {
      log.error("Received event with no RFID information. The event will not be further processed.");
    }
  }

  Optional<SmartProduct> findSmartProductByFirstRFID(String rfidKey) {
    for (SmartProduct product : smartProducts) {
      if (rfidKey.equals(product.getRfidParts().get(0))) {
        return Optional.of(product);
      }
    }
    return Optional.empty();
  }

  private Optional<String> requestOrchestration(ServiceRequestForm srf) {
    try {
      Response response = Utility.sendRequest(orchestratorUrl, "POST", srf);
      OrchestrationResponse orchResponse = response.readEntity(OrchestrationResponse.class);

      //Getting the first provider from the response
      ArrowheadSystem provider = orchResponse.getResponse().get(0).getProvider();
      String serviceURI = orchResponse.getResponse().get(0).getServiceURI();
      //Compiling the URL for the provider
      UriBuilder ub = UriBuilder.fromPath("").host(provider.getAddress()).scheme("http");
      if (serviceURI != null) {
        ub.path(serviceURI);
      }
      if (provider.getPort() != null && provider.getPort() > 0) {
        ub.port(provider.getPort());
      }
      if (orchResponse.getResponse().get(0).getService().getServiceMetadata().containsKey("security")) {
        ub.scheme("https");
        ub.queryParam("token", orchResponse.getResponse().get(0).getAuthorizationToken());
        ub.queryParam("signature", orchResponse.getResponse().get(0).getSignature());
      }
      log.debug("Successful orchestration process, received provider system URL: " + ub.toString());
      return Optional.of(ub.toString());
    } catch (Exception e) {
      log.error("Orchestration process failed", e);
      return Optional.empty();
    }
  }

  private void consumeArrowheadService(String serviceDef, String url) {
    Response response = Utility.sendRequest(url, "GET", null);
    String providerResponse = response.readEntity(String.class);
    log.info(serviceDef + " service by " + url + " returned with: " + providerResponse);
  }

}
