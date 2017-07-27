package eu.arrowhead.ArrowheadProvider;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Properties;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.ssl.SSLContextConfigurator;
import org.glassfish.grizzly.ssl.SSLEngineConfigurator;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import eu.arrowhead.ArrowheadProvider.common.Utility;
import eu.arrowhead.ArrowheadProvider.common.model.ArrowheadSystem;
import eu.arrowhead.ArrowheadProvider.common.model.ServiceMetadata;
import eu.arrowhead.ArrowheadProvider.common.model.ServiceRegistryEntry;
import eu.arrowhead.ArrowheadProvider.common.ssl.AuthenticationException;
import eu.arrowhead.ArrowheadProvider.common.ssl.SecurityUtils;

public class Main {

	private static Properties prop;

	public static final String BASE_URI_SECURED = getProp().getProperty("base_uri_secured", "https://0.0.0.0:8452/");
	public static final String BASE_URI = getProp().getProperty("base_uri", "http://0.0.0.0:8453/");

	public static HttpServer server = null;
	public static HttpServer secureServer = null;

	// Service Registry Data
	public static final String SR_BASE_URI = getProp().getProperty("sr_base_uri",
			"http://arrowhead.tmit.bme.hu:8444/serviceregistry");
	public static final String TSIG_KEY = getProp().getProperty("tsig_key", "RM/jKKEPYB83peT0DQnYGg==");

	// security-related variable
	public static X509Certificate serverCert = null;

	// address and port to be registered into the Service Registry, same as in
	// BASE_URI_*
	public static final String ADDRESS = getProp().getProperty("address", "0.0.0.0");
	public static final String PORT_SECURE = getProp().getProperty("port_secure", "8452");
	public static final String PORT_UNSECURE = getProp().getProperty("port_unsecure", "8453");
	public static PrivateKey privateKey = null;
	public static PublicKey authorizationKey = null;
	
	

	public static void main(String[] args) throws Exception {
		try {
			String authKeystorePath = getProp().getProperty("ssl.auth_keystore");
			String authKeystorePass = getProp().getProperty("ssl.auth_keystorepass");
			KeyStore authKeyStore = SecurityUtils.loadKeyStore(authKeystorePath,
					authKeystorePass);
			X509Certificate cert = SecurityUtils.getFirstCertFromKeyStore(authKeyStore);
			authorizationKey = cert.getPublicKey();

		} catch (Exception ex) {
			ex.printStackTrace();
		}

		try {
			KeyStore keyStore = Utility.loadKeyStore(Utility.getProp().getProperty("ssl.keystore"),
					Utility.getProp().getProperty("ssl.keystorepass"));
			privateKey = Utility.getPrivateKey(keyStore, Utility.getProp().getProperty("ssl.keystorepass"));
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		
		int mode = 0;
		for (int i = 0; i < args.length; ++i) {
			if (args[i].equals("-m")) {
				++i;
				switch (args[i]) {
				case "secure":
					mode = 1;
					break;
				case "both":
					mode = 2;
					break;
				} 
			}
		}
		
		switch (mode) {
		case 0:
			System.out.println("Starting unsecure server...");
			server = startServer(BASE_URI);
			registerToServiceRegistry("UnsecureTemperatureSensor", PORT_UNSECURE);
			break;
		case 1:
			System.out.println("Starting secure server...");
			secureServer = startSecureServer(BASE_URI_SECURED);
			registerToServiceRegistry("SecureTemperatureSensor", PORT_SECURE);
			break;
		case 2:
			System.out.println("Starting secure and unsecure servers...");
			server = startServer(BASE_URI);
			secureServer = startSecureServer(BASE_URI_SECURED);
			registerToServiceRegistry("UnsecureTemperatureSensor", PORT_UNSECURE);
			registerToServiceRegistry("SecureTemperatureSensor", PORT_SECURE);
			
			break;
		}

		System.out.println("Press enter to shutdown the Temperature Provider Server...\n\n");
		System.in.read();

		
		if (server != null) {
			unregisterFromServiceRegistry(PORT_UNSECURE);
			server.shutdownNow();
		}
		
		if (secureServer != null) {
			unregisterFromServiceRegistry(PORT_SECURE);
			secureServer.shutdownNow();
		}

		System.out.println("Temperature Provider Server stopped.");
	}

	public static HttpServer startServer(String URI) throws IOException {
		URI uri = UriBuilder.fromUri(URI).build();

		final ResourceConfig config = new ResourceConfig();
		config.registerClasses(TemperatureResource.class);

		final HttpServer server = GrizzlyHttpServerFactory.createHttpServer(uri, config);
		server.getServerConfiguration().setAllowPayloadForUndefinedHttpMethods(true);
		server.start();
		return server;
	}

	public static HttpServer startSecureServer(String URI) throws IOException {
		URI uri = UriBuilder.fromUri(URI).build();

		final ResourceConfig config = new ResourceConfig();
		config.registerClasses(TemperatureResource.class);
		config.packages("eu.arrowhead.common.ssl");

		SSLContextConfigurator sslCon = new SSLContextConfigurator();

		String keystorePath = getProp().getProperty("ssl.keystore");
		String keystorePass = getProp().getProperty("ssl.keystorepass");
		String keyPass = getProp().getProperty("ssl.keypass");
		String truststorePath = getProp().getProperty("ssl.truststore");
		String truststorePass = getProp().getProperty("ssl.truststorepass");

		sslCon.setKeyStoreFile(keystorePath);
		sslCon.setKeyStorePass(keystorePass);
		sslCon.setKeyPass(keyPass);
		sslCon.setTrustStoreFile(truststorePath);
		sslCon.setTrustStorePass(truststorePass);

		try {
			KeyStore keyStore = SecurityUtils.loadKeyStore(keystorePath, keystorePass);
			serverCert = SecurityUtils.getFirstCertFromKeyStore(keyStore);
			System.out.println("Server PublicKey encoded: " + serverCert.getPublicKey().getEncoded());
			System.out.println("Server PublicKey Base64: "
					+ Base64.getEncoder().encodeToString(serverCert.getPublicKey().getEncoded()));
		} catch (Exception ex) {
			throw new AuthenticationException(ex.getMessage());
		}
		String serverCN = SecurityUtils.getCertCNFromSubject(serverCert.getSubjectDN().getName());
		System.out.println("Certificate of the secure server: " + serverCN);
		config.property("server_common_name", serverCN);

		final HttpServer server = GrizzlyHttpServerFactory.createHttpServer(uri, config, true,
				new SSLEngineConfigurator(sslCon).setClientMode(false).setNeedClientAuth(true));
		server.getServerConfiguration().setAllowPayloadForUndefinedHttpMethods(true);
		server.start();
		return server;
	}

	public static void registerToServiceRegistry(String name, String port) throws Exception {
		// prepare AS
		ArrowheadSystem provider = new ArrowheadSystem("TemperatureSensors", name, ADDRESS, port,
				"TBD");

		// prepare SM
		ServiceMetadata unit = new ServiceMetadata("unit", "celsius");
		List<ServiceMetadata> metadataList = new ArrayList<ServiceMetadata>();
		metadataList.add(unit);
		if (port.equals(PORT_SECURE)) {
			ServiceMetadata security = new ServiceMetadata("security", "token");
			metadataList.add(security);
		}
		
		// prepare ServiceRegistryEntry - the final payload
		ServiceRegistryEntry entry = new ServiceRegistryEntry(provider, "/temperature", metadataList, TSIG_KEY, "1.0");

		// prepare URI
		UriBuilder ub = UriBuilder.fromPath(SR_BASE_URI).path("Temperature").path("IndoorTemperature").path("json");

		// send request
		Response response = Utility.sendRequest(ub.toString(), "POST", entry);
		if (response.getStatus() != 204) {
			throw new Exception("Service registering FAILED!");
		} else {
			System.out.println("Service registering successful!");
		}
	}

	public static void unregisterFromServiceRegistry(String port) throws Exception {
		// prepare AS
		ArrowheadSystem provider = new ArrowheadSystem("TemperatureSensors", "TemperatureSensor14", ADDRESS, port,
				"TBD");

		// prepare SM
		ServiceMetadata smd = new ServiceMetadata("unit", "celsius");
		List<ServiceMetadata> metadataList = new ArrayList<>(Arrays.asList(smd));

		// prepare ServiceRegistryEntry - the final payload
		ServiceRegistryEntry entry = new ServiceRegistryEntry(provider, "temperature", metadataList, TSIG_KEY, "1.0");

		// prepare URI
		UriBuilder ub = UriBuilder.fromPath(SR_BASE_URI).path("Temperature").path("IndoorTemperature").path("json");

		// send request
		Response response = Utility.sendRequest(ub.toString(), "PUT", entry);
		if (response.getStatus() != 204) {
			throw new Exception("Removing service FAILED!");
		} else {
			System.out.println("Removing service successful!");
		}
	}

	public synchronized static Properties getProp() {
		try {
			if (prop == null) {
				prop = new Properties();
				File file = new File("config" + File.separator + "app.properties");
				FileInputStream inputStream = new FileInputStream(file);
				if (inputStream != null) {
					prop.load(inputStream);
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		return prop;
	}

}
