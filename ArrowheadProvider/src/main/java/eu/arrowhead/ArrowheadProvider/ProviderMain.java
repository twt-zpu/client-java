/*
 * Copyright (c) 2018 AITIA International Inc.
 *
 * This work is part of the Productive 4.0 innovation project, which receives grants from the
 * European Commissions H2020 research and innovation programme, ECSEL Joint Undertaking
 * (project no. 737459), the free state of Saxony, the German Federal Ministry of Education and
 * national funding authorities from involved countries.
 */

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
import javax.net.ssl.SSLContext;
import javax.ws.rs.core.UriBuilder;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.ssl.SSLContextConfigurator;
import org.glassfish.grizzly.ssl.SSLEngineConfigurator;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

public class ProviderMain {

  public static boolean DEBUG_MODE;
  public static PublicKey authorizationKey;
  public static PrivateKey privateKey;

  private static String SR_BASE_URI = getProp().getProperty("sr_base_uri", "http://localhost:8442/serviceregistry");
  private static HttpServer server;
  private static HttpServer secureServer;
  private static String PROVIDER_PUBLIC_KEY;
  private static Properties prop;

  private static final String BASE_URI = getProp().getProperty("base_uri", "http://localhost:8454/");
  private static final String BASE_URI_SECURED = getProp().getProperty("base_uri_secured", "https://localhost:8455/");

  public static void main(String[] args) throws IOException {
    System.out.println("Working directory: " + System.getProperty("user.dir"));
    Utility.isUrlValid(BASE_URI, false);
    Utility.isUrlValid(BASE_URI_SECURED, true);
    if (SR_BASE_URI.startsWith("https")) {
      Utility.isUrlValid(SR_BASE_URI, true);
    } else {
      Utility.isUrlValid(SR_BASE_URI, false);
    }
    if (!SR_BASE_URI.contains("serviceregistry")) {
      SR_BASE_URI = UriBuilder.fromUri(SR_BASE_URI).path("serviceregistry").build().toString();
    }

    boolean daemon = false;
    boolean serverModeSet = false;
    for (int i = 0; i < args.length; ++i) {
      switch (args[i]) {
        case "-daemon":
          daemon = true;
          System.out.println("Starting server as daemon!");
          break;
        case "-d":
          DEBUG_MODE = true;
          System.out.println("Starting server in debug mode!");
          break;
        case "-m":
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

    if (daemon) {
      System.out.println("In daemon mode, process will terminate for TERM signal...");
      Runtime.getRuntime().addShutdownHook(new Thread(() -> {
        System.out.println("Received TERM signal, shutting down...");
        shutdown(registeredEntries);
      }));
    } else {
      System.out.println("Type \"stop\" to shutdown ArrowheadProvider Server(s)...");
      BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
      String input = "";
      while (!input.equals("stop")) {
        input = br.readLine();
      }
      br.close();
      shutdown(registeredEntries);
    }
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

    SSLContext sslContext = sslCon.createSSLContext();
    Utility.setSSLContext(sslContext);

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

  private static void shutdown(List<ServiceRegistryEntry> registeredEntries) {
    unregisterFromServiceRegistry(registeredEntries);
    if (server != null) {
      server.shutdownNow();
    }
    if (secureServer != null) {
      secureServer.shutdownNow();
    }

    System.out.println("Temperature Provider Server(s) stopped.");
  }

  private static List<ServiceRegistryEntry> registerToServiceRegistry() {
    List<ServiceRegistryEntry> entries = new ArrayList<>();

    // create the URI for the request
    String registerUri = UriBuilder.fromPath(SR_BASE_URI).path("register").toString();

    // create the metadata HashMap for the service
    Map<String, String> metadata = new HashMap<>();
    metadata.put("unit", "celsius");

    // create the ArrowheadService object
    ArrowheadService service = new ArrowheadService("IndoorTemperature", Collections.singletonList("json"), metadata);

    // objects specific to insecure mode
    if (server != null) {
      URI baseUri;
      try {
        baseUri = new URI(BASE_URI);
      } catch (URISyntaxException e) {
        throw new RuntimeException("Parsing the BASE_URI resulted in an error.", e);
      }
      // create the ArrowheadSystem object
      ArrowheadSystem provider = new ArrowheadSystem("InsecureTemperatureSensor", baseUri.getHost(), baseUri.getPort(), null);
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
        } else {
          throw e;
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
      ArrowheadSystem provider = new ArrowheadSystem("SecureTemperatureSensor", baseUri.getHost(), baseUri.getPort(), PROVIDER_PUBLIC_KEY);
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
        } else {
          throw e;
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

  private static synchronized Properties getProp() {
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
