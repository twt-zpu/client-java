package eu.arrowhead.common.api.server;

import eu.arrowhead.common.api.ArrowheadConverter;
import eu.arrowhead.common.api.ArrowheadSecurityContext;
import eu.arrowhead.common.exception.ArrowheadRuntimeException;
import eu.arrowhead.common.exception.AuthException;
import eu.arrowhead.common.misc.SecurityUtils;
import eu.arrowhead.common.model.RawTokenInfo;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.util.Base64;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.SecurityContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ArrowheadTokenSignatureSecurityFilter extends ArrowheadSecurityFilter {
  protected final Logger log = LogManager.getLogger(getClass());
  private final PrivateKey privateKey;
  private final PublicKey publicAuthKey;

  public ArrowheadTokenSignatureSecurityFilter(ArrowheadSecurityContext securityContext) {
    this.privateKey = securityContext.getPrivateKey();
    this.publicAuthKey = securityContext.getPublicAuthKey();
  }

  @Override
  public void filter(ContainerRequestContext requestContext) {
    super.filter(requestContext);
    SecurityContext sc = requestContext.getSecurityContext();
    if (sc.isSecure()) {
      String path = requestContext.getUriInfo().getPath();
      MultivaluedMap<String, String> queryParams = requestContext.getUriInfo().getQueryParameters();
      String token = queryParams.getFirst("token");
      String signature = queryParams.getFirst("signature");

      //The method will throw a runtime exception if the verification fails, which the exception mapper will catch
      verifyRequester(sc, path, token, signature);
    }
  }

  /**
   * Based on the local Authorization public key and provider private key, this method verifies that the provided
   * token/signature pair was created by the Authorization Core System with the provider public key. It also checks if
   * the token expired or not, plus the token has to contain the same consumer name as the common name field of the
   * client certificate.
   * @param context
   * @param path  Allows subclasses to filter on path
   * @param token
   * @param signature
   */
  public void verifyRequester(SecurityContext context, String path, String token, String signature) {
    if (context == null || token == null || signature == null)
      throw new AuthException("Authorization core system signature verification failed (no token/signature)!");

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
      signatureInstance.initVerify(publicAuthKey);
      signatureInstance.update(tokenbytes);

      boolean verifies = signatureInstance.verify(signaturebytes);
      if (!verifies) {
        throw new AuthException("Authorization core system signature verification failed!");
      }

      Cipher cipher = Cipher.getInstance("RSA/NONE/PKCS1Padding", "BC");
      cipher.init(Cipher.DECRYPT_MODE, privateKey);
      //Check if the provider public key registered in the database is the same as the one used by the provider at the moment
      byte[] byteToken = cipher.doFinal(tokenbytes);

      String json = new String(byteToken, StandardCharsets.UTF_8);
      RawTokenInfo rawTokenInfo = ArrowheadConverter.json().fromString(json, RawTokenInfo.class);
      String[] rawTokenInfoParts = rawTokenInfo.getC().split("\\.");
      String consumerTokenName = rawTokenInfoParts[0];

      long endTime = rawTokenInfo.getE();
      long currentTime = System.currentTimeMillis();

      if (consumerName.equalsIgnoreCase(consumerTokenName)) {
        if (!(endTime == 0L || (endTime > currentTime))) {
          throw new AuthException("Given token has expired!");
        }

      } else {
        log.warn("Token and provider name mismatch: " + consumerName + " " + consumerTokenName);
        throw new AuthException("Cert common name and token information are mismatched!");
      }

      log.info("Token and signature from " + commonName + " are ok!");
    } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException | NoSuchPaddingException | BadPaddingException | NoSuchProviderException | IllegalBlockSizeException e) {
      throw new ArrowheadRuntimeException("Internal Server Error during token validation!", 500, e);
    }
  }
}
