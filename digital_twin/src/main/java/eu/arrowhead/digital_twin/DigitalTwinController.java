package eu.arrowhead.digital_twin;

import eu.arrowhead.client.common.model.Event;
import java.util.concurrent.CompletableFuture;
import javax.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DigitalTwinController {

  static final String RECEIVE_EVENT_SUBPATH = "notify";
  private static final Logger log = LoggerFactory.getLogger(DigitalTwinController.class);

  private final DigitalTwinService digitalTwinService;

  @Autowired
  public DigitalTwinController(DigitalTwinService dtService) {
    this.digitalTwinService = dtService;
  }

  @PostMapping(RECEIVE_EVENT_SUBPATH)
  public ResponseEntity<?> receiveEvent(@Valid @RequestBody Event event) {
    log.info("Received new event: " + event.toString());

    CompletableFuture.supplyAsync(() -> {
      digitalTwinService.handleArrowheadEvent(event);
      return null;
    });
    return ResponseEntity.ok().build();
  }

  //TODO transfer/purchase szolgáltatás, ahol az összegyűjtött RFID adatot átadjuk
}
