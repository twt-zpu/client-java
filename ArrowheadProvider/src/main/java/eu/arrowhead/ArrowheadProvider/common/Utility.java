package eu.arrowhead.ArrowheadProvider.common;

import com.google.gson.Gson;
import eu.arrowhead.ArrowheadProvider.ProviderMain;
import eu.arrowhead.ArrowheadProvider.common.model.ArrowheadSystem;
import eu.arrowhead.ArrowheadProvider.common.model.ErrorMessage;
import eu.arrowhead.ArrowheadProvider.common.model.RawTokenInfo;
import eu.arrowhead.ArrowheadProvider.common.ssl.AuthenticationException;
import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.InvocationTargetException;
import java.security.Principal;
import java.security.Signature;
import java.util.Base64;
import java.util.Properties;
import javax.crypto.Cipher;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.ws.rs.NotAllowedException;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status.Family;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriBuilder;
import org.glassfish.grizzly.ssl.SSLContextConfigurator;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;

public final class Utility {

  private static Properties prop;
  private static SSLContext sslContext = null;

  private Utility() {
  }

  private static SSLContext getSSLContext() {
    if (sslContext == null) {
      String keystorePath = getProp().getProperty("ssl.keystore");
      String keystorePass = getProp().getProperty("ssl.keystorepass");
      String keyPass = getProp().getProperty("ssl.keypass");
      String truststorePath = getProp().getProperty("ssl.truststore");
      String truststorePass = getProp().getProperty("ssl.truststorepass");

      SSLContextConfigurator sslCon = new SSLContextConfigurator();
      sslCon.setKeyStoreFile(keystorePath);
      sslCon.setKeyStorePass(keystorePass);
      sslCon.setKeyPass(keyPass);
      sslCon.setTrustStoreFile(truststorePath);
      sslCon.setTrustStorePass(truststorePass);
      if (!sslCon.validateConfiguration(true)) {
        throw new AuthenticationException("SSL Context is not valid, check the certificate files or app.properties!");
      }

      sslContext = sslCon.createSSLContext();
    }

    return sslContext;
  }

  @SuppressWarnings("UnusedReturnValue")
  public static <T> Response sendRequest(String uri, String method, T payload) {
    ClientConfig configuration = new ClientConfig();
    configuration.property(ClientProperties.CONNECT_TIMEOUT, 30000);
    configuration.property(ClientProperties.READ_TIMEOUT, 30000);
    Client client;

    if (uri.startsWith("https")) {
      SSLContext sslContext = getSSLContext();
      HostnameVerifier allHostsValid = (hostname, session) -> {
        // Decide whether to allow the connection...
        return true;
      };

      client = ClientBuilder.newBuilder().sslContext(sslContext).withConfig(configuration).hostnameVerifier(allHostsValid).build();
    } else {
      client = ClientBuilder.newClient(configuration);
    }

    Response response;
    try {
      WebTarget target = client.target(UriBuilder.fromUri(uri).build());
      switch (method) {
        case "GET":
          response = target.request().header("Content-type", "application/json").get();
          break;
        case "POST":
          response = target.request().header("Content-type", "application/json").post(Entity.json(payload));
          break;
        case "PUT":
          response = target.request().header("Content-type", "application/json").put(Entity.json(payload));
          break;
        case "DELETE":
          response = target.request().header("Content-type", "application/json").delete();
          break;
        default:
          throw new NotAllowedException("Invalid method type was given to the Utility.sendRequest() method");
      }
    } catch (ProcessingException e) {
      throw new RuntimeException("Could not get any response from: " + uri);
    }

    //If the response status code does not start with 2 the request was not successful
    if (!(response.getStatusInfo().getFamily() == Family.SUCCESSFUL)) {
      ErrorMessage errorMessage;
      try {
        errorMessage = response.readEntity(ErrorMessage.class);
      } catch (RuntimeException e) {
        throw new RuntimeException("Unknown error occurred at " + uri);
      }
      //noinspection unchecked
      throwExceptionAgain(errorMessage.getExceptionType(), errorMessage.getErrorMessage() + "(This exception was passed from another module)");
    }

    return response;
  }

  public static <T> Response verifyRequester(SecurityContext context, String token, String signature, T responseEntity) {
    try {
      Principal consumerPrincipal = context.getUserPrincipal();
      String consumerName = consumerPrincipal.getName().substring(3, consumerPrincipal.getName().indexOf(" ") - 1);

      ArrowheadSystem consumer = new ArrowheadSystem();
      String[] consumerNameParts = consumerName.split("\\.");
      consumer.setSystemName(consumerNameParts[0]);
      consumer.setSystemGroup(consumerNameParts[1]);

      byte[] tokenbytes = Base64.getDecoder().decode(token);
      byte[] signaturebytes = Base64.getDecoder().decode(signature);

      Signature signatureInstance = Signature.getInstance("SHA1withRSA");
      signatureInstance.initVerify(ProviderMain.authorizationKey);
      signatureInstance.update(tokenbytes);

      boolean verifies = signatureInstance.verify(signaturebytes);

      if (!verifies) {
        ErrorMessage error = new ErrorMessage("Token validation failed", 401, AuthenticationException.class);
        return Response.status(401).entity(error).build();
      }

      Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
      cipher.init(Cipher.DECRYPT_MODE, ProviderMain.privateKey);
      byte[] byteToken = cipher.doFinal(tokenbytes);

      Gson gson = new Gson();
      String json = new String(byteToken, "UTF-8");
      RawTokenInfo rawTokenInfo = gson.fromJson(json, RawTokenInfo.class);

      ArrowheadSystem consumerWithToken = new ArrowheadSystem();
      String[] rawTokenInfoParts = rawTokenInfo.getC().split("\\.");
      consumerWithToken.setSystemName(rawTokenInfoParts[0]);
      consumerWithToken.setSystemGroup(rawTokenInfoParts[1]);

      long endTime = rawTokenInfo.getE();
      long currentTime = System.currentTimeMillis();

      if (consumer.equals(consumerWithToken)) {
        if (endTime == 0L || (endTime > currentTime)) {
          return Response.status(200).entity(responseEntity).build();
        }
        ErrorMessage error = new ErrorMessage("Authorization token has expired", 401, AuthenticationException.class);
        return Response.status(401).entity(error).build();

      } else {
        ErrorMessage error = new ErrorMessage("Permission denied", 401, AuthenticationException.class);
        return Response.status(401).entity(error).build();
      }

    } catch (Exception ex) {
      ex.printStackTrace();
      ErrorMessage error = new ErrorMessage("Internal Server Error: " + ex.getMessage(), 500, null);
      return Response.status(500).entity(error).build();
    }
  }

  public static synchronized Properties getProp() {
    try {
      if (prop == null) {
        prop = new Properties();
        File file = new File("config" + File.separator + "app.properties");
        FileInputStream inputStream = new FileInputStream(file);
        prop.load(inputStream);
      }
    } catch (Exception ex) {
      ex.printStackTrace();
    }

    return prop;
  }

  // IMPORTANT: only use this function with RuntimeExceptions that have a public String constructor
  private static <T extends RuntimeException> void throwExceptionAgain(Class<T> exceptionType, String message) {
    try {
      throw exceptionType.getConstructor(String.class).newInstance(message);
    }
    // Exception is thrown if the given exception type does not have an accessible constructor which accepts a String argument.
    catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException |
        SecurityException e) {
      e.printStackTrace();
    }
  }

}
