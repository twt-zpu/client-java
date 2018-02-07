/*
 * Copyright (c) 2018 AITIA International Inc.
 *
 * This work is part of the Productive 4.0 innovation project, which receives grants from the
 * European Commissions H2020 research and innovation programme, ECSEL Joint Undertaking
 * (project no. 737459), the free state of Saxony, the German Federal Ministry of Education and
 * national funding authorities from involved countries.
 */

package eu.arrowhead.ArrowheadConsumer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import eu.arrowhead.ArrowheadConsumer.exception.ArrowheadException;
import eu.arrowhead.ArrowheadConsumer.exception.ErrorMessage;
import eu.arrowhead.ArrowheadConsumer.exception.UnavailableServerException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.ServiceConfigurationError;
import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.ws.rs.NotAllowedException;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.Response.Status.Family;
import javax.ws.rs.core.UriBuilder;
import org.glassfish.jersey.SslConfigurator;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;

final class Utility {

  private static Properties prop;
  private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

  private Utility() {
  }

  static <T> Response sendRequest(String uri, String method, T payload) {
    ClientConfig configuration = new ClientConfig();
    configuration.property(ClientProperties.CONNECT_TIMEOUT, 60000);
    configuration.property(ClientProperties.READ_TIMEOUT, 30000);
    Client client;

    if (uri.startsWith("https")) {
      SslConfigurator sslConfig = SslConfigurator.newInstance().trustStoreFile(getProp().getProperty("truststore"))
          .trustStorePassword(getProp().getProperty("truststorepass")).keyStoreFile(getProp().getProperty("keystore"))
          .keyStorePassword(getProp().getProperty("keystorepass")).keyPassword(getProp().getProperty("keypass"));
      SSLContext sslContext = sslConfig.createSSLContext();

      X509Certificate clientCert = null;
      try {
        KeyStore keyStore = loadKeyStore(getProp().getProperty("keystore"), getProp().getProperty("keystorepass"));
        clientCert = getFirstCertFromKeyStore(keyStore);
      } catch (Exception ex) {
        ex.printStackTrace();
      }
      if (clientCert != null) {
        String clientCN = getCertCNFromSubject(clientCert.getSubjectDN().getName());
        System.out.println("Sending request with the common name: " + clientCN + "\n");
      }
      // building hostname verifier to avoid exception
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
      throw new UnavailableServerException("Could not get any response from: " + uri, Status.SERVICE_UNAVAILABLE.getStatusCode(),
          UnavailableServerException.class.getName(), null, e);
    }

    // If the response status code does not start with 2 the request was not successful
    if (!(response.getStatusInfo().getFamily() == Family.SUCCESSFUL)) {
      handleException(response, uri);
    }

    return response;
  }

  private static void handleException(Response response, String uri) {
    //The response body has to be extracted before the stream closes
    String errorMessageBody = toPrettyJson(null, response.getEntity());
    ErrorMessage errorMessage;
    try {
      errorMessage = response.readEntity(ErrorMessage.class);
    } catch (RuntimeException e) {
      throw new ArrowheadException("Unknown error occurred at " + uri, e);
    }
    if (errorMessage == null || errorMessage.getExceptionType() == null) {
      System.out.println("Request failed, response status code: " + response.getStatus());
      System.out.println("Request failed, response body: " + errorMessageBody);
      throw new ArrowheadException("Unknown error occurred at " + uri);
    } else {
      System.out.println("Request failed, response status code: " + errorMessage.getErrorCode());
      System.out.println("The returned error message: " + errorMessage.getErrorMessage());
      System.out.println("Exception type: " + errorMessage.getExceptionType());
      System.out.println("Origin of the exception:" + errorMessage.getOrigin());
      throw new ArrowheadException(errorMessage.getErrorMessage(), errorMessage.getErrorCode(), errorMessage.getExceptionType(),
          errorMessage.getOrigin());
    }
  }

  static synchronized Properties getProp() {
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

  public static String toPrettyJson(String jsonString, Object obj) {
    if (jsonString != null) {
      jsonString = jsonString.trim();
      JsonParser parser = new JsonParser();
      if (jsonString.startsWith("{")) {
        JsonObject json = parser.parse(jsonString).getAsJsonObject();
        return gson.toJson(json);
      } else {
        JsonArray json = parser.parse(jsonString).getAsJsonArray();
        return gson.toJson(json);
      }
    }
    if (obj != null) {
      return gson.toJson(obj);
    }
    return null;
  }

  public static void isUrlValid(String url, boolean isSecure) {
    String errorMessage = " is not a valid URL to start a HTTP server! Please fix the URL in the properties file.";
    try {
      URI uri = new URI(url);

      if ("mailto".equals(uri.getScheme())) {
        throw new ServiceConfigurationError(url + errorMessage);
      }
      if (uri.getHost() == null) {
        throw new ServiceConfigurationError(url + errorMessage);
      }
      if ((isSecure && "http".equals(uri.getScheme())) || (!isSecure && "https".equals(uri.getScheme()))) {
        throw new ServiceConfigurationError("Secure URIs should use the HTTPS protocol and insecure URIs should use the HTTP protocol. Please fix "
            + "the following URL accordingly in the properties file: " + url);
      }
    } catch (URISyntaxException e) {
      throw new ServiceConfigurationError(url + errorMessage);
    }
  }

  // Below this comment are non-essential methods for acquiring the common name from the client certificate
  private static KeyStore loadKeyStore(String filePath, String pass) {
    File file = new File(filePath);

    try {
      KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
      FileInputStream is = new FileInputStream(file);
      keystore.load(is, pass.toCharArray());
      is.close();
      return keystore;
    } catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException e) {
      e.printStackTrace();
      throw new ServiceConfigurationError("Loading the keystore failed: " + e.getMessage(), e);
    }
  }

  private static X509Certificate getFirstCertFromKeyStore(KeyStore keystore) {
    try {
      Enumeration<String> enumeration = keystore.aliases();
      String alias = enumeration.nextElement();
      Certificate certificate = keystore.getCertificate(alias);
      return (X509Certificate) certificate;
    } catch (KeyStoreException | NoSuchElementException e) {
      e.printStackTrace();
      throw new ServiceConfigurationError("Getting the first cert from keystore failed: " + e.getMessage(), e);
    }
  }

  private static String getCertCNFromSubject(String subjectname) {
    String cn = null;
    try {
      // Subject is in LDAP format, we can use the LdapName object for parsing
      LdapName ldapname = new LdapName(subjectname);
      for (Rdn rdn : ldapname.getRdns()) {
        // Find the data after the CN field
        if (rdn.getType().equalsIgnoreCase("CN")) {
          cn = (String) rdn.getValue();
        }
      }
    } catch (InvalidNameException e) {
      System.out.println("InvalidNameException in getCertCNFromSubject: " + e.getMessage());
      return "";
    }

    if (cn == null) {
      return "";
    }

    return cn;
  }

}
