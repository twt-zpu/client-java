package eu.arrowhead.digital_twin;

import eu.arrowhead.client.common.Utility;
import eu.arrowhead.client.common.exception.DuplicateEntryException;
import eu.arrowhead.client.common.model.ArrowheadService;
import eu.arrowhead.client.common.model.ArrowheadSystem;
import eu.arrowhead.client.common.model.Event;
import eu.arrowhead.client.common.model.EventFilter;
import eu.arrowhead.client.common.model.OrchestrationResponse;
import eu.arrowhead.client.common.model.ServiceRegistryEntry;
import eu.arrowhead.client.common.model.ServiceRequestForm;
import eu.arrowhead.digital_twin.model.SmartProduct;
import eu.arrowhead.digital_twin.model.SmartProductCSV;
import eu.arrowhead.digital_twin.model.SmartProductLifeCycle;
import eu.arrowhead.digital_twin.model.SmartProductPosition;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.supercsv.cellprocessor.constraint.NotNull;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.io.CsvBeanReader;
import org.supercsv.io.CsvBeanWriter;
import org.supercsv.io.ICsvBeanReader;
import org.supercsv.io.ICsvBeanWriter;
import org.supercsv.prefs.CsvPreference;

@Service
public class DigitalTwinService {

  private AtomicBoolean firstTry = new AtomicBoolean(true);

  private static final String CONSUMER_NAME = "IPS_DIGITAL_TWIN";
  private static final Map<SmartProductLifeCycle, SmartProductPosition> nextStepForProduct = new HashMap<>();
  //TODO is this thread safe? refactor it to use concurrenthashmap
  private final List<SmartProduct> smartProducts = new ArrayList<>();

  private final String serviceRegistryUrl;
  private final String eventHandlerUrl;
  private final String orchestratorUrl;
  private final String stateSaveLocation;
  private final Logger log = LoggerFactory.getLogger(DigitalTwinService.class);
  private final ArrowheadSystem digitalTwin;

  private enum EventsToListenFor {area_entered, area_left}

  @Autowired
  public DigitalTwinService(@Value("${service_registry_url}") String serviceRegistryUrl, @Value("${event_handler_url}") String eventHandlerUrl,
                            @Value("${orchestrator_url}") String orchestratorUrl, @Value("${internal_state_save_location}") String stateSaveLocation,
                            @Value("${server.address}") String myHost, @Value("${server.port}") int myPort) {
    this.serviceRegistryUrl = serviceRegistryUrl;
    //TODO make the digital twin acquire these URLs from the SR?
    this.eventHandlerUrl = eventHandlerUrl;
    this.orchestratorUrl = orchestratorUrl;
    if (!stateSaveLocation.endsWith(".csv")) {
      stateSaveLocation = stateSaveLocation.concat(".csv");
    }
    this.stateSaveLocation = stateSaveLocation;

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
    try {
      Utility.sendRequest(registerUrl, "POST", srEntry);
    } catch (DuplicateEntryException e) {
      unregisterPurchaseService();
      if (firstTry.get()) {
        firstTry.compareAndSet(true, false);
        registerPurchaseService();
      } else {
        throw e;
      }
    }
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
    List<SmartProductCSV> stringifiedProducts = new ArrayList<>();
    for (SmartProduct product : smartProducts) {
      SmartProductCSV productCSV = new SmartProductCSV(product.getRfidParts().toString(), product.getLifeCycle().name(),
                                                       product.getLastKnownPosition().name());
      stringifiedProducts.add(productCSV);
    }

    final CellProcessor[] cellProcessors = new CellProcessor[]{new NotNull(), new NotNull(), new NotNull()};
    try (ICsvBeanWriter beanWriter = new CsvBeanWriter(new FileWriter(stateSaveLocation), CsvPreference.STANDARD_PREFERENCE)) {

      // the header elements are used to map the bean values to each column (names must match)
      final String[] headers = new String[]{"rfidParts", "lifeCycle", "lastKnownPosition"};

      // write the header
      beanWriter.writeHeader(headers);

      // write the beans
      for (final SmartProductCSV smartProduct : stringifiedProducts) {
        beanWriter.write(smartProduct, headers, cellProcessors);
      }
    } catch (IOException e) {
      log.error("IOException during state saving.", e);
    }
  }

  void loadSmartProductStatesFromFile() {
    if (Files.isReadable(Paths.get(stateSaveLocation))) {
      final CellProcessor[] cellProcessors = new CellProcessor[]{new NotNull(), new NotNull(), new NotNull()};
      try (ICsvBeanReader beanReader = new CsvBeanReader(new FileReader(stateSaveLocation), CsvPreference.STANDARD_PREFERENCE)) {

        final String[] header = beanReader.getHeader(true);

        SmartProductCSV smartProductCSV;
        while ((smartProductCSV = beanReader.read(SmartProductCSV.class, header, cellProcessors)) != null) {
          String rfidParts = smartProductCSV.getRfidParts();
          List<String> rfidList = new ArrayList<>(Arrays.asList(rfidParts.substring(1, rfidParts.length() - 1).split(", ")));
          SmartProduct smartProduct = new SmartProduct(rfidList, SmartProductLifeCycle.valueOf(smartProductCSV.getLifeCycle()),
                                                       SmartProductPosition.valueOf(smartProductCSV.getLastKnownPosition()));
          smartProducts.add(smartProduct);
        }
      } catch (IOException e) {
        log.error("IOException during reading in the state from file.", e);
      }
    }
  }

  synchronized void handleArrowheadEvent(Event event) {
    System.out.println(smartProducts.toString());
    String rfidData = event.getEventMetadata().get("rfid");
    if (rfidData != null) {
      //NOTE hardcoded business logic specific to the demo
      String[] rfidTags = rfidData.split("_");
      String smartProductId = null;

      //Check if this a new product, or it existed before (RFID orders are not guaranteed)
      SmartProduct productWithStateChange = null;
      for (SmartProduct smartProduct : smartProducts) {
        if (Utility.hasCommonElement(smartProduct.getRfidParts(), Arrays.asList(rfidTags))) {
          productWithStateChange = smartProduct;
          smartProductId = smartProduct.getRfidParts().get(0);
        }
      }
      if (productWithStateChange == null) {
        productWithStateChange = new SmartProduct(Arrays.asList(rfidTags));
        smartProductId = rfidTags[0];
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
          //TODO check what happens between the 2 breakpoints on the 3rd request!
          String serviceDefinition = event.getEventMetadata().get("extra");
          ArrowheadService nextServiceToConsume = new ArrowheadService(serviceDefinition, Collections.singleton("JSON"), null);
          Map<String, Boolean> orchestrationFlags = new HashMap<>();
          orchestrationFlags.put("enableInterCloud", true);
          orchestrationFlags.put("overrideStore", true);
          ServiceRequestForm srf = new ServiceRequestForm.Builder(digitalTwin).requestedService(nextServiceToConsume)
                                                                              .orchestrationFlags(orchestrationFlags).build();

          Optional<String> providerURL = requestOrchestration(srf, smartProductId);
          providerURL.ifPresent(url -> consumeArrowheadService(serviceDefinition, url));

          //Update RFID and lifecycle information on the product
          List<String> newRfidTags = Utility.difference(productWithStateChange.getRfidParts(), Arrays.asList(rfidTags));
          //TODO weird thing happens here during 3rd request
          productWithStateChange.getRfidParts().addAll(newRfidTags);
          System.out.println("saaaajt");
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
        smartProducts.set(smartProductIndex, productWithStateChange);
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
      for (String rfid : product.getRfidParts()) {
        if (rfidKey.equals(rfid)) {
          return Optional.of(product);
        }
      }
    }
    return Optional.empty();
  }

  private Optional<String> requestOrchestration(ServiceRequestForm srf, String smartProductId) {
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
      //NOTE hardcoded business logic specific to the demo
      if (srf.getRequestedService().getServiceDefinition().equals("PurchaseSmartProduct")) {
        ub.path(smartProductId);
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
    //NOTE hardcoded business logic specific to the demo
    if (serviceDef.equals("PurchaseSmartProduct")) {
      SmartProduct purchasedSmartProduct = response.readEntity(SmartProduct.class);
      //NOTE this assumes that RFID tags are unique even between local clouds
      boolean exists = false;
      for (SmartProduct smartProduct : smartProducts) {
        if (Utility.hasCommonElement(smartProduct.getRfidParts(), purchasedSmartProduct.getRfidParts())) {
          exists = true;
        }
      }
      if (exists) {
        log.error("Purchased a smart product with an already existing RFID tag: " + purchasedSmartProduct.toString());
      } else {
        smartProducts.add(purchasedSmartProduct);
        log.info("Following smart product purchased: " + purchasedSmartProduct.toString());
      }
    } else {
      String providerResponse = response.readEntity(String.class);
      log.info(serviceDef + " service by " + url + " returned with: " + providerResponse);
    }
  }
}
