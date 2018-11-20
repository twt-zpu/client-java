/*
 *  Copyright (c) 2018 AITIA International Inc.
 *
 *  This work is part of the Productive 4.0 innovation project, which receives grants from the
 *  European Commissions H2020 research and innovation programme, ECSEL Joint Undertaking
 *  (project no. 737459), the free state of Saxony, the German Federal Ministry of Education and
 *  national funding authorities from involved countries.
 */

package eu.arrowhead.client.subscriber;

import eu.arrowhead.common.api.resources.ArrowheadResource;
import eu.arrowhead.common.api.resources.ArrowheadSubscriberResource;
import eu.arrowhead.common.api.server.ArrowheadHttpServer;
import eu.arrowhead.common.model.Event;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("subscriber")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class SubscriberResource extends ArrowheadResource implements ArrowheadSubscriberResource {

  public SubscriberResource(ArrowheadHttpServer server) {
    super(server);
  }

  @GET
  public Response getIt() {
    return Response.ok().build();
  }

  @Override
  public Response receiveEvent(Event event) {
    log.info("Received new event: " + event.toString());
    return Response.ok().build();
  }

}
