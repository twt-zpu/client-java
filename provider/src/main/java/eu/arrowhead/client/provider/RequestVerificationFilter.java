package eu.arrowhead.client.provider;

import eu.arrowhead.client.common.Utility;
import eu.arrowhead.client.common.exception.ArrowheadException;
import eu.arrowhead.client.common.exception.AuthException;
import eu.arrowhead.client.common.misc.SecurityUtils;
import eu.arrowhead.client.common.model.RawTokenInfo;
import java.nio.charset.StandardCharsets;
import java.security.Signature;
import java.util.Base64;
import javax.annotation.Priority;
import javax.crypto.Cipher;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.ext.Provider;

@Provider
@Priority(Priorities.AUTHORIZATION)
public class RequestVerificationFilter implements ContainerRequestFilter {

  private enum TokenVerificationResult {OK, SIGNATURE, EXPIRED, MISMATCH, SERVER_ERROR}

  @Override
  public void filter(ContainerRequestContext requestContext) {
    SecurityContext sc = requestContext.getSecurityContext();
    if (sc.isSecure()) {
      MultivaluedMap<String, String> queryParams = requestContext.getUriInfo().getQueryParameters();
      String token = queryParams.getFirst("token");
      String signature = queryParams.getFirst("signature");

      TokenVerificationResult result = verifyRequester(sc, token, signature);
      switch (result) {
        case OK:
          break;
        case SIGNATURE:
          throw new AuthException("Authorization core system signature verification failed!");
        case EXPIRED:
          throw new AuthException("Given token has expired!");
        case MISMATCH:
          throw new AuthException("Cert common name and token information are mismatched!");
        case SERVER_ERROR:
          throw new ArrowheadException("Internal Server Error during token validation!", 500);
      }
    }
  }

  /*
    Based on the local Authorization public key and provider private key, this method verifies that the provided token/signature pair
    was created by the Authorization Core System with the provider public key. It also checks if the token expired or not, plus the token
    has to contain the same consumer name as the common name field of the client certificate.
   */
  private TokenVerificationResult verifyRequester(SecurityContext context, String token, String signature) {
    try {
      String commonName = SecurityUtils.getCertCNFromSubject(context.getUserPrincipal().getName());
      String[] commonNameParts = commonName.split("\\.");
      String consumerName = commonNameParts[0];

      /*System.out.println(token);
      System.out.println(signature);*/
      if (token.contains(" ")) {
        token = token.replaceAll("\\s", "+");
      }
      if (signature.contains(" ")) {
        signature = signature.replaceAll("\\s", "+");
      }

      byte[] tokenbytes = Base64.getDecoder().decode(token);
      byte[] signaturebytes = Base64.getDecoder().decode(signature);

      SecurityUtils.addSecurityProvider();
      Signature signatureInstance = Signature.getInstance("SHA256withRSA", "BC");
      signatureInstance.initVerify(FullProviderMain.authorizationKey);
      signatureInstance.update(tokenbytes);

      boolean verifies = signatureInstance.verify(signaturebytes);
      if (!verifies) {
        return TokenVerificationResult.SIGNATURE;
        //throw new AuthException("Authorization core system signature verification failed!");
      }

      Cipher cipher = Cipher.getInstance("RSA/NONE/PKCS1Padding", "BC");
      cipher.init(Cipher.DECRYPT_MODE, FullProviderMain.privateKey);
      //Check if the provider public key registered in the database is the same as the one used by the provider at the moment
      byte[] byteToken = cipher.doFinal(tokenbytes);

      String json = new String(byteToken, StandardCharsets.UTF_8);
      RawTokenInfo rawTokenInfo = Utility.fromJson(json, RawTokenInfo.class);
      String[] rawTokenInfoParts = rawTokenInfo.getC().split("\\.");
      String consumerTokenName = rawTokenInfoParts[0];

      long endTime = rawTokenInfo.getE();
      long currentTime = System.currentTimeMillis();

      if (consumerName.equalsIgnoreCase(consumerTokenName)) {
        if (endTime == 0L || (endTime > currentTime)) {
          return TokenVerificationResult.OK;
        }
        return TokenVerificationResult.EXPIRED;
        //throw new AuthException("Given token has expired!");

      } else {
        return TokenVerificationResult.MISMATCH;
        //throw new AuthException("Cert common name and token information are mismatched!");
      }

    } catch (Exception ex) {
      ex.printStackTrace();
      return TokenVerificationResult.SERVER_ERROR;
    }
  }
}
