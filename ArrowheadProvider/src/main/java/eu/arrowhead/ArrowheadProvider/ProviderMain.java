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
import eu.arrowhead.ArrowheadProvider.common.model.IntraCloudAuthEntry;
import eu.arrowhead.ArrowheadProvider.common.model.OrchestrationStore;
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
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceConfigurationError;
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
  private static boolean NEED_AUTH;
  private static boolean NEED_ORCH;
  private static boolean FROM_FILE;
  private static String BASE_URI;
  private static String SR_BASE_URI;
  private static HttpServer server;
  private static String PROVIDER_PUBLIC_KEY;
  private static TypeSafeProperties prop;

  //JSON payloads
  private static ServiceRegistryEntry srEntry;
  private static IntraCloudAuthEntry authEntry;
  private static List<OrchestrationStore> storeEntry = new ArrayList<>();

  public static void main(String[] args) throws IOException {
    System.out.println("Working directory: " + System.getProperty("user.dir"));

    String address = getProp().getProperty("address", "0.0.0.0");
    int insecurePort = getProp().getIntProperty("insecure_port", 8460);
    int securePort = getProp().getIntProperty("secure_port", 8461);

    String srAddress = getProp().getProperty("sr_address", "0.0.0.0");
    int srInsecurePort = getProp().getIntProperty("sr_insecure_port", 8442);
    int srSecurePort = getProp().getIntProperty("sr_secure_port", 8443);

    boolean daemon = false;
    List<String> alwaysMandatoryProperties = Arrays.asList("service_name", "service_uri", "interfaces", "metadata", "insecure_system_name");
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
          List<String> allMandatoryProperties = new ArrayList<>(alwaysMandatoryProperties);
          allMandatoryProperties.addAll(
              Arrays.asList("keystore", "keystorepass", "keypass", "truststore", "truststorepass", "authorization_cert", "secure_system_name"));
          Utility.checkProperties(getProp().stringPropertyNames(), allMandatoryProperties);
          BASE_URI = Utility.getUri(address, securePort, null, true);
          SR_BASE_URI = Utility.getUri(srAddress, srSecurePort, "serviceregistry", true);
          server = startSecureServer();
          IS_SECURE = true;
          break;
        case "-ff":
          FROM_FILE = true;
          break;
        case "-auth":
          NEED_AUTH = true;
          break;
        case "-orch":
          NEED_ORCH = true;
          break;
      }
    }
    if (IS_SECURE && (NEED_AUTH || NEED_ORCH)) {
      throw new ServiceConfigurationError("The Authorization/Store registration features can only be used in insecure mode!");
    }
    if (server == null) {
      Utility.checkProperties(getProp().stringPropertyNames(), alwaysMandatoryProperties);
      BASE_URI = Utility.getUri(address, insecurePort, null, false);
      SR_BASE_URI = Utility.getUri(srAddress, srInsecurePort, "serviceregistry", false);
      server = startServer();
    }

    loadAndCompilePayloads(FROM_FILE);
    registerToServiceRegistry();
    if (NEED_AUTH) {
      registerToAuthorization();
    }
    if (NEED_ORCH) {
      registerToStore();
    }

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
      while (!input.equals("stop")) {
        input = br.readLine();
      }
      br.close();
      shutdown();
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

  private static void shutdown() {
    unregisterFromServiceRegistry();
    if (server != null) {
      server.shutdownNow();
    }
    System.out.println("Temperature Provider Server stopped.");
    System.exit(0);
  }

  private static void loadAndCompilePayloads(boolean fromFile) {
    if (fromFile) {
      String srPath = getProp().getProperty("sr_entry");
      srEntry = Utility.fromJson(Utility.loadJsonFromFile(srPath), ServiceRegistryEntry.class);
      if (NEED_AUTH) {
        String authPath = getProp().getProperty("auth_entry");
        authEntry = Utility.fromJson(Utility.loadJsonFromFile(authPath), IntraCloudAuthEntry.class);
      }
      if (NEED_ORCH) {
        String storePath = getProp().getProperty("store_entry");
        storeEntry = Arrays.asList(Utility.fromJson(Utility.loadJsonFromFile(storePath), OrchestrationStore[].class));
      }
    } else {
      String serviceDef = getProp().getProperty("service_name");
      String serviceUri = getProp().getProperty("temperature");
      String interfaceList = getProp().getProperty("interfaces");
      List<String> interfaces = new ArrayList<>();
      if (interfaceList != null && !interfaceList.isEmpty()) {
        interfaces.addAll(Arrays.asList(interfaceList.replaceAll("\\s+", "").split(",")));
      }
      Map<String, String> metadata = new HashMap<>();
      String metadataString = getProp().getProperty("metadata");
      if (metadataString != null && !metadataString.isEmpty()) {
        String[] parts = metadataString.split(",");
        for (String part : parts) {
          String[] pair = part.split("-");
          metadata.put(pair[0], pair[1]);
        }
      }
      ArrowheadService service = new ArrowheadService(serviceDef, interfaces, metadata);

      URI baseUri;
      try {
        baseUri = new URI(BASE_URI);
      } catch (URISyntaxException e) {
        throw new AssertionError("Parsing the BASE_URI resulted in an error.", e);
      }
      ArrowheadSystem provider;
      if (IS_SECURE) {
        if (!metadata.containsKey("security")) {
          metadata.put("security", "token");
        }
        String secProviderName = getProp().getProperty("secure_system_name");
        provider = new ArrowheadSystem(secProviderName, baseUri.getHost(), baseUri.getPort(), PROVIDER_PUBLIC_KEY);
      } else {
        String insecProviderName = getProp().getProperty("insecure_system_name");
        provider = new ArrowheadSystem(insecProviderName, baseUri.getHost(), baseUri.getPort(), null);
      }

      ArrowheadSystem consumer = null;
      if (NEED_AUTH || NEED_ORCH) {
        String consumerName = getProp().getProperty("consumer_name");
        String consumerAddress = getProp().getProperty("consumer_address");
        String consumerPK = getProp().getProperty("consumer_public_key");
        consumer = new ArrowheadSystem(consumerName, consumerAddress, 0, consumerPK);
      }

      srEntry = new ServiceRegistryEntry(service, provider, serviceUri);
      if (NEED_AUTH) {
        authEntry = new IntraCloudAuthEntry(consumer, Collections.singletonList(provider), Collections.singletonList(service));
      }
      if (NEED_ORCH) {
        storeEntry = Collections.singletonList(new OrchestrationStore(service, consumer, provider, null, 0, false));
      }
    }
    System.out.println("Service Registry Entry: " + Utility.toPrettyJson(null, srEntry));
    System.out.println("IntraCloud Auth Entry: " + Utility.toPrettyJson(null, authEntry));
    System.out.println("Orchestration Store Entry: " + Utility.toPrettyJson(null, storeEntry));
  }

  private static void registerToServiceRegistry() {
    // create the URI for the request
    String registerUri = UriBuilder.fromPath(SR_BASE_URI).path("register").toString();
    try {
      Utility.sendRequest(registerUri, "POST", srEntry);
    } catch (ArrowheadException e) {
      if (e.getExceptionType() == ExceptionType.DUPLICATE_ENTRY) {
        System.out.println("Received DuplicateEntryException from SR, sending delete request and then registering again.");
        unregisterFromServiceRegistry();
        Utility.sendRequest(registerUri, "POST", srEntry);
      } else {
        throw e;
      }
    }
    System.out.println("Registering service is successful!");
  }

  private static void unregisterFromServiceRegistry() {
    String removeUri = UriBuilder.fromPath(SR_BASE_URI).path("remove").toString();
    Utility.sendRequest(removeUri, "PUT", srEntry);
    System.out.println("Removing service is successful!");
  }

  private static void registerToAuthorization() {
    String authAddress = getProp().getProperty("auth_address", "0.0.0.0");
    int authPort = getProp().getIntProperty("auth_port", 8444);
    String authUri = Utility.getUri(authAddress, authPort, "authorization/mgmt/intracloud", false);
    Utility.sendRequest(authUri, "POST", authEntry);
    System.out.println("Authorization registration is successful!");
  }

  private static void registerToStore() {
    String orchAddress = getProp().getProperty("orch_address", "0.0.0.0");
    int orchPort = getProp().getIntProperty("orch_port", 8440);
    String orchUri = Utility.getUri(orchAddress, orchPort, "orchestrator/mgmt/store", false);
    Utility.sendRequest(orchUri, "POST", storeEntry);
    System.out.println("Store registration is successful!");
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
