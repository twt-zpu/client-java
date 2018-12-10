/*
 *  Copyright (c) 2018 AITIA International Inc.
 *
 *  This work is part of the Productive 4.0 innovation project, which receives grants from the
 *  European Commissions H2020 research and innovation programme, ECSEL Joint Undertaking
 *  (project no. 737459), the free state of Saxony, the German Federal Ministry of Education and
 *  national funding authorities from involved countries.
 */

package eu.arrowhead.client.publisher;

import eu.arrowhead.common.api.server.ArrowheadHttpServer;
import eu.arrowhead.common.api.server.ArrowheadResource;
import eu.arrowhead.common.exception.ArrowheadException;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Map;

@Path("publisher")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class PublisherResource extends ArrowheadResource {

  public PublisherResource(ArrowheadHttpServer server) throws ArrowheadException {
    super(server);
  }

  @GET
  public Response getIt() {
    return Response.ok().build();
  }

  @POST
  @Path("feedback")
  public Response receiveEvent(Map<String, Boolean> results) {
    log.info("Event publishing results: " + results.toString());
    return Response.ok().build();
  }

}
