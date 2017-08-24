package eu.arrowhead.ArrowheadProvider.common;

import eu.arrowhead.ArrowheadProvider.common.model.ErrorMessage;
import eu.arrowhead.ArrowheadProvider.common.ssl.AuthenticationException;
import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;
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
import javax.ws.rs.core.UriBuilder;
import org.glassfish.grizzly.ssl.SSLContextConfigurator;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;

public final class Utility {

  private static Properties prop;
  private static SSLContext sslContext = null;

  private Utility() {
  }

  public static SSLContext getSSLContext() {
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
      throw new RuntimeException(
          errorMessage.getErrorMessage() + "(This exception is from " + uri + " Status code: " + errorMessage.getErrorCode() + ")");
    }

    return response;
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

}
