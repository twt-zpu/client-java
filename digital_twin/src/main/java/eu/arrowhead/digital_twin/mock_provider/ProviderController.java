package eu.arrowhead.digital_twin.mock_provider;

import java.time.LocalDateTime;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProviderController {

  static final String MILLING_SUBPATH = "milling";
  static final String ASSEMBLY_SUBPATH = "assembly";
  static final String STORE_SUBPATH = "store";
  static final String WELDING_SUBPATH = "welding";

  @GetMapping(path = MILLING_SUBPATH, produces = "text/plain")
  public String millingService() {
    return "Milling finished at " + LocalDateTime.now() + ". Machine ID: „A”, parameters: {„tool”:”GR-2MM”, „finished”: 5000}";
  }

  @GetMapping(path = ASSEMBLY_SUBPATH, produces = "text/plain")
  public String assemblyService() {
    return "Part assembly finished at " + LocalDateTime.now()
        + ", part type: „RFID2-EPC”, part ID: „RFID2-USR”. Machine ID: „B”, parameters: {„torque”: 200, „spins”: 10.56, „status”: „OK”}";
  }

  @GetMapping(path = STORE_SUBPATH, produces = "text/plain")
  public String storeService() {
    return "Product stored in WH1 at " + LocalDateTime.now() + ", parameters: {„location”: „C-137”, „handler”:”Mike”, „status”:”outbound”}";
  }

  @GetMapping(path = WELDING_SUBPATH, produces = "text/plain")
  public String weldingService() {
    return "Welding finished at " + LocalDateTime.now() + ", part type: „RFID3-EPC”, part ID: „RFID3-USR”. Machine ID: „C” , parameters: "
        + "{„weldingCurrent”: 100, „selfTest”:”Successful”}";
  }
}
