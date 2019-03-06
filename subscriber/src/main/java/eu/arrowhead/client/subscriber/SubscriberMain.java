/*
 *  Copyright (c) 2018 AITIA International Inc.
 *
 *  This work is part of the Productive 4.0 innovation project, which receives grants from the
 *  European Commissions H2020 research and innovation programme, ECSEL Joint Undertaking
 *  (project no. 737459), the free state of Saxony, the German Federal Ministry of Education and
 *  national funding authorities from involved countries.
 */

package eu.arrowhead.client.subscriber;

import eu.arrowhead.common.api.ArrowheadApplication;
import eu.arrowhead.common.api.ArrowheadSecurityContext;
import eu.arrowhead.common.api.clients.core.EventHandlerClient;
import eu.arrowhead.common.api.server.ArrowheadGrizzlyHttpServer;
import eu.arrowhead.common.api.server.ArrowheadHttpServer;
import eu.arrowhead.common.api.server.ArrowheadSecurityFilter;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.model.ArrowheadSystem;

class SubscriberMain extends ArrowheadApplication {

  public static void main(String[] args) throws ArrowheadException {
    new SubscriberMain(args).start();
  }

  private SubscriberMain(String[] args) throws ArrowheadException {
    super(args);
  }

  @Override
  protected void onStart() throws ArrowheadException {
    final ArrowheadSecurityContext securityContext = ArrowheadSecurityContext.createFromProperties(true);
    final ArrowheadHttpServer server = ArrowheadGrizzlyHttpServer
            .createFromProperties(securityContext)
            .addResources(SubscriberResource.class)
            .setSecurityFilter(new ArrowheadSecurityFilter())
            .start();

    final ArrowheadSystem me = ArrowheadSystem.createFromProperties(server);

    final EventHandlerClient eventHandler = EventHandlerClient.createFromProperties(securityContext);
    eventHandler.subscribe(getProps().getEventType(), me);
  }

  @Override
  protected void onStop() {

  }

}
