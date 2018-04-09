/*
 * Copyright (c) 2018 AITIA International Inc.
 *
 * This work is part of the Productive 4.0 innovation project, which receives grants from the
 * European Commissions H2020 research and innovation programme, ECSEL Joint Undertaking
 * (project no. 737459), the free state of Saxony, the German Federal Ministry of Education and
 * national funding authorities from involved countries.
 */

package eu.arrowhead.ArrowheadProvider;

import eu.arrowhead.ArrowheadProvider.common.TypeSafeProperties;
import eu.arrowhead.ArrowheadProvider.common.exception.ArrowheadException;
import eu.arrowhead.ArrowheadProvider.common.exception.AuthException;
import eu.arrowhead.ArrowheadProvider.common.exception.ExceptionType;
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
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.net.ssl.SSLContext;
import javax.ws.rs.core.UriBuilder;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.ssl.SSLContextConfigurator;
import org.glassfish.grizzly.ssl.SSLEngineConfigurator;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

public class ProviderMain {

  public static boolean DEBUG_MODE;

  static PublicKey authorizationKey;
  static PrivateKey privateKey;

  private static boolean IS_SECURE;
  private static String BASE_URI;
  private static String SR_BASE_URI;
  private static HttpServer server;
  private static String PROVIDER_PUBLIC_KEY;
  private static TypeSafeProperties prop;

  public static void main(String[] args) throws IOException {
    System.out.println("Working directory: " + System.getProperty("user.dir"));

    String address = getProp().getProperty("address", "0.0.0.0");
    int insecurePort = getProp().getIntProperty("insecure_port", 8460);
    int securePort = getProp().getIntProperty("secure_port", 8461);

    String srAddress = getProp().getProperty("sr_address", "0.0.0.0");
    int srInsecurePort = getProp().getIntProperty("sr_insecure_port", 8442);
    int srSecurePort = getProp().getIntProperty("sr_secure_port", 8443);

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
          List<String> secureMandatoryProperties = Arrays
              .asList("keystore", "keystorepass", "keypass", "truststore", "truststorepass", "authorization_cert");
          Utility.checkProperties(getProp().stringPropertyNames(), secureMandatoryProperties);
          BASE_URI = Utility.getUri(address, securePort, null, true);
          SR_BASE_URI = Utility.getUri(srAddress, srSecurePort, "serviceregistry", true);
          server = startSecureServer();
          IS_SECURE = true;
      }
    }
    if (server == null) {
      BASE_URI = Utility.getUri(address, insecurePort, null, false);
      SR_BASE_URI = Utility.getUri(srAddress, srInsecurePort, "serviceregistry", false);
      server = startServer();
    }

    ServiceRegistryEntry registeredEntry = registerToServiceRegistry();

    if (daemon) {
      System.out.println("In daemon mode, process will terminate for TERM signal...");
      Runtime.getRuntime().addShutdownHook(new Thread(() -> {
        System.out.println("Received TERM signal, shutting down...");
        shutdown(registeredEntry);
      }));
    } else {
      System.out.println("Type \"stop\" to shutdown ArrowheadProvider Server...");
      BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
      String input = "";
      while (!input.equals("stop")) {
        input = br.readLine();
      }
      br.close();
      shutdown(registeredEntry);
    }
  }

  private static HttpServer startServer() throws IOException {
    URI uri = UriBuilder.fromUri(BASE_URI).build();
    final ResourceConfig config = new ResourceConfig();
    config.registerClasses(TemperatureResource.class);
    config.packages("eu.arrowhead.ArrowheadProvider.common");

    final HttpServer server = GrizzlyHttpServerFactory.createHttpServer(uri, config);
    server.getServerConfiguration().setAllowPayloadForUndefinedHttpMethods(true);
    server.start();
    System.out.println("Insecure server launched...");
    return server;
  }

  private static HttpServer startSecureServer() throws IOException {
    URI uri = UriBuilder.fromUri(BASE_URI).build();
    final ResourceConfig config = new ResourceConfig();
    config.registerClasses(TemperatureResource.class);
    config.packages("eu.arrowhead.ArrowheadProvider.common");

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

  private static void shutdown(ServiceRegistryEntry registeredEntry) {
    unregisterFromServiceRegistry(registeredEntry);
    if (server != null) {
      server.shutdownNow();
    }
    System.out.println("Temperature Provider Server stopped.");
    System.exit(0);
  }

  private static ServiceRegistryEntry registerToServiceRegistry() {
    // create the URI for the request
    String registerUri = UriBuilder.fromPath(SR_BASE_URI).path("register").toString();

    // create the metadata HashMap for the service
    Map<String, String> metadata = new HashMap<>();
    metadata.put("unit", "celsius");

    // create the ArrowheadService object
    ArrowheadService service = new ArrowheadService("IndoorTemperature", Collections.singletonList("json"), metadata);

    URI baseUri;
    try {
      baseUri = new URI(BASE_URI);
    } catch (URISyntaxException e) {
      //Should never be called, since Utility.getUri() also handles this exception
      throw new AssertionError("Parsing the BASE_URI resulted in an error.", e);
    }

    ArrowheadSystem provider;
    if (IS_SECURE) {
      metadata.put("security", "token");
      provider = new ArrowheadSystem("SecureTemperatureSensor", baseUri.getHost(), baseUri.getPort(), PROVIDER_PUBLIC_KEY);
    } else {
      provider = new ArrowheadSystem("InsecureTemperatureSensor", baseUri.getHost(), baseUri.getPort(), null);
    }

    ServiceRegistryEntry entry = new ServiceRegistryEntry(service, provider, "temperature");
    try {
      Utility.sendRequest(registerUri, "POST", entry);
    } catch (ArrowheadException e) {
      if (e.getExceptionType() == ExceptionType.DUPLICATE_ENTRY) {
        System.out.println("Received DuplicateEntryException from SR, sending delete request and then registering again.");
        unregisterFromServiceRegistry(entry);
        Utility.sendRequest(registerUri, "POST", entry);
      } else {
        throw e;
      }
    }
    System.out.println("Registering service is successful!");

    return entry;
  }

  private static void unregisterFromServiceRegistry(ServiceRegistryEntry registeredEntry) {
    String removeUri = UriBuilder.fromPath(SR_BASE_URI).path("remove").toString();

    Utility.sendRequest(removeUri, "PUT", registeredEntry);

    System.out.println("Removing service is successful!");
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
