/*
 *  Copyright (c) 2018 AITIA International Inc.
 *
 *  This work is part of the Productive 4.0 innovation project, which receives grants from the
 *  European Commissions H2020 research and innovation programme, ECSEL Joint Undertaking
 *  (project no. 737459), the free state of Saxony, the German Federal Ministry of Education and
 *  national funding authorities from involved countries.
 */

package eu.arrowhead.client.publisher;

import eu.arrowhead.common.api.ArrowheadServer;
import eu.arrowhead.common.api.resources.ArrowheadPublisherResource;
import eu.arrowhead.common.api.resources.ArrowheadResource;
import org.apache.log4j.Logger;

import java.util.Map;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("publisher")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class PublisherResource extends ArrowheadResource implements ArrowheadPublisherResource {
  private final Logger log = Logger.getLogger(getClass());

  public PublisherResource(ArrowheadServer server) {
    super(server);
  }

  @GET
  public Response getIt() {
    return Response.ok().build();
  }

  @Override
  public Response receiveEvent(Map<String, Boolean> results) {
    log.info("Event publishing results: " + results.toString());
    return Response.ok().build();
  }

}
