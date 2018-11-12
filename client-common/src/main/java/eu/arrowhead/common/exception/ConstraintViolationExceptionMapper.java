/*
 *  Copyright (c) 2018 AITIA International Inc.
 *
 *  This work is part of the Productive 4.0 innovation project, which receives grants from the
 *  European Commissions H2020 research and innovation programme, ECSEL Joint Undertaking
 *  (project no. 737459), the free state of Saxony, the German Federal Ministry of Education and
 *  national funding authorities from involved countries.
 */

package eu.arrowhead.common.exception;

import org.apache.log4j.Logger;
import org.glassfish.jersey.server.ContainerRequest;

import javax.inject.Inject;
import javax.validation.ConstraintViolationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class ConstraintViolationExceptionMapper implements ExceptionMapper<ConstraintViolationException> {
  protected final Logger log = Logger.getLogger(getClass());

  @Inject
  private javax.inject.Provider<ContainerRequest> requestContext;

  @Override
  public Response toResponse(ConstraintViolationException ex) {
    log.warn("Replying with error message", ex);
    int errorCode = 404; //Bad Request
    String origin = requestContext.get() != null ? requestContext.get().getAbsolutePath().toString() : "unknown";

    ErrorMessage errorMessage = new ErrorMessage(ex.getMessage(), errorCode, ExceptionType.VALIDATION, origin);
    return Response.status(errorCode).entity(errorMessage).header("Content-type", "application/json").build();
  }
}
