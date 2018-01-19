package eu.arrowhead.ArrowheadProvider;

import eu.arrowhead.ArrowheadProvider.common.Utility;
import eu.arrowhead.ArrowheadProvider.common.exception.ArrowheadException;
import eu.arrowhead.ArrowheadProvider.common.exception.AuthenticationException;
import eu.arrowhead.ArrowheadProvider.common.model.ArrowheadService;
import eu.arrowhead.ArrowheadProvider.common.model.ArrowheadSystem;
import eu.arrowhead.ArrowheadProvider.common.model.ServiceRegistryEntry;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
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

  private static HttpServer server = null;
  private static HttpServer secureServer = null;
  private static String PROVIDER_PUBLIC_KEY;
  private static Properties prop;
  private static final String BASE_URI = getProp().getProperty("base_uri", "http://0.0.0.0:8454/");
  private static final String BASE_URI_SECURED = getProp().getProperty("base_uri_secured", "https://0.0.0.0:8455/");
  private static final String SR_BASE_URI = getProp().getProperty("sr_base_uri", "http://localhost:8442/serviceregistry");

  public static PublicKey authorizationKey;
  public static PrivateKey privateKey;
  public static boolean DEBUG_MODE;

  public static void main(String[] args) throws IOException {
    System.out.println("Working directory: " + System.getProperty("user.dir"));

    boolean serverModeSet = false;
    argLoop:
    for (int i = 0; i < args.length; ++i) {
      if (args[i].equals("-m")) {
        serverModeSet = true;
        ++i;
        switch (args[i]) {
          case "insecure":
            server = startServer();
            break argLoop;
          case "secure":
            secureServer = startSecureServer();
            break argLoop;
          case "both":
            server = startServer();
            secureServer = startSecureServer();
            break argLoop;
          default:
            throw new AssertionError("Unknown server mode: " + args[i]);
        }
      }
      if (args[i].equals("-d")) {
        DEBUG_MODE = true;
        System.out.println("Starting server in debug mode!");
      }
    }
    if (!serverModeSet) {
      server = startServer();
    }

    List<ServiceRegistryEntry> registeredEntries = registerToServiceRegistry();

    System.out.println("Type \"stop\" to shutdown the Provider Server(s)...");
    BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
    String input = "";
    while (!input.equals("stop")) {
      input = br.readLine();
    }
    br.close();

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
    config.registerClasses(TemperatureResource.class);
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
    config.registerClasses(TemperatureResource.class);
    config.packages("eu.arrowhead.ArrowheadProvider.common.filter", "eu.arrowhead.ArrowheadProvider.common.security");

    String keystorePath = getProp().getProperty("keystore");
    String keystorePass = getProp().getProperty("keystorepass");
    String keyPass = getProp().getProperty("keypass");
    String truststorePath = getProp().getProperty("truststore");
    String truststorePass = getProp().getProperty("truststorepass");

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

    String authCertPath = getProp().getProperty("authorization_cert");
    KeyStore authKeyStore = Utility.createKeyStoreFromCert(authCertPath);
    X509Certificate authCert = Utility.getFirstCertFromKeyStore(authKeyStore);
    authorizationKey = authCert.getPublicKey();
    System.out.println("Authorization CN: " + Utility.getCertCNFromSubject(authCert.getSubjectDN().getName()));
    System.out.println("Authorization System PublicKey Base64: " + Base64.getEncoder().encodeToString(authorizationKey.getEncoded()));

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

    // create the metadata HashMap for the service
    Map<String, String> metadata = new HashMap<>();
    metadata.put("unit", "celsius");

    // create the ArrowheadService object
    ArrowheadService service = new ArrowheadService("Temperature", "IndoorTemperature", Collections.singletonList("json"), metadata);

    // objects specific to insecure mode
    if (server != null) {
      URI baseUri;
      try {
        baseUri = new URI(BASE_URI);
      } catch (URISyntaxException e) {
        throw new RuntimeException("Parsing the BASE_URI resulted in an error.", e);
      }
      // create the ArrowheadSystem object
      ArrowheadSystem provider = new ArrowheadSystem("TemperatureSensors", "InsecureTemperatureSensor", baseUri.getHost(), baseUri.getPort(), null);
      // create the final request payload
      ServiceRegistryEntry entry = new ServiceRegistryEntry(service, provider, "temperature");
      System.out.println("Request payload: " + Utility.toPrettyJson(null, entry));
      try {
        Utility.sendRequest(registerUri, "POST", entry);
      } catch (ArrowheadException e) {
        if (e.getExceptionType().contains("DuplicateEntryException")) {
          System.out.println("Received DuplicateEntryException from SR, sending delete request and then registering again.");
          unregisterFromServiceRegistry(Collections.singletonList(entry));
          Utility.sendRequest(registerUri, "POST", entry);
        }
      }
      System.out.println("Registering insecure service is successful!");
      entries.add(entry);
    }

    // objects specific to secure mode
    if (secureServer != null) {
      // adding metadata indicating the security choice of the provider
      metadata.put("security", "token");

      URI baseUri;
      try {
        baseUri = new URI(BASE_URI_SECURED);
      } catch (URISyntaxException e) {
        throw new RuntimeException("Parsing the BASE_URI_SECURED resulted in an error.", e);
      }
      // create the ArrowheadSystem object
      ArrowheadSystem provider = new ArrowheadSystem("TemperatureSensors", "SecureTemperatureSensor", baseUri.getHost(), baseUri.getPort(),
                                                     PROVIDER_PUBLIC_KEY);
      // create the final request payload
      ServiceRegistryEntry entry = new ServiceRegistryEntry(service, provider, "temperature");
      System.out.println("Request payload: " + Utility.toPrettyJson(null, entry));
      try {
        Utility.sendRequest(registerUri, "POST", entry);
      } catch (ArrowheadException e) {
        if (e.getExceptionType().contains("DuplicateEntryException")) {
          System.out.println("Received DuplicateEntryException from SR, sending delete request and then registering again.");
          unregisterFromServiceRegistry(Collections.singletonList(entry));
          Utility.sendRequest(registerUri, "POST", entry);
        }
      }
      System.out.println("Registering secure service is successful!");
      entries.add(entry);
    }

    return entries;
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
