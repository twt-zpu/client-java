package eu.arrowhead.digital_twin;

import eu.arrowhead.client.common.Utility;
import eu.arrowhead.client.common.model.ArrowheadSystem;
import eu.arrowhead.client.common.model.EventFilter;
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
  private final String myHost;
  private final int myPort;
  private final Logger log = LoggerFactory.getLogger(DigitalTwinService.class);

  private enum EventsToListenFor {area_entered, area_left}

  @Autowired
  public DigitalTwinService(@Value("${event_handler_url}") String eventHandlerUrl, @Value("${server.address}") String myHost,
                            @Value("${server.port}") int myPort) {
    this.eventHandlerUrl = eventHandlerUrl;
    this.myHost = myHost;
    this.myPort = myPort;
  }

  void subscribeToSmartProductEvents() {
    //Create the EventFilter request payload, send the subscribe request to the Event Handler
    ArrowheadSystem consumer = new ArrowheadSystem(CONSUMER_NAME, myHost, myPort, null);
    for (EventsToListenFor eventType : EventsToListenFor.values()) {
      EventFilter filter = new EventFilter(eventType.toString(), consumer, DigitalTwinController.RECEIVE_EVENT_SUBPATH);
      Utility.sendRequest(eventHandlerUrl, "POST", filter);
      log.info("Subscribed to " + eventType.name() + " events");
    }
  }

  void unsubscribeFromEvents() {
    for (EventsToListenFor eventType : EventsToListenFor.values()) {
      String url = UriBuilder.fromPath(eventHandlerUrl).path("type").path(eventType.name()).path("consumer").path(CONSUMER_NAME).toString();
      Utility.sendRequest(url, "DELETE", null);
      log.info("Unsubscribed from " + eventType.name() + " events.");
    }
  }

}
