/*
 *  Copyright (c) 2018 AITIA International Inc.
 *
 *  This work is part of the Productive 4.0 innovation project, which receives grants from the
 *  European Commissions H2020 research and innovation programme, ECSEL Joint Undertaking
 *  (project no. 737459), the free state of Saxony, the German Federal Ministry of Education and
 *  national funding authorities from involved countries.
 */

package eu.arrowhead.ArrowheadSubscriber;

import eu.arrowhead.ArrowheadSubscriber.common.TypeSafeProperties;
import eu.arrowhead.ArrowheadSubscriber.common.Utility;
import eu.arrowhead.ArrowheadSubscriber.common.exception.AuthException;
import eu.arrowhead.ArrowheadSubscriber.common.model.ArrowheadSystem;
import eu.arrowhead.ArrowheadSubscriber.common.model.EventFilter;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.ServiceConfigurationError;
import javax.net.ssl.SSLContext;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.core.UriBuilder;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.ssl.SSLContextConfigurator;
import org.glassfish.grizzly.ssl.SSLEngineConfigurator;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

public class SubscriberMain {

  public static boolean DEBUG_MODE;

  private static boolean IS_SECURE;
  private static String BASE_URI;
  private static String EH_BASE_URI;
  private static HttpServer server;
  private static String PROVIDER_PUBLIC_KEY;
  private static TypeSafeProperties prop;

  public static void main(String[] args) {
    System.out.println("Working directory: " + System.getProperty("user.dir"));

    String address = getProp().getProperty("address", "0.0.0.0");
    int insecurePort = getProp().getIntProperty("insecure_port", 8462);
    int securePort = getProp().getIntProperty("secure_port", 8463);

    String ehAddress = getProp().getProperty("eh_address", "0.0.0.0");
    int ehInsecurePort = getProp().getIntProperty("eh_insecure_port", 8454);
    int ehSecurePort = getProp().getIntProperty("eh_secure_port", 8455);

    boolean daemon = false;
    for (String arg : args) {
      switch (arg) {
        case "-daemon":
          daemon = true;
          System.out.println("Starting server as daemon!");
          break;
        case "-d":
          DEBUG_MODE = true;
          System.out.println("Starting server in debug mode!");
          break;
        case "-tls":
          List<String> secureMandatoryProperties = new ArrayList<>(
              Arrays.asList("keystore", "keystorepass", "keypass", "truststore", "truststorepass"));
          Utility.checkProperties(getProp().stringPropertyNames(), secureMandatoryProperties);
          BASE_URI = Utility.getUri(address, securePort, null, true);
          EH_BASE_URI = Utility.getUri(ehAddress, ehSecurePort, "eventhandler/subscription", true);
          server = startSecureServer();
          IS_SECURE = true;
          break;
      }
    }
    if (server == null) {
      BASE_URI = Utility.getUri(address, insecurePort, null, false);
      EH_BASE_URI = Utility.getUri(ehAddress, ehInsecurePort, "eventhandler/subscription", false);
      server = startServer();
    }

    subscribe();

    if (daemon) {
      System.out.println("In daemon mode, process will terminate for TERM signal...");
      Runtime.getRuntime().addShutdownHook(new Thread(() -> {
        System.out.println("Received TERM signal, shutting down...");
        shutdown();
      }));
    } else {
      System.out.println("Type \"stop\" to shutdown ArrowheadProvider Server...");
      BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
      String input = "";
      try {
        while (!input.equals("stop")) {
          input = br.readLine();
        }
        br.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
      shutdown();
    }
  }

  private static HttpServer startServer() {
    URI uri = UriBuilder.fromUri(BASE_URI).build();
    final ResourceConfig config = new ResourceConfig();
    config.registerClasses(SubscriberResource.class);
    config.packages("eu.arrowhead.ArrowheadSubscriber.common");

    final HttpServer server;
    try {
      server = GrizzlyHttpServerFactory.createHttpServer(uri, config, false);
      server.getServerConfiguration().setAllowPayloadForUndefinedHttpMethods(true);
      server.start();
    } catch (IOException | ProcessingException e) {
      throw new ServiceConfigurationError(
          "Make sure you gave a valid address in the app.properties file! (Assignable to this JVM and not in use already)", e);
    }
    System.out.println("Insecure server launched...");
    return server;
  }

  private static HttpServer startSecureServer() {
    URI uri = UriBuilder.fromUri(BASE_URI).build();
    final ResourceConfig config = new ResourceConfig();
    config.registerClasses(SubscriberResource.class);
    config.packages("eu.arrowhead.ArrowheadSubscriber.common");

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
      throw new AuthException("SSL Context is not valid, check the certificate files or app.properties!");
    }

    SSLContext sslContext = sslCon.createSSLContext();
    Utility.setSSLContext(sslContext);

    // Getting certificate keys
    KeyStore keyStore = Utility.loadKeyStore(keystorePath, keystorePass);
    X509Certificate serverCert = Utility.getFirstCertFromKeyStore(keyStore);
    PROVIDER_PUBLIC_KEY = Base64.getEncoder().encodeToString(serverCert.getPublicKey().getEncoded());
    System.out.println("My certificate PublicKey in Base64: " + PROVIDER_PUBLIC_KEY);
    String serverCN = Utility.getCertCNFromSubject(serverCert.getSubjectDN().getName());
    System.out.println("My certificate CN: " + serverCN);
    config.property("server_common_name", serverCN);

    final HttpServer server;
    try {
      server = GrizzlyHttpServerFactory
          .createHttpServer(uri, config, true, new SSLEngineConfigurator(sslCon).setClientMode(false).setNeedClientAuth(true), false);
      server.getServerConfiguration().setAllowPayloadForUndefinedHttpMethods(true);
      server.start();
    } catch (IOException | ProcessingException e) {
      throw new ServiceConfigurationError(
          "Make sure you gave a valid address in the app.properties file! (Assignable to this JVM and not in use already)", e);
    }
    System.out.println("Secure server launched...");
    return server;
  }

  private static void shutdown() {
    unsubscribe();
    if (server != null) {
      server.shutdownNow();
    }
    System.out.println("Temperature Provider Server stopped.");
    System.exit(0);
  }

  private static void subscribe() {
    //Create the EventFilter payload (hardwired for now)
    URI baseUri;
    try {
      baseUri = new URI(BASE_URI);
    } catch (URISyntaxException e) {
      throw new AssertionError("Parsing the BASE_URI resulted in an error.", e);
    }

    ArrowheadSystem consumer = new ArrowheadSystem(null, baseUri.getHost(), baseUri.getPort(), PROVIDER_PUBLIC_KEY);
    consumer.setSystemName(IS_SECURE ? "SecureSubscriber" : "InsecureSubscriber");

    EventFilter filter = new EventFilter("test", consumer, "notify");
    Utility.sendRequest(EH_BASE_URI, "POST", filter);
    System.out.println("Subscribed to \"test\" event types.");
  }

  private static void unsubscribe() {
    String url = UriBuilder.fromPath(EH_BASE_URI).path("type").path("test").path("consumer").path("InsecureSubscriber").toString();
    Utility.sendRequest(url, "DELETE", null);
    System.out.println("Unsubscribed from \"test\" event types.");
  }

  private static synchronized TypeSafeProperties getProp() {
    try {
      if (prop == null) {
        prop = new TypeSafeProperties();
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
