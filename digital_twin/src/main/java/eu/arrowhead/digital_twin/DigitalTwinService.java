package eu.arrowhead.digital_twin;

import eu.arrowhead.client.common.Utility;
import eu.arrowhead.client.common.model.ArrowheadService;
import eu.arrowhead.client.common.model.ArrowheadSystem;
import eu.arrowhead.client.common.model.Event;
import eu.arrowhead.client.common.model.EventFilter;
import eu.arrowhead.client.common.model.ServiceRegistryEntry;
import java.util.Collections;
import javax.ws.rs.core.UriBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class DigitalTwinService {

  private static final String CONSUMER_NAME = "IPS_DIGITAL_TWIN";

  private final String eventHandlerUrl;
  private final String serviceRegistryUrl;
  private final Logger log = LoggerFactory.getLogger(DigitalTwinService.class);
  private final ArrowheadSystem digitalTwin;

  private enum EventsToListenFor {area_entered, area_left}

  @Autowired
  public DigitalTwinService(@Value("${event_handler_url}") String eventHandlerUrl, @Value("${service_registry_url}") String serviceRegistryUrl,
                            @Value("${server.address}") String myHost, @Value("${server.port}") int myPort) {
    this.eventHandlerUrl = eventHandlerUrl;
    this.serviceRegistryUrl = serviceRegistryUrl;

    digitalTwin = new ArrowheadSystem(CONSUMER_NAME, myHost, myPort, null);
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
    Utility.sendRequest(unregisterUrl, "PUT", srEntry);
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

  //TODO handle exception at unregistering sendRequest methods
  void unsubscribeFromEvents() {
    for (EventsToListenFor eventType : EventsToListenFor.values()) {
      String url = UriBuilder.fromPath(eventHandlerUrl).path("type").path(eventType.name()).path("consumer").path(CONSUMER_NAME).toString();
      Utility.sendRequest(url, "DELETE", null);
      log.info("Unsubscribed from " + eventType.name() + " events.");
    }
  }

  //TODO RFID metadata logolása és számon tartása az RFID adatnak memóriában
  void handleArrowheadEvent(Event event) {
    if (event.getType().equals(EventsToListenFor.area_entered.name())) {
      //log the entered area

      //Event extra metadata will be the service def needed

      //Compile the SRF from the event and service global variables

      //send Orch request

      //In a new method consume and log the service

    } else if (event.getType().equals(EventsToListenFor.area_left.name())) {
      log.info("The smart product left the " + event.getPayload() + " area");
    } else {
      log.info("Received unknown event type from Event Handler. Type: " + event.getType());
    }
  }

}
