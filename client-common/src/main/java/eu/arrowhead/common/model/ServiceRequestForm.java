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

import javax.validation.constraints.Size;
import java.util.*;

/**
 * This is what the Orchestrator Core System receives from Arrowhead Systems trying to request services.
 */
public class ServiceRequestForm {

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

    public ArrowheadCloud getRequesterCloud() {
        return requesterCloud;
    }

    public ArrowheadService getRequestedService() {
        return requestedService;
    }


    public Map<String, Boolean> getOrchestrationFlags() {
        return orchestrationFlags;
    }

    public List<PreferredProvider> getPreferredProviders() {
        return preferredProviders;
    }

    public Map<String, String> getRequestedQoS() {
        return requestedQoS;
    }

    public Map<String, String> getCommands() {
        return commands;
    }

    public static class Builder {

        // Required parameters
        private final ArrowheadSystem requesterSystem;
        // Optional parameters
        private ArrowheadCloud requesterCloud;
        private ArrowheadService requestedService;

        @Size(max = 9, message = "There are only 9 orchestration flags, map size must not be bigger than 9")
        private final OrchestrationFlags orchestrationFlags = new OrchestrationFlags();
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

        public Builder requestedService(String serviceDefinition, Set<String> interfaces, ServiceMetadata serviceMetadata) {
            requestedService = new ArrowheadService(serviceDefinition, interfaces, serviceMetadata);
            return this;
        }

        public Builder requestedService(String serviceDefinition, String aInterface, ServiceMetadata serviceMetadata) {
            requestedService = new ArrowheadService(serviceDefinition, aInterface, serviceMetadata);
            return this;
        }

        public Builder requestedService(String serviceDefinition, String aInterface, boolean secure) {
            requestedService = new ArrowheadService(serviceDefinition, aInterface, secure);
            return this;
        }

        public Builder flag(OrchestrationFlags.Flags flag, Boolean value) {
            this.orchestrationFlags.put(flag, value);
            return this;
        }

        public Builder flag(String key, Boolean value) {
            this.orchestrationFlags.put(key, value);
            return this;
        }

        public Builder flags(Map<String, Boolean> flags) {
            this.orchestrationFlags.putAll(flags);
            return this;
        }

        public Builder flags(OrchestrationFlags flags) {
            this.orchestrationFlags.putAll(flags);
            return this;
        }

        public Builder metadata(ServiceMetadata.Keys key, String value) {
            this.requestedService.getServiceMetadata().put(key, value);
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
            return new ServiceRequestForm(this).validateCrossParameterConstraints();
        }
    }

    public ServiceRequestForm validateCrossParameterConstraints() {
        if (requestedService == null && orchestrationFlags.get(OrchestrationFlags.Flags.OVERRIDE_STORE)) {
            throw new BadPayloadException("RequestedService can not be null when overrideStore is TRUE");
        }

        if (orchestrationFlags.get(OrchestrationFlags.Flags.ONLY_PREFERRED)) {
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

        if (orchestrationFlags.get(OrchestrationFlags.Flags.ENABLE_QOS) && (requestedQoS.isEmpty() || commands.isEmpty())) {
            throw new BadPayloadException("RequestedQoS or commands hashmap is empty while \"enableQoS\" is set to true");
        }

        return this;
    }

}
