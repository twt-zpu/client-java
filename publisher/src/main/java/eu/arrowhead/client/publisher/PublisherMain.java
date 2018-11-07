/*
 *  Copyright (c) 2018 AITIA International Inc.
 *
 *  This work is part of the Productive 4.0 innovation project, which receives grants from the
 *  European Commissions H2020 research and innovation programme, ECSEL Joint Undertaking
 *  (project no. 737459), the free state of Saxony, the German Federal Ministry of Education and
 *  national funding authorities from involved countries.
 */

package eu.arrowhead.client.publisher;

import eu.arrowhead.common.api.ArrowheadClient;
import eu.arrowhead.common.api.ArrowheadSecurityContext;
import eu.arrowhead.common.api.ArrowheadServer;
import eu.arrowhead.common.api.clients.CertificateAuthorityClient;
import eu.arrowhead.common.api.clients.EventHandlerClient;
import eu.arrowhead.common.exception.KeystoreException;
import eu.arrowhead.common.model.ArrowheadSystem;
import eu.arrowhead.common.model.Event;

import java.util.*;

class PublisherMain extends ArrowheadClient {

  public static void main(String[] args) {
    new PublisherMain(args);
  }

  private PublisherMain(String[] args) {
    super(args);

    ArrowheadSecurityContext securityContext = null;
    if (props.isSecure()) {
      try {
        securityContext = ArrowheadSecurityContext.createFromProperties();
      } catch (KeystoreException e) {
        securityContext = CertificateAuthorityClient.createFromProperties().bootstrap(true);
      }
    }

    final ArrowheadServer server = ArrowheadServer.createFromProperties(securityContext);
    server.start(new Class[] { PublisherResource.class });

    final ArrowheadSystem me = ArrowheadSystem.createFromProperties(server);

    final EventHandlerClient eventHandler = EventHandlerClient.createFromProperties(securityContext);
    Timer timer = new Timer();
    TimerTask authTask = new TimerTask() {
      @Override
      public void run() {
        eventHandler.publish(new Event(props.getEventType(), "Hello World"), me);
      }
    };
    timer.schedule(authTask, 2L * 1000L, 8L * 1000L);

    listenForInput();
  }

}
