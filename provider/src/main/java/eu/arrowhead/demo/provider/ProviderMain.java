package eu.arrowhead.demo.provider;

import eu.arrowhead.common.api.ArrowheadApplication;
import eu.arrowhead.common.api.ArrowheadSecurityContext;
import eu.arrowhead.common.api.clients.core.ServiceRegistryClient;
import eu.arrowhead.common.api.server.ArrowheadGrizzlyHttpServer;
import eu.arrowhead.common.api.server.ArrowheadHttpServer;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.misc.ArrowheadProperties;
import eu.arrowhead.common.model.ServiceRegistryEntry;

public class ProviderMain extends ArrowheadApplication {

  static String customResponsePayload;

  public static void main(String[] args) throws ArrowheadException {
    new ProviderMain(args).start();
  }

  public ProviderMain(String[] args) throws ArrowheadException {
    super(args);
  }

  @Override
  protected void onStart() throws ArrowheadException {
    final ArrowheadProperties props = getProps();
    if (props.getBooleanProperty("payload_from_file", false)) {
      customResponsePayload = props.getProperty("custom_payload");
    }

    final ArrowheadSecurityContext securityContext = ArrowheadSecurityContext.createFromProperties(true);
    final ArrowheadHttpServer server = ArrowheadGrizzlyHttpServer
            .createFromProperties(securityContext)
            .addResources(TemperatureResource.class, RestResource.class)
            .addPackages("eu.arrowhead.demo")
            .start();

    final ServiceRegistryClient registry = ServiceRegistryClient.createFromProperties(securityContext);
    registry.register(ServiceRegistryEntry.createFromProperties(server));
  }

  @Override
  protected void onStop() {
    
  }

}