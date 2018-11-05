package eu.arrowhead.demo.provider;

import eu.arrowhead.common.api.ArrowheadClient;
import eu.arrowhead.common.api.ArrowheadServer;
import eu.arrowhead.common.api.clients.CertificateAuthorityClient;
import eu.arrowhead.common.api.clients.ServiceRegistryClient;
import eu.arrowhead.common.model.ServiceRegistryEntry;

public class ProviderMain extends ArrowheadClient {

  public static void main(String[] args) {
    new ProviderMain(args);
  }

  private ProviderMain(String[] args) {
    super(args, CertificateAuthorityClient.createFromProperties());

    final ArrowheadServer server = ArrowheadServer.createFromProperties();
    server.start(
            new Class[] { TemperatureResource.class, RestResource.class },
            new String[] { "eu.arrowhead.common", "eu.arrowhead.demo" }
    );

    final ServiceRegistryClient registry = ServiceRegistryClient.createFromProperties();
    registry.register(ServiceRegistryEntry.createFromProperties(server));

    listenForInput();
  }

}