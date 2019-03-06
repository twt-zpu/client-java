/*
 *  Copyright (c) 2018 AITIA International Inc.
 *
 *  This work is part of the Productive 4.0 innovation project, which receives grants from the
 *  European Commissions H2020 research and innovation programme, ECSEL Joint Undertaking
 *  (project no. 737459), the free state of Saxony, the German Federal Ministry of Education and
 *  national funding authorities from involved countries.
 */

package eu.arrowhead.client.publisher;

import eu.arrowhead.common.api.ArrowheadApplication;
import eu.arrowhead.common.api.ArrowheadSecurityContext;
import eu.arrowhead.common.api.clients.core.EventHandlerClient;
import eu.arrowhead.common.api.server.ArrowheadGrizzlyHttpServer;
import eu.arrowhead.common.api.server.ArrowheadHttpServer;
import eu.arrowhead.common.api.server.ArrowheadSecurityFilter;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.misc.ArrowheadProperties;
import eu.arrowhead.common.model.ArrowheadSystem;
import eu.arrowhead.common.model.Event;
import java.util.Timer;
import java.util.TimerTask;

class PublisherMain extends ArrowheadApplication {

  public static void main(String[] args) throws ArrowheadException {
    new PublisherMain(args).start();
  }

  private PublisherMain(String[] args) throws ArrowheadException {
    super(args);
  }

  @Override
  protected void onStart() throws ArrowheadException {
    final ArrowheadSecurityContext securityContext = ArrowheadSecurityContext.createFromProperties(true);
    final ArrowheadHttpServer server = ArrowheadGrizzlyHttpServer
            .createFromProperties(securityContext)
            .addResources(PublisherResource.class)
            .setSecurityFilter(new ArrowheadSecurityFilter())
            .start();

    final ArrowheadSystem me = ArrowheadSystem.createFromProperties(server);
    final EventHandlerClient eventHandler = EventHandlerClient.createFromProperties(securityContext);
    final ArrowheadProperties props = getProps();
    final String eventType = props.getEventType();
    final boolean secure = props.isSecure();

    Timer timer = new Timer();
    TimerTask authTask = new TimerTask() {
      @Override
      public void run() {
        eventHandler.publish(new Event(eventType, String.format("Hello %s World", secure ? "secure" : "insecure")), me);
      }
    };
    timer.schedule(authTask, 2L * 1000L, 8L * 1000L);
  }

  @Override
  protected void onStop() {

  }

}
