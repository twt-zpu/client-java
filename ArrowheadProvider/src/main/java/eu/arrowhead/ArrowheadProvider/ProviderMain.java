package eu.arrowhead.ArrowheadProvider;

import eu.arrowhead.ArrowheadProvider.common.Utility;
import eu.arrowhead.ArrowheadProvider.common.model.ArrowheadService;
import eu.arrowhead.ArrowheadProvider.common.model.ArrowheadSystem;
import eu.arrowhead.ArrowheadProvider.common.model.ServiceMetadata;
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
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
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

  public static void main(String[] args) throws IOException {

    if (args.length > 0) {
      switch (args[0]) {
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
          throw new AssertionError("Unknown server mode: " + args[0]);
      }
    } else {
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
    config.registerClasses(TemperatureResource.class);

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
    config.packages("eu.arrowhead.ArrowheadProvider.common.security");

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
    System.out.println("Server PublicKey encoded: " + Arrays.toString(serverCert.getPublicKey().getEncoded()));
    System.out.println("Server PublicKey Base64: " + Base64.getEncoder().encodeToString(serverCert.getPublicKey().getEncoded()));
    String serverCN = Utility.getCertCNFromSubject(serverCert.getSubjectDN().getName());
    System.out.println("Certificate of the secure server: " + serverCN);
    config.property("server_common_name", serverCN);

    String authKeystorePath = getProp().getProperty("ssl.auth_keystore");
    String authKeystorePass = getProp().getProperty("ssl.auth_keystorepass");
    KeyStore authKeyStore = Utility.loadKeyStore(authKeystorePath, authKeystorePass);
    X509Certificate cert = Utility.getFirstCertFromKeyStore(authKeyStore);
    authorizationKey = cert.getPublicKey();

    System.out.println("Authorization PublicKey Base64: " + Base64.getEncoder().encodeToString(authorizationKey.getEncoded()));

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
    String registerUri = UriBuilder.fromPath(SR_BASE_URI).path("registration").toString();

    // create the ServiceMetadata list object
    ServiceMetadata unit = new ServiceMetadata("unit", "celsius");
    List<ServiceMetadata> metadataList = new ArrayList<>();
    metadataList.add(unit);

    // create the ArrowheadService object
    ArrowheadService service = new ArrowheadService("Temperature", "IndoorTemperature", Collections.singletonList("json"), metadataList);

    // objects specific to insecure mode
    if (server != null) {
      URI baseUri;
      try {
        baseUri = new URI(BASE_URI);
      } catch (URISyntaxException e) {
        throw new RuntimeException("Parsing the BASE_URI resulted in an error.", e);
      }
      // create the ArrowheadSystem object
      ArrowheadSystem provider = new ArrowheadSystem("TemperatureSensors", "InsecureTemperatureSensor", baseUri.getHost(), baseUri.getPort(), "TBD");
      // create the final request payload
      ServiceRegistryEntry entry = new ServiceRegistryEntry(service, provider, "/temperature");
      Utility.sendRequest(registerUri, "POST", entry);
      System.out.println("Registering insecure service is successful!");
      entries.add(entry);
    }

    // objects specific to secure mode
    if (secureServer != null) {
      // adding metadata indicating the security choice of the provider
      ServiceMetadata security = new ServiceMetadata("security", "token");
      metadataList.add(security);

      URI baseUri;
      try {
        baseUri = new URI(BASE_URI_SECURED);
      } catch (URISyntaxException e) {
        throw new RuntimeException("Parsing the BASE_URI_SECURED resulted in an error.", e);
      }
      // create the ArrowheadSystem object
      ArrowheadSystem provider = new ArrowheadSystem("TemperatureSensors", "SecureTemperatureSensor", baseUri.getHost(), baseUri.getPort(), "TBD");
      // create the final request payload
      ServiceRegistryEntry entry = new ServiceRegistryEntry(service, provider, "/temperature");
      Utility.sendRequest(registerUri, "POST", entry);
      System.out.println("Registering secure service is successful!");
      entries.add(entry);
    }

    return entries;
  }

  private static void unregisterFromServiceRegistry(List<ServiceRegistryEntry> registeredEntries) {
    // create the URI for the request
    String removeUri = UriBuilder.fromPath(SR_BASE_URI).path("removing").toString();
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
