/*
 *  Copyright (c) 2018 AITIA International Inc.
 *
 *  This work is part of the Productive 4.0 innovation project, which receives grants from the
 *  European Commissions H2020 research and innovation programme, ECSEL Joint Undertaking
 *  (project no. 737459), the free state of Saxony, the German Federal Ministry of Education and
 *  national funding authorities from involved countries.
 */

package eu.arrowhead.common.model;

import eu.arrowhead.common.misc.ArrowheadProperties;
import eu.arrowhead.common.misc.Utility;

import java.util.*;

public class ArrowheadService {

    private Long id;
    private String serviceDefinition;
    private Set<String> interfaces = new HashSet<>();
    private ServiceMetadata serviceMetadata = new ServiceMetadata();

    public static ArrowheadService createFromProperties() {
        return createFromProperties(Utility.getProp());
    }

    public static ArrowheadService createFromProperties(ArrowheadProperties props) {
        boolean isSecure = props.isSecure();
        String serviceDef = props.getServiceName();

        String interfaceList = props.getInterfaces();
        Set<String> interfaces = new HashSet<>();
        if (interfaceList != null && !interfaceList.isEmpty()) {
            interfaces.addAll(Arrays.asList(interfaceList.replaceAll("\\s+", "").split(",")));
        }

        final ServiceMetadata metadata = props.getServiceMetadata();

        if (isSecure) {
            if (!metadata.containsKey(ServiceMetadata.Keys.SECURITY)) {
                metadata.setSecurity(ServiceMetadata.Security.TOKEN);
            }
        }

        return new ArrowheadService(serviceDef, interfaces, metadata);
    }

    public ArrowheadService() {
    }

    /**
     * Constructor with all the fields of the ArrowheadService class.
     *
     * @param serviceDefinition A descriptive name for the service
     * @param interfaces The set of interfaces that can be used to consume this service (helps interoperability between
     *     ArrowheadSystems). Concrete meaning of what is an interface is service specific (e.g. JSON, I2C)
     * @param serviceMetadata Arbitrary additional serviceMetadata belonging to the service, stored as key-value pairs.
     */
    public ArrowheadService(String serviceDefinition, Set<String> interfaces, ServiceMetadata serviceMetadata) {
        this.serviceDefinition = serviceDefinition;
        this.interfaces = interfaces;
        this.serviceMetadata = serviceMetadata;
    }

    public ArrowheadService(String serviceDefinition, String aInterface, ServiceMetadata serviceMetadata) {
        this.serviceDefinition = serviceDefinition;
        this.interfaces = Collections.singleton(aInterface);
        this.serviceMetadata = serviceMetadata;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getServiceDefinition() {
        return serviceDefinition;
    }

    public void setServiceDefinition(String serviceDefinition) {
        this.serviceDefinition = serviceDefinition;
    }

    public Set<String> getInterfaces() {
        return interfaces;
    }

    public void setInterfaces(Set<String> interfaces) {
        this.interfaces = interfaces;
    }

    public ServiceMetadata getServiceMetadata() {
        return serviceMetadata;
    }

    public void setServiceMetadata(ServiceMetadata serviceMetadata) {
        this.serviceMetadata = serviceMetadata;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ArrowheadService)) {
            return false;
        }

        ArrowheadService that = (ArrowheadService) o;

        if (!serviceDefinition.equals(that.serviceDefinition)) {
            return false;
        }

        //2 services can be equal if they have at least 1 common interface
        Set<String> intersection = new HashSet<>(interfaces);
        intersection.retainAll(that.interfaces);
        return !intersection.isEmpty();
    }

    @Override
    public int hashCode() {
        return serviceDefinition.hashCode();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ArrowheadService{");
        sb.append("id=").append(id);
        sb.append(", serviceDefinition='").append(serviceDefinition).append('\'');
        sb.append('}');
        return sb.toString();
    }

    // TODO This does not copy entries - changing one list changes the other, Thomas
    public void partialUpdate(ArrowheadService other) {
        this.serviceDefinition = other.getServiceDefinition() != null ? other.getServiceDefinition() : this.serviceDefinition;
        this.interfaces = other.getInterfaces().isEmpty() ? this.interfaces : other.getInterfaces();
        this.serviceMetadata = other.getServiceMetadata().isEmpty() ? this.serviceMetadata : other.getServiceMetadata();
    }
}