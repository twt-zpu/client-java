package eu.arrowhead.client.provider;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ws.rs.core.UriBuilder;

import eu.arrowhead.client.common.ArrowheadClientMain;
import eu.arrowhead.client.common.Utility;
import eu.arrowhead.client.common.exception.ArrowheadException;
import eu.arrowhead.client.common.exception.ExceptionType;
import eu.arrowhead.client.common.misc.ClientType;
import eu.arrowhead.client.common.model.IntraCloudAuthEntry;
import eu.arrowhead.client.common.model.OrchestrationStore;
import eu.arrowhead.client.common.model.ServiceRegistryEntry;

public class FeldbusProviderMain extends ArrowheadClientMain {
	static String customResponsePayload;
	static PublicKey authorizationKey;
	static PrivateKey privateKey;

	private static List<String> argsList = new ArrayList<String>();
	private static boolean NEED_AUTH = false;
	private static boolean NEED_ORCH = false;
	private static String SR_BASE_URI;

	//JSON payloads
	private static List<ServiceRegistryEntry> srEntry = new ArrayList<>();
	private static IntraCloudAuthEntry authEntry;
	private static List<OrchestrationStore> storeEntry = new ArrayList<>();
	
	public static void main(String[] args) {
		FeldbusProviderMain provider = new FeldbusProviderMain(args);
		provider.startProvider();
	}
	
	public FeldbusProviderMain(String[] args){
		for (String arg: args){
			argsList.add(arg);
		}
	}
	
	public void startProvider() {
		Set<Class<?>> classes = new HashSet<>(Arrays.asList(FeldbusCouplerLResource.class, RestResource.class));
		String[] packages = {"eu.arrowhead.client.common"};
		String[] args = new String[argsList.size()];
		int index = 0;
		for (String arg : argsList){
			args[index++] = arg;
		}
		init(ClientType.PROVIDER, args, classes, packages);
		//Compile the base of the Service Registry URL
		getServiceRegistryUrl();
		//Compile the request payload
		loadAndCompilePayloads();
		//Send the registration to the Service Registry
		unregisterFromServiceRegistry();
		registerToServiceRegistry();
		if (NEED_AUTH) {
			registerToAuthorization();
		}
		if (NEED_ORCH) {
			registerToStore();
		}
		//Listen for a stop command
		listenForInput();
	
	}
	
	private void getServiceRegistryUrl() {
		String srAddress = props.getProperty("sr_address", "0.0.0.0");
		int srPort = props.getIntProperty("sr_insecure_port", 8442);
		SR_BASE_URI = Utility.getUri(srAddress, srPort, "serviceregistry", false, false);
	}

	private void loadAndCompilePayloads() {
		//Compile the ArrowheadService (providedService)
		String srPath = props.getProperty("sr_entry");
		srEntry = Arrays.asList(Utility.fromJson(Utility.loadJsonFromFile(srPath), ServiceRegistryEntry[].class));
		if (NEED_AUTH) {
			String authPath = props.getProperty("auth_entry");
			authEntry = Utility.fromJson(Utility.loadJsonFromFile(authPath), IntraCloudAuthEntry.class);
		}
		if (NEED_ORCH) {
			String storePath = props.getProperty("store_entry");
			storeEntry = Arrays.asList(Utility.fromJson(Utility.loadJsonFromFile(storePath), OrchestrationStore[].class));
		}
		
		System.out.println("Service Registry Entry: " + Utility.toPrettyJson(null, srEntry));
		System.out.println("IntraCloud Auth Entry: " + Utility.toPrettyJson(null, authEntry));
		System.out.println("Orchestration Store Entry: " + Utility.toPrettyJson(null, storeEntry));
	}
	
	private static void registerToServiceRegistry() {
		// create the URI for the request
		String registerUri = UriBuilder.fromPath(SR_BASE_URI).path("register").toString();
		try {
			Utility.sendRequest(registerUri, "POST", srEntry.get(0));
			Utility.sendRequest(registerUri, "POST", srEntry.get(1));
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
		Utility.sendRequest(removeUri, "PUT", srEntry.get(0));
		Utility.sendRequest(removeUri, "PUT", srEntry.get(1));
		System.out.println("Removing service is successful!");
	}

	private void registerToAuthorization() {
		String authAddress = props.getProperty("auth_address", "0.0.0.0");
		int authPort = isSecure ? props.getIntProperty("auth_secure_port", 8445) : props.getIntProperty("auth_insecure_port", 8444);
		String authUri = Utility.getUri(authAddress, authPort, "authorization/mgmt/intracloud", isSecure, false);
		try {
			Utility.sendRequest(authUri, "POST", authEntry);
			System.out.println("Authorization registration is successful!");
		} catch (ArrowheadException e) {
			e.printStackTrace();
			System.out.println("Authorization registration failed!");
		}

	}

	private void registerToStore() {
		String orchAddress = props.getProperty("orch_address", "0.0.0.0");
		int orchPort = props.getIntProperty("orch_port", 8440);
		String orchUri = Utility.getUri(orchAddress, orchPort, "orchestrator/mgmt/store", false, false);
		try {
			Utility.sendRequest(orchUri, "POST", storeEntry);
			System.out.println("Store registration is successful!");
		} catch (ArrowheadException e) {
			e.printStackTrace();
			System.out.println("Store registration failed!");
		}
	}
	
}
