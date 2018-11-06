/*
 *  Copyright (c) 2018 AITIA International Inc.
 *
 *  This work is part of the Productive 4.0 innovation project, which receives grants from the
 *  European Commissions H2020 research and innovation programme, ECSEL Joint Undertaking
 *  (project no. 737459), the free state of Saxony, the German Federal Ministry of Education and
 *  national funding authorities from involved countries.
 */

package eu.arrowhead.common.model;

import eu.arrowhead.common.exception.BadPayloadException;

import java.util.*;

/**
 * This is what the Orchestrator Core System receives from Arrowhead Systems trying to request services.
 */
public class ServiceRequestForm {

  public enum Flags {
    TRIGGER_INTER_CLOUD("triggerInterCloud"),
    EXTERNAL_SERVICE_REQUEST("externalServiceRequest"),
    ENABLE_INTER_CLOUD("enableInterCloud"),
    METADATA_SEARCH("metadataSearch"),
    PING_PROVIDERS("pingProviders"),
    OVERRIDE_STORE("overrideStore"),
    MATCHMAKING("matchmaking"),
    ONLY_PREFERRED("onlyPreferred"),
    ENABLE_QOS("enableQoS"),
    ;

    private final String flag;

    Flags(final String flag) {
      this.flag = flag;
    }

    @Override
    public String toString() {
      return flag;
    }
  }

  public static class OrchestrationFlags extends HashMap<String, Boolean> {
    public OrchestrationFlags() {
      for (Flags flag : Flags.values()) {
        put(flag, false);
      }
    }

    public Boolean get(Flags flag) {
      return get(flag.toString());
    }

    public Boolean put(Flags flag, Boolean value) {
      return super.put(flag.toString(), value);
    }
  }

  private ArrowheadSystem requesterSystem;
  private ArrowheadCloud requesterCloud;
  private ArrowheadService requestedService;
  private OrchestrationFlags orchestrationFlags = new OrchestrationFlags();
  private List<PreferredProvider> preferredProviders = new ArrayList<>();
  private Map<String, String> requestedQoS = new HashMap<>();
  private Map<String, String> commands = new HashMap<>();

  public ServiceRequestForm() {
  }

  private ServiceRequestForm(Builder builder) {
    requesterSystem = builder.requesterSystem;
    requesterCloud = builder.requesterCloud;
    requestedService = builder.requestedService;
    orchestrationFlags = builder.orchestrationFlags;
    preferredProviders = builder.preferredProviders;
    requestedQoS = builder.requestedQoS;
    commands = builder.commands;
  }

  public ArrowheadSystem getRequesterSystem() {
    return requesterSystem;
  }

  public void setRequesterSystem(ArrowheadSystem requesterSystem) {
    this.requesterSystem = requesterSystem;
  }

  public ArrowheadCloud getRequesterCloud() {
    return requesterCloud;
  }

  public void setRequesterCloud(ArrowheadCloud requesterCloud) {
    this.requesterCloud = requesterCloud;
  }

  public ArrowheadService getRequestedService() {
    return requestedService;
  }

  public void setRequestedService(ArrowheadService requestedService) {
    this.requestedService = requestedService;
  }


  public Map<String, Boolean> getOrchestrationFlags() {
    return orchestrationFlags;
  }

  public void setOrchestrationFlags(OrchestrationFlags orchestrationFlags) {
    orchestrationFlags.forEach((k, v)->this.orchestrationFlags.put(k, v));
  }

  public void setOrchestrationFlags(Map<String, Boolean> orchestrationFlags) {
    orchestrationFlags.forEach((k, v)->this.orchestrationFlags.put(k, v));
  }

  public void setFlag(Flags flag, Boolean value) {
    this.orchestrationFlags.put(flag, value);
  }

  public void setFlag(String key, Boolean value) {
    this.orchestrationFlags.put(key, value);
  }

  public List<PreferredProvider> getPreferredProviders() {
    return preferredProviders;
  }

  public void setPreferredProviders(List<PreferredProvider> preferredProviders) {
    this.preferredProviders = preferredProviders;
  }

  public Map<String, String> getRequestedQoS() {
    return requestedQoS;
  }

  public void setRequestedQoS(Map<String, String> requestedQoS) {
    this.requestedQoS = requestedQoS;
  }

  public Map<String, String> getCommands() {
    return commands;
  }

  public void setCommands(Map<String, String> commands) {
    this.commands = commands;
  }

  public static class Builder {

    // Required parameters
    private final ArrowheadSystem requesterSystem;
    // Optional parameters
    private ArrowheadCloud requesterCloud;
    private ArrowheadService requestedService;

    private OrchestrationFlags orchestrationFlags = new OrchestrationFlags();
    private List<PreferredProvider> preferredProviders = new ArrayList<>();
    private Map<String, String> requestedQoS = new HashMap<>();
    private Map<String, String> commands = new HashMap<>();

    public Builder(ArrowheadSystem requesterSystem) {
      this.requesterSystem = requesterSystem;
    }

    public Builder requesterCloud(ArrowheadCloud cloud) {
      requesterCloud = cloud;
      return this;
    }

    public Builder requestedService(ArrowheadService service) {
      requestedService = service;
      return this;
    }

    public Builder flag(Flags flag, Boolean value) {
      this.orchestrationFlags.put(flag, value);
      return this;
    }

    public Builder flag(String key, Boolean value) {
      this.orchestrationFlags.put(key, value);
      return this;
    }

    public Builder preferredProviders(List<PreferredProvider> providers) {
      preferredProviders = providers;
      return this;
    }

    public Builder requestedQoS(Map<String, String> qos) {
      requestedQoS = qos;
      return this;
    }

    public Builder commands(Map<String, String> commands) {
      this.commands = commands;
      return this;
    }

    public ServiceRequestForm build() {
      return new ServiceRequestForm(this);
    }
  }

  public void validateCrossParameterConstraints() {
    if (requestedService == null && orchestrationFlags.get(Flags.OVERRIDE_STORE)) {
      throw new BadPayloadException("RequestedService can not be null when overrideStore is TRUE");
    }

    if (orchestrationFlags.get(Flags.ONLY_PREFERRED)) {
      List<PreferredProvider> tmp = new ArrayList<>();
      for (PreferredProvider provider : preferredProviders) {
        if (!provider.isValid()) {
          tmp.add(provider);
        }
      }
      preferredProviders.removeAll(tmp);
      if (preferredProviders.isEmpty()) {
        throw new BadPayloadException("There is no valid PreferredProvider, but \"onlyPreferred\" is set to true");
      }
    }

    if (orchestrationFlags.get(Flags.ENABLE_QOS) && (requestedQoS.isEmpty() || commands.isEmpty())) {
      throw new BadPayloadException("RequestedQoS or commands hashmap is empty while \"enableQoS\" is set to true");
    }
  }

}
