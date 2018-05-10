/*
 *  Copyright (c) 2018 AITIA International Inc.
 *
 *  This work is part of the Productive 4.0 innovation project, which receives grants from the
 *  European Commissions H2020 research and innovation programme, ECSEL Joint Undertaking
 *  (project no. 737459), the free state of Saxony, the German Federal Ministry of Education and
 *  national funding authorities from involved countries.
 */

package eu.arrowhead.client.subscriber.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.arrowhead.client.subscriber.common.exception.ArrowheadException;
import eu.arrowhead.client.subscriber.common.exception.AuthException;
import eu.arrowhead.client.subscriber.common.exception.BadPayloadException;
import eu.arrowhead.client.subscriber.common.exception.DataNotFoundException;
import eu.arrowhead.client.subscriber.common.exception.DnsException;
import eu.arrowhead.client.subscriber.common.exception.DuplicateEntryException;
import eu.arrowhead.client.subscriber.common.exception.ErrorMessage;
import eu.arrowhead.client.subscriber.common.exception.UnavailableServerException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.ServiceConfigurationError;
import java.util.Set;
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
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;

@SuppressWarnings("WeakerAccess")
public final class Utility {

  private static SSLContext sslContext;
  private static final ObjectMapper mapper = JacksonJsonProviderAtRest.getMapper();

  private Utility() {
  }

  public static void setSSLContext(SSLContext context) {
    sslContext = context;
  }

  @SuppressWarnings("UnusedReturnValue")
  public static <T> Response sendRequest(String uri, String method, T payload) {
    ClientConfig configuration = new ClientConfig();
    configuration.property(ClientProperties.CONNECT_TIMEOUT, 30000);
    configuration.property(ClientProperties.READ_TIMEOUT, 30000);
    Client client;

    if (uri.startsWith("https")) {
      HostnameVerifier allHostsValid = (hostname, session) -> {
        // Decide whether to allow the connection...
        return true;
      };
      if (sslContext == null) {
        throw new ServiceConfigurationError(
            "SSL Context is not set, but secure request sending was invoked. An insecure module can not send requests to secure modules.");
      }
      client = ClientBuilder.newBuilder().sslContext(sslContext).withConfig(configuration).hostnameVerifier(allHostsValid).build();
    } else {
      client = ClientBuilder.newClient(configuration);
    }
    client.register(JacksonJsonProviderAtRest.class);

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
      throw new UnavailableServerException("Could not get any response from: " + uri, Status.SERVICE_UNAVAILABLE.getStatusCode(), e);
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
    if (errorMessageBody == null || errorMessageBody.equals("null")) {
      response.bufferEntity();
      errorMessageBody = response.readEntity(String.class);
    }

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
      switch (errorMessage.getExceptionType()) {
        case ARROWHEAD:
          throw new ArrowheadException(errorMessage.getErrorMessage(), errorMessage.getErrorCode());
        case AUTH:
          throw new AuthException(errorMessage.getErrorMessage(), errorMessage.getErrorCode());
        case BAD_METHOD:
          throw new ArrowheadException(errorMessage.getErrorMessage(), errorMessage.getErrorCode());
        case BAD_PAYLOAD:
          throw new BadPayloadException(errorMessage.getErrorMessage(), errorMessage.getErrorCode());
        case BAD_URI:
          throw new ArrowheadException(errorMessage.getErrorMessage(), errorMessage.getErrorCode());
        case DATA_NOT_FOUND:
          throw new DataNotFoundException(errorMessage.getErrorMessage(), errorMessage.getErrorCode());
        case DNSSD:
          throw new DnsException(errorMessage.getErrorMessage(), errorMessage.getErrorCode(), errorMessage.getOrigin());
        case DUPLICATE_ENTRY:
          throw new DuplicateEntryException(errorMessage.getErrorMessage(), errorMessage.getErrorCode());
        case GENERIC:
          throw new ArrowheadException(errorMessage.getErrorMessage(), errorMessage.getErrorCode());
        case JSON_PROCESSING:
          throw new ArrowheadException(errorMessage.getErrorMessage(), errorMessage.getErrorCode());
        case UNAVAILABLE:
          throw new UnavailableServerException(errorMessage.getErrorMessage(), errorMessage.getErrorCode());
      }
    }
  }

  public static KeyStore loadKeyStore(String filePath, String pass) {
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

  public static X509Certificate getFirstCertFromKeyStore(KeyStore keystore) {
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

  public static String getCertCNFromSubject(String subjectname) {
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
      System.out.println("Exception in getCertCN: " + e.toString());
      return "";
    }

    if (cn == null) {
      return "";
    }

    return cn;
  }

  public static PrivateKey getPrivateKey(KeyStore keystore, String pass) {
    PrivateKey privatekey = null;
    String element;
    try {
      Enumeration<String> enumeration = keystore.aliases();
      while (enumeration.hasMoreElements()) {
        element = enumeration.nextElement();
        privatekey = (PrivateKey) keystore.getKey(element, pass.toCharArray());
        if (privatekey != null) {
          break;
        }
      }
    } catch (KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException e) {
      e.printStackTrace();
      throw new ServiceConfigurationError("Getting the private key from keystore failed...", e);
    }

    if (privatekey == null) {
      throw new ServiceConfigurationError("Getting the private key failed, keystore aliases do not identify a key.");
    }
    return privatekey;
  }

  public static boolean isCommonNameArrowheadValid(String commonName) {
    String[] cnFields = commonName.split("\\.", 0);
    return cnFields.length == 5 && cnFields[3].equals("arrowhead") && cnFields[4].equals("eu");
  }

  public static KeyStore createKeyStoreFromCert(String filePath) {
    try {
      InputStream is = new FileInputStream(filePath);
      CertificateFactory cf = CertificateFactory.getInstance("X.509");
      X509Certificate cert = (X509Certificate) cf.generateCertificate(is);
      String alias = getCertCNFromSubject(cert.getSubjectDN().getName());
      System.out.println("alias: " + alias);

      KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
      keystore.load(null); // We don't need the KeyStore instance to come from a file.
      keystore.setCertificateEntry(alias, cert);
      return keystore;
    } catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException e) {
      e.printStackTrace();
      throw new ServiceConfigurationError("Keystore creation from cert failed...", e);
    }
  }

  public static String stripEndSlash(String uri) {
    if (uri != null && uri.endsWith("/")) {
      return uri.substring(0, uri.length() - 1);
    }
    return uri;
  }

  public static String toPrettyJson(String jsonString, Object obj) {
    try {
      if (jsonString != null) {
        jsonString = jsonString.trim();
        if (jsonString.startsWith("{")) {
          Object tempObj = mapper.readValue(jsonString, Object.class);
          return mapper.writeValueAsString(tempObj);
        } else {
          Object[] tempObj = mapper.readValue(jsonString, Object[].class);
          return mapper.writeValueAsString(tempObj);
        }
      }
      if (obj != null) {
        return mapper.writeValueAsString(obj);
      }
    } catch (IOException e) {
      throw new ArrowheadException(
          "Jackson library threw IOException during JSON serialization! Wrapping it in RuntimeException. Exception message: " + e.getMessage(), e);
    }
    return null;
  }

  public static <T> T fromJson(String json, Class<T> parsedClass) {
    try {
      return mapper.readValue(json, parsedClass);
    } catch (IOException e) {
      throw new ArrowheadException("Jackson library threw eu.arrowhead.clientcommon.exception during JSON parsing!", e);
    }
  }

  public static String getUri(String address, int port, String serviceUri, boolean isSecure) {
    if (address == null) {
      throw new NullPointerException("Address can not be null (Utility:getUri throws NPE)");
    }

    UriBuilder ub = UriBuilder.fromPath("").host(address);
    if (isSecure) {
      ub.scheme("https");
    } else {
      ub.scheme("http");
    }
    if (port > 0) {
      ub.port(port);
    }
    if (serviceUri != null) {
      ub.path(serviceUri);
    }

    String url = ub.toString();
    try {
      new URI(url);
    } catch (URISyntaxException e) {
      throw new ServiceConfigurationError(url + " is not a valid URL to start a HTTP server! Please fix the address field in the properties file.");
    }

    return url;
  }

  public static void checkProperties(Set<String> propertyNames, List<String> mandatoryProperties) {
    if (mandatoryProperties == null || mandatoryProperties.isEmpty()) {
      return;
    }
    //Arrays.asList() returns immutable lists, so we have to copy it first
    List<String> properties = new ArrayList<>(mandatoryProperties);
    if (!propertyNames.containsAll(mandatoryProperties)) {
      properties.removeIf(propertyNames::contains);
      throw new ServiceConfigurationError("Missing field(s) from app.properties file: " + properties.toString());
    }
  }

  public static String loadJsonFromFile(String pathName) {
    StringBuilder sb;
    try {
      File file = new File(pathName);
      FileInputStream is = new FileInputStream(file);

      BufferedReader br = new BufferedReader(new InputStreamReader(is, "utf-8"));
      sb = new StringBuilder();
      String line;
      while ((line = br.readLine()) != null) {
        sb.append(line).append("\n");
      }
      br.close();
    } catch (IOException e) {
      throw new RuntimeException(e.getClass().toString() + ": " + e.getMessage(), e);
    }

    if (!sb.toString().isEmpty()) {
      return sb.toString();
    } else {
      return null;
    }
  }

}
