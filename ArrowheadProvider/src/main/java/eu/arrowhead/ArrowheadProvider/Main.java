package eu.arrowhead.ArrowheadProvider;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import eu.arrowhead.ArrowheadProvider.common.Utility;
import eu.arrowhead.ArrowheadProvider.common.model.ArrowheadSystem;
import eu.arrowhead.ArrowheadProvider.common.model.ServiceMetadata;
import eu.arrowhead.ArrowheadProvider.common.model.ServiceRegistryEntry;

public class Main {

	private static Properties prop;
	public static final String SR_BASE_URI = getProp().getProperty("sr_base_uri", "http://arrowhead.tmit.bme.hu:8444/serviceregistry");
	public static final String ADDRESS = getProp().getProperty("address", "0.0.0.0");
	public static final String PORT = getProp().getProperty("port", "8452");
	public static final String TSIG_KEY = getProp().getProperty("tsig_key", "RM/jKKEPYB83peT0DQnYGg==");
	
	public static void main(String[] args) throws Exception {
		String URI = "http://" + ADDRESS + ":" + PORT;
		HttpServer server = startServer(URI);
		registerToServiceRegistry();
		
		System.out.println("Press enter to shutdown the Temperature Provider Server...");
		System.in.read();
		
		if(server != null){
			unregisterFromServiceRegistry();
        	server.shutdownNow();
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
	
	public static void registerToServiceRegistry() throws Exception{
		//prepare AS
		ArrowheadSystem provider = new ArrowheadSystem("TemperatureSensors", "TemperatureSensor14", ADDRESS, PORT, "TBD");
		
		//prepare SM
		ServiceMetadata smd = new ServiceMetadata("unit", "celsius");
		List<ServiceMetadata> metadataList = new ArrayList<>(Arrays.asList(smd));
		
		//prepare ServiceRegistryEntry - the final payload
		ServiceRegistryEntry entry = new ServiceRegistryEntry(provider, "/temperature", metadataList, TSIG_KEY, "1.0");
		
		//prepare URI
		UriBuilder ub = UriBuilder.fromPath(SR_BASE_URI).path("Temperature").path("IndoorTemperature").path("json");
		
		//send request
		Response response = Utility.sendRequest(ub.toString(), "POST", entry);
		if(response.getStatus() != 204){
			throw new Exception("Service registering FAILED!");
		}
		else{
			System.out.println("Service registering successful!");
		}
	}
	
	public static void unregisterFromServiceRegistry() throws Exception{
		//prepare AS
		ArrowheadSystem provider = new ArrowheadSystem("TemperatureSensors", "TemperatureSensor14", ADDRESS, PORT, "TBD");
		
		//prepare SM
		ServiceMetadata smd = new ServiceMetadata("unit", "celsius");
		List<ServiceMetadata> metadataList = new ArrayList<>(Arrays.asList(smd));
		
		//prepare ServiceRegistryEntry - the final payload
		ServiceRegistryEntry entry = new ServiceRegistryEntry(provider, "temperature", metadataList, TSIG_KEY, "1.0");
		
		//prepare URI
		UriBuilder ub = UriBuilder.fromPath(SR_BASE_URI).path("Temperature").path("IndoorTemperature").path("json");
		
		//send request
		Response response = Utility.sendRequest(ub.toString(), "PUT", entry);
		if(response.getStatus() != 204){
			throw new Exception("Removing service FAILED!");
		}
		else{
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
