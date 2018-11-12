package eu.arrowhead.digital_twin;

import eu.arrowhead.client.common.model.Event;
import eu.arrowhead.digital_twin.model.ResourceNotFoundException;
import eu.arrowhead.digital_twin.model.SmartProduct;
import java.util.concurrent.CompletableFuture;
import javax.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

    CompletableFuture.runAsync(() -> digitalTwinService.handleArrowheadEvent(event));
    return ResponseEntity.ok().build();
  }

  @GetMapping("purchase/{rfidKey}")
  public SmartProduct transferSmartProductInformation(@PathVariable String rfidKey) {
    return digitalTwinService.findSmartProductByFirstRFID(rfidKey)
                             .orElseThrow(() -> new ResourceNotFoundException("Smart product was not found with the following RFID key: " + rfidKey));
  }
}
