package eu.arrowhead.ArrowheadProvider.common.security;

import eu.arrowhead.ArrowheadProvider.common.Utility;
import java.io.IOException;
import javax.annotation.Priority;
import javax.inject.Inject;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Provider;

@Provider
@Priority(Priorities.AUTHORIZATION) //2nd highest priority constant, this filter gets executed after the SecurityFilter
public class AccessControlFilter implements ContainerRequestFilter {

  @Context
  Configuration configuration;
  @Inject
  private javax.inject.Provider<UriInfo> uriInfo;

  @Override
  public void filter(ContainerRequestContext requestContext) throws IOException {
    SecurityContext sc = requestContext.getSecurityContext();
    String requestTarget = Utility.stripEndSlash(requestContext.getUriInfo().getRequestUri().toString());
    if (sc.isSecure()) {
      String subjectName = sc.getUserPrincipal().getName();
      if (isClientAuthorized(subjectName)) {
        System.out.println("SSL identification is successful! Cert: " + subjectName);
      } else {
        System.out.println(Utility.getCertCNFromSubject(subjectName) + " is unauthorized to access " + requestTarget);
        throw new AuthenticationException(Utility.getCertCNFromSubject(subjectName) + " is unauthorized to access " + requestTarget);
      }
    }
  }

  private boolean isClientAuthorized(String subjectName) {
    String clientCN = Utility.getCertCNFromSubject(subjectName);
    String serverCN = (String) configuration.getProperty("server_common_name");

    if (!Utility.isCommonNameArrowheadValid(clientCN)) {
      System.out.println("Client cert does not have 6 parts, so the access will be denied.");
      return false;
    }
    // All requests from the local cloud are allowed, so omit the first 2 parts of the common names (systemName.systemGroup)
    String[] serverFields = serverCN.split("\\.", 3);
    String[] clientFields = clientCN.split("\\.", 3);
    // serverFields contains: coreSystemName, coresystems, cloudName.operator.arrowhead.eu

    // If this is true, then the certificates are from the same local cloud
    return serverFields[2].equalsIgnoreCase(clientFields[2]);
  }

}
