/*
 *  Copyright (c) 2018 AITIA International Inc.
 *
 *  This work is part of the Productive 4.0 innovation project, which receives grants from the
 *  European Commissions H2020 research and innovation programme, ECSEL Joint Undertaking
 *  (project no. 737459), the free state of Saxony, the German Federal Ministry of Education and
 *  national funding authorities from involved countries.
 */

package eu.arrowhead.ArrowheadPublisher;

import eu.arrowhead.ArrowheadPublisher.common.TypeSafeProperties;
import eu.arrowhead.ArrowheadPublisher.common.exception.AuthException;
import eu.arrowhead.ArrowheadPublisher.common.model.ArrowheadSystem;
import eu.arrowhead.ArrowheadPublisher.common.model.PublishEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
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

public class PublisherMain {

  public static boolean DEBUG_MODE;

  private static boolean IS_SECURE;
  private static String BASE_URI;
  private static String EH_BASE_URI;
  private static HttpServer server;
  private static String PROVIDER_PUBLIC_KEY;
  private static TypeSafeProperties prop = getProp();

  public static void main(String[] args) {
    System.out.println("Working directory: " + System.getProperty("user.dir"));

    String address = prop.getProperty("address", "0.0.0.0");
    int insecurePort = prop.getIntProperty("insecure_port", 8462);
    int securePort = prop.getIntProperty("secure_port", 8463);

    String ehAddress = prop.getProperty("eh_address", "0.0.0.0");
    int ehInsecurePort = prop.getIntProperty("eh_insecure_port", 8454);
    int ehSecurePort = prop.getIntProperty("eh_secure_port", 8455);

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
          Utility.checkProperties(prop.stringPropertyNames(), secureMandatoryProperties);
          BASE_URI = Utility.getUri(address, securePort, null, true);
          EH_BASE_URI = Utility.getUri(ehAddress, ehSecurePort, "eventhandler/publish", true);
          server = startSecureServer();
          IS_SECURE = true;
          break;
      }
    }
    if (server == null) {
      BASE_URI = Utility.getUri(address, insecurePort, null, false);
      EH_BASE_URI = Utility.getUri(ehAddress, ehInsecurePort, "eventhandler/publish", false);
      server = startServer();
    }

    publishEvent();
  }

  private static void publishEvent() {
    String systemName = IS_SECURE ? prop.getProperty("secure_system_name") : prop.getProperty("insecure_system_name");
    String address = prop.getProperty("address", "0.0.0.0");
    int insecurePort = prop.getIntProperty("insecure_port", 8462);
    int securePort = prop.getIntProperty("secure_port", 8463);
    int usedPort = IS_SECURE ? securePort : insecurePort;
    String type = prop.getProperty("event_type");
    String payload = prop.getProperty("event_payload");
    String callbackUri = prop.getProperty("callback_uri");

    ArrowheadSystem source = new ArrowheadSystem(systemName, address, usedPort, PROVIDER_PUBLIC_KEY);
    PublishEvent event = new PublishEvent(source, type, payload, LocalDateTime.now(), null, callbackUri);
    Utility.sendRequest(EH_BASE_URI, "POST", event);
    System.out.println("Event published to EH.");
  }

  private static HttpServer startServer() {
    URI uri = UriBuilder.fromUri(BASE_URI).build();
    final ResourceConfig config = new ResourceConfig();
    config.registerClasses(PublisherResource.class);
    config.packages("eu.arrowhead.ArrowheadPublisher.common");

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
    config.registerClasses(PublisherResource.class);
    config.packages("eu.arrowhead.ArrowheadPublisher.common");

    String keystorePath = prop.getProperty("keystore");
    String keystorePass = prop.getProperty("keystorepass");
    String keyPass = prop.getProperty("keypass");
    String truststorePath = prop.getProperty("truststore");
    String truststorePass = prop.getProperty("truststorepass");

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
    if (server != null) {
      server.shutdownNow();
    }
    System.out.println("Arrowhead Publisher Server stopped.");
    System.exit(0);
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
