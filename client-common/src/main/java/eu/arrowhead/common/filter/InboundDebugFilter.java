/*
 *  Copyright (c) 2018 AITIA International Inc.
 *
 *  This work is part of the Productive 4.0 innovation project, which receives grants from the
 *  European Commissions H2020 research and innovation programme, ECSEL Joint Undertaking
 *  (project no. 737459), the free state of Saxony, the German Federal Ministry of Education and
 *  national funding authorities from involved countries.
 */

package eu.arrowhead.common.filter;

import eu.arrowhead.common.misc.Utility;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.ext.Provider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Provider
@Priority(Priorities.USER)
public class InboundDebugFilter implements ContainerRequestFilter {
  protected final Logger log = LogManager.getLogger(getClass());

  @Override
  public void filter(ContainerRequestContext requestContext) {
    if (Boolean.valueOf(System.getProperty("debug_mode", "false"))) {
      log.info("New " + requestContext.getMethod() + " request at: " + requestContext.getUriInfo().getRequestUri().toString());
      String prettyJson = Utility.getRequestPayload(requestContext.getEntityStream());
      log.info(prettyJson);

      InputStream in = new ByteArrayInputStream(prettyJson.getBytes(StandardCharsets.UTF_8));
      requestContext.setEntityStream(in);
    }
  }
}
