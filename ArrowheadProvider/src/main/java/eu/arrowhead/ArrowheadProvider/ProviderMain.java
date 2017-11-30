package eu.arrowhead.ArrowheadProvider;

import eu.arrowhead.ArrowheadProvider.common.Utility;
import eu.arrowhead.ArrowheadProvider.common.model.ArrowheadService;
import eu.arrowhead.ArrowheadProvider.common.model.ArrowheadSystem;
import eu.arrowhead.ArrowheadProvider.common.model.ServiceRegistryEntry;
import eu.arrowhead.ArrowheadProvider.common.security.AuthenticationException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.ws.rs.core.UriBuilder;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.ssl.SSLContextConfigurator;
import org.glassfish.grizzly.ssl.SSLEngineConfigurator;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

public class ProviderMain {

  private static Properties prop;
  public static PrivateKey privateKey = null;
  public static PublicKey authorizationKey = null;
  private static HttpServer server = null;
  private static HttpServer secureServer = null;
  private static final String BASE_URI = getProp().getProperty("base_uri", "http://0.0.0.0:8454/");
  private static final String BASE_URI_SECURED = getProp().getProperty("base_uri_secured", "https://0.0.0.0:8455/");
  private static final String SR_BASE_URI = getProp().getProperty("sr_base_uri", "http://arrowhead.tmit.bme.hu:8444/serviceregistry");
  private static String PROVIDER_PUBLIC_KEY;
  public static boolean DEBUG_MODE;

  public static void main(String[] args) throws IOException {
    System.out.println("Working directory: " + System.getProperty("user.dir"));

    boolean serverModeSet = false;
    for (int i = 0; i < args.length; ++i) {
      if (args[i].equals("-d")) {
        DEBUG_MODE = true;
        System.out.println("Starting server in debug mode!");
      }
      if (args[i].equals("-m")) {
        serverModeSet = true;
        ++i;
        switch (args[i]) {
          case "insecure":
            server = startServer();
            break;
          case "secure":
            secureServer = startSecureServer();
            break;
          case "both":
            server = startServer();
            secureServer = startSecureServer();
            break;
          default:
            throw new AssertionError("Unknown server mode: " + args[i]);
        }
      }
    }
    if (!serverModeSet) {
      server = startServer();
    }

    List<ServiceRegistryEntry> registeredEntries = registerToServiceRegistry();
    System.out.println("Press enter to shutdown the Temperature Provider Server(s)...");
    //noinspection ResultOfMethodCallIgnored
    System.in.read();

    unregisterFromServiceRegistry(registeredEntries);
    if (server != null) {
      server.shutdownNow();
    }
    if (secureServer != null) {
      secureServer.shutdownNow();
    }

    System.out.println("Temperature Provider Server(s) stopped.");
  }

  private static HttpServer startServer() throws IOException {
    URI uri = UriBuilder.fromUri(BASE_URI).build();
    final ResourceConfig config = new ResourceConfig();
    config.registerClasses(TemperatureResource.class, BreakResource.class);
    config.packages("eu.arrowhead.ArrowheadProvider.common.filter");

    final HttpServer server = GrizzlyHttpServerFactory.createHttpServer(uri, config);
    server.getServerConfiguration().setAllowPayloadForUndefinedHttpMethods(true);
    server.start();
    System.out.println("Insecure server launched...");
    return server;
  }

  private static HttpServer startSecureServer() throws IOException {
    URI uri = UriBuilder.fromUri(BASE_URI_SECURED).build();
    final ResourceConfig config = new ResourceConfig();
    config.registerClasses(TemperatureResource.class, BreakResource.class);
    config.packages("eu.arrowhead.ArrowheadProvider.common.filter", "eu.arrowhead.ArrowheadProvider.common.security");

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
    Utility.sslContext = sslCon.createSSLContext();

    // Getting certificate keys
    KeyStore keyStore = Utility.loadKeyStore(keystorePath, keystorePass);
    privateKey = Utility.getPrivateKey(keyStore, keystorePass);
    X509Certificate serverCert = Utility.getFirstCertFromKeyStore(keyStore);
    PROVIDER_PUBLIC_KEY = Base64.getEncoder().encodeToString(serverCert.getPublicKey().getEncoded());
    System.out.println("My certificate PublicKey in Base64: " + PROVIDER_PUBLIC_KEY);
    String serverCN = Utility.getCertCNFromSubject(serverCert.getSubjectDN().getName());
    System.out.println("My certificate CN: " + serverCN);
    config.property("server_common_name", serverCN);

    String authKeystorePath = getProp().getProperty("ssl.auth_keystore");
    String authKeystorePass = getProp().getProperty("ssl.auth_keystorepass");
    KeyStore authKeyStore = Utility.loadKeyStore(authKeystorePath, authKeystorePass);
    X509Certificate authCert = Utility.getFirstCertFromKeyStore(authKeyStore);
    authorizationKey = authCert.getPublicKey();
    System.out.println("Authorization CN: " + Utility.getCertCNFromSubject(authCert.getSubjectDN().getName()));
    //System.out.println("Authorization System PublicKey Base64: " + Base64.getEncoder().encodeToString(authorizationKey.getEncoded()));

    final HttpServer server = GrizzlyHttpServerFactory
        .createHttpServer(uri, config, true, new SSLEngineConfigurator(sslCon).setClientMode(false).setNeedClientAuth(true));
    server.getServerConfiguration().setAllowPayloadForUndefinedHttpMethods(true);
    server.start();
    System.out.println("Secure server launched...");
    return server;
  }

  private static List<ServiceRegistryEntry> registerToServiceRegistry() {
    List<ServiceRegistryEntry> entries = new ArrayList<>();

    // create the URI for the request
    String registerUri = UriBuilder.fromPath(SR_BASE_URI).path("register").toString();

    if (server != null) {
      ServiceRegistryEntry temperatureEntry = createServiceRegistryEntry("temperature", false);
      try {
        Utility.sendRequest(registerUri, "POST", temperatureEntry);
      } catch (Exception e) {
        if (e.getMessage().contains("DuplicateEntryException")) {
          System.out.println("Received DuplicateEntryException from SR, sending delete request and then registering again.");
          unregisterFromServiceRegistry(Collections.singletonList(temperatureEntry));
          Utility.sendRequest(registerUri, "POST", temperatureEntry);
        }
      }
      entries.add(temperatureEntry);

      ServiceRegistryEntry breakEntry = createServiceRegistryEntry("break", false);
      try {
        Utility.sendRequest(registerUri, "POST", breakEntry);
      } catch (Exception e) {
        if (e.getMessage().contains("DuplicateEntryException")) {
          System.out.println("Received DuplicateEntryException from SR, sending delete request and then registering again.");
          unregisterFromServiceRegistry(Collections.singletonList(breakEntry));
          Utility.sendRequest(registerUri, "POST", breakEntry);
        }
      }
      entries.add(breakEntry);
      System.out.println("Registering insecure services was successful!");
    }

    if (secureServer != null) {
      ServiceRegistryEntry temperatureEntry = createServiceRegistryEntry("temperature", true);
      try {
        Utility.sendRequest(registerUri, "POST", temperatureEntry);
      } catch (Exception e) {
        if (e.getMessage().contains("DuplicateEntryException")) {
          System.out.println("Received DuplicateEntryException from SR, sending delete request and then registering again.");
          unregisterFromServiceRegistry(Collections.singletonList(temperatureEntry));
          Utility.sendRequest(registerUri, "POST", temperatureEntry);
        }
      }
      entries.add(temperatureEntry);

      ServiceRegistryEntry breakEntry = createServiceRegistryEntry("break", true);
      try {
        Utility.sendRequest(registerUri, "POST", breakEntry);
      } catch (Exception e) {
        if (e.getMessage().contains("DuplicateEntryException")) {
          System.out.println("Received DuplicateEntryException from SR, sending delete request and then registering again.");
          unregisterFromServiceRegistry(Collections.singletonList(breakEntry));
          Utility.sendRequest(registerUri, "POST", breakEntry);
        }
      }
      entries.add(breakEntry);
      System.out.println("Registering secure services was successful!");
    }

    return entries;
  }

  private static ServiceRegistryEntry createServiceRegistryEntry(String useCase, boolean isSecure) {
    URI baseUri;
    Map<String, String> metadata = new HashMap<>();

    switch (useCase) {
      case "temperature":
        metadata.put("unit", "celsius");
        ArrowheadService tempService = new ArrowheadService("Temperature", "IndoorTemperature", Collections.singletonList("json"), metadata);
        if (isSecure) {
          //TODO test if this works as intented with the ArrowheadService
          metadata.put("security", "token");
          try {
            baseUri = new URI(BASE_URI_SECURED);
          } catch (URISyntaxException e) {
            throw new RuntimeException("Parsing the BASE_URI_SECURED resulted in an error.", e);
          }
          ArrowheadSystem provider = new ArrowheadSystem("TemperatureSensors", "SecureTemperatureSensor", baseUri.getHost(), baseUri.getPort(),
                                                         PROVIDER_PUBLIC_KEY);
          return new ServiceRegistryEntry(tempService, provider, "temperature");
        } else {
          try {
            baseUri = new URI(BASE_URI);
          } catch (URISyntaxException e) {
            throw new RuntimeException("Parsing the BASE_URI resulted in an error.", e);
          }
          ArrowheadSystem provider = new ArrowheadSystem("TemperatureSensors", "InsecureTemperatureSensor", baseUri.getHost(), baseUri.getPort(),
                                                         null);
          return new ServiceRegistryEntry(tempService, provider, "temperature");
        }
      case "break":
        ArrowheadService breakService = new ArrowheadService("SafetyServices", "BrakeSignal", Collections.singletonList("json"), metadata);
        if (isSecure) {
          metadata.put("security", "token");
          try {
            baseUri = new URI(BASE_URI_SECURED);
          } catch (URISyntaxException e) {
            throw new RuntimeException("Parsing the BASE_URI_SECURED resulted in an error.", e);
          }
          ArrowheadSystem provider = new ArrowheadSystem("CarDemoSSE", "Car1", baseUri.getHost(), baseUri.getPort(), PROVIDER_PUBLIC_KEY);
          return new ServiceRegistryEntry(breakService, provider, "break");
        } else {
          try {
            baseUri = new URI(BASE_URI);
          } catch (URISyntaxException e) {
            throw new RuntimeException("Parsing the BASE_URI resulted in an error.", e);
          }
          ArrowheadSystem provider = new ArrowheadSystem("CarDemoSSE", "Car1", baseUri.getHost(), baseUri.getPort(), null);
          return new ServiceRegistryEntry(breakService, provider, "break");
        }
      default:
        return new ServiceRegistryEntry();
    }
  }

  private static void unregisterFromServiceRegistry(List<ServiceRegistryEntry> registeredEntries) {
    // create the URI for the request
    String removeUri = UriBuilder.fromPath(SR_BASE_URI).path("remove").toString();
    // remove every service we registered (2 at max)
    for (ServiceRegistryEntry entry : registeredEntries) {
      Utility.sendRequest(removeUri, "PUT", entry);
    }

    System.out.println("Removing service(s) is successful!");
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
