package eu.arrowhead.ArrowheadConsumerClient.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServiceRequestForm {

	private ArrowheadSystem requesterSystem;
	private ArrowheadService requestedService;
	private String requestedQoS;
	private Map<String, Boolean> orchestrationFlags = new HashMap<String, Boolean>();
	private List<ArrowheadCloud> preferredClouds = new ArrayList<ArrowheadCloud>();
	private List<ArrowheadSystem> preferredProviders = new ArrayList<ArrowheadSystem>();
	
	public ServiceRequestForm (){
		this.orchestrationFlags.put("triggerInterCloud", false);
		this.orchestrationFlags.put("externalServiceRequest", false);
		this.orchestrationFlags.put("enableInterCloud", false);
		this.orchestrationFlags.put("metadataSearch", false);
		this.orchestrationFlags.put("pingProviders", false);
		this.orchestrationFlags.put("overrideStore", false);
		this.orchestrationFlags.put("storeOnlyActive", false);
		this.orchestrationFlags.put("matchmaking", false);
		this.orchestrationFlags.put("onlyPreferred", false);
		this.orchestrationFlags.put("generateToken", false);
	}

	public ServiceRequestForm(ArrowheadSystem requesterSystem, ArrowheadService requestedService, 
			String requestedQoS, List<ArrowheadCloud> preferredClouds, 
			List<ArrowheadSystem> preferredProviders) {
		this.requesterSystem = requesterSystem;
		this.requestedService = requestedService;
		this.requestedQoS = requestedQoS;
		this.preferredClouds = preferredClouds;
		this.preferredProviders = preferredProviders;
		this.orchestrationFlags.put("triggerInterCloud", false);
		this.orchestrationFlags.put("externalServiceRequest", false);
		this.orchestrationFlags.put("enableInterCloud", false);
		this.orchestrationFlags.put("metadataSearch", false);
		this.orchestrationFlags.put("pingProviders", false);
		this.orchestrationFlags.put("overrideStore", false);
		this.orchestrationFlags.put("storeOnlyActive", false);
		this.orchestrationFlags.put("matchmaking", false);
		this.orchestrationFlags.put("onlyPreferred", false);
		this.orchestrationFlags.put("generateToken", false);
	}
	
	public ServiceRequestForm(ArrowheadSystem requesterSystem, ArrowheadService requestedService, 
			String requestedQoS, Map<String, Boolean> orchestrationFlags, 
			List<ArrowheadCloud> preferredClouds, List<ArrowheadSystem> preferredProviders) {
		this.requesterSystem = requesterSystem;
		this.requestedService = requestedService;
		this.requestedQoS = requestedQoS;
		this.orchestrationFlags = orchestrationFlags;
		this.preferredClouds = preferredClouds;
		this.preferredProviders = preferredProviders;
	}

	public ServiceRequestForm(ArrowheadSystem requesterSystem, ArrowheadService requestedService,
			Map<String, Boolean> orchestrationFlags) {
		this.requesterSystem = requesterSystem;
		this.requestedService = requestedService;
		this.orchestrationFlags = orchestrationFlags;
	}

	public ArrowheadSystem getRequesterSystem() {
		return requesterSystem;
	}

	public void setRequesterSystem(ArrowheadSystem requesterSystem) {
		this.requesterSystem = requesterSystem;
	}

	public ArrowheadService getRequestedService() {
		return requestedService;
	}

	public void setRequestedService(ArrowheadService requestedService) {
		this.requestedService = requestedService;
	}

	public String getRequestedQoS() {
		return requestedQoS;
	}

	public void setRequestedQoS(String requestedQoS) {
		this.requestedQoS = requestedQoS;
	}

	public Map<String, Boolean> getOrchestrationFlags() {
		return orchestrationFlags;
	}

	public void setOrchestrationFlags(Map<String, Boolean> orchestrationFlags) {
		this.orchestrationFlags = orchestrationFlags;
	}

	public List<ArrowheadCloud> getPreferredClouds() {
		return preferredClouds;
	}

	public void setPreferredClouds(List<ArrowheadCloud> preferredClouds) {
		this.preferredClouds = preferredClouds;
	}

	public List<ArrowheadSystem> getPreferredProviders() {
		return preferredProviders;
	}

	public void setPreferredProviders(List<ArrowheadSystem> preferredProviders) {
		this.preferredProviders = preferredProviders;
	}

	
}
