/*
 *  Copyright (c) 2018 AITIA International Inc.
 *
 *  This work is part of the Productive 4.0 innovation project, which receives grants from the
 *  European Commissions H2020 research and innovation programme, ECSEL Joint Undertaking
 *  (project no. 737459), the free state of Saxony, the German Federal Ministry of Education and
 *  national funding authorities from involved countries.
 */

package eu.arrowhead.client.subscriber;

import eu.arrowhead.common.api.ArrowheadClient;
import eu.arrowhead.common.api.ArrowheadServer;
import eu.arrowhead.common.api.clients.CertificateAuthorityClient;
import eu.arrowhead.common.api.clients.EventHandlerClient;
import eu.arrowhead.common.model.ArrowheadSystem;

public class SubscriberMain extends ArrowheadClient {

  public static void main(String[] args) {
    new SubscriberMain(args);
  }

  private SubscriberMain(String[] args) {
    super(args, CertificateAuthorityClient.createFromProperties());

    final ArrowheadServer server = ArrowheadServer.createFromProperties();
    server.start(new Class[] { SubscriberResource.class });

    final ArrowheadSystem me = ArrowheadSystem.createFromProperties(server);

    final EventHandlerClient eventHandler = EventHandlerClient.createFromProperties();
    eventHandler.subscribe(props.getEventType(), me);

    listenForInput();
  }

}
