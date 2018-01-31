/*
 * Copyright (c) 2018 AITIA International Inc.
 *
 * This work is part of the Productive 4.0 innovation project, which receives grants from the
 * European Commissions H2020 research and innovation programme, ECSEL Joint Undertaking
 * (project no. 737459), the free state of Saxony, the German Federal Ministry of Education and
 * national funding authorities from involved countries.
 */

package eu.arrowhead.ArrowheadProvider.common.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Entity class for storing Arrowhead Services in the database. The "service_definition" column must be unique.
 */
public class ArrowheadService {

  private String serviceDefinition;
  private List<String> interfaces = new ArrayList<>();
  private Map<String, String> serviceMetadata = new HashMap<>();

  public ArrowheadService() {
  }

  public ArrowheadService(String serviceDefinition, List<String> interfaces, Map<String, String> serviceMetadata) {
    this.serviceDefinition = serviceDefinition;
    this.interfaces = interfaces;
    this.serviceMetadata = serviceMetadata;
  }

  public String getServiceDefinition() {
    return serviceDefinition;
  }

  public void setServiceDefinition(String serviceDefinition) {
    this.serviceDefinition = serviceDefinition;
  }

  public List<String> getInterfaces() {
    return interfaces;
  }

  public void setInterfaces(List<String> interfaces) {
    this.interfaces = interfaces;
  }

  public void setOneInterface(String oneInterface) {
    this.interfaces.clear();
    this.interfaces.add(oneInterface);
  }

  public Map<String, String> getServiceMetadata() {
    return serviceMetadata;
  }

  public void setServiceMetadata(Map<String, String> metaData) {
    this.serviceMetadata = metaData;
  }

  @JsonIgnore
  public boolean isValid() {
    return (serviceDefinition != null && !interfaces.isEmpty());
  }

  @JsonIgnore
  public boolean isValidForDatabase() {
    return serviceDefinition != null;
  }

  @JsonIgnore
  public boolean isValidForDNSSD() {
    boolean areInterfacesClean = true;
    for (String interf : interfaces) {
      if (interf.contains("_")) {
        areInterfacesClean = false;
      }
    }

    return (serviceDefinition != null && !interfaces.isEmpty() && !serviceDefinition.contains("_") && areInterfacesClean);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    ArrowheadService that = (ArrowheadService) o;

    return serviceDefinition.equals(that.serviceDefinition);
  }

  @Override
  public int hashCode() {
    return serviceDefinition.hashCode();
  }

  @Override
  public String toString() {
    return "\"" + serviceDefinition + "\"";
  }

}
