package eu.arrowhead.BasicConsumer.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ArrowheadService {

  private String serviceGroup;
  private String serviceDefinition;
  private List<String> interfaces = new ArrayList<>();
  private Map<String, String> serviceMetadata = new HashMap<>();

  public ArrowheadService() {
  }

  public ArrowheadService(String serviceGroup, String serviceDefinition, List<String> interfaces, Map<String, String> serviceMetadata) {
    this.serviceGroup = serviceGroup;
    this.serviceDefinition = serviceDefinition;
    this.interfaces = interfaces;
    this.serviceMetadata = serviceMetadata;
  }

  public String getServiceGroup() {
    return serviceGroup;
  }

  public void setServiceGroup(String serviceGroup) {
    this.serviceGroup = serviceGroup;
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

  public void setInterfaces(String oneInterface) {
    List<String> interfaces = new ArrayList<>();
    interfaces.add(oneInterface);
    this.interfaces = interfaces;
  }

  public void setInterfaces(List<String> interfaces) {
    this.interfaces = interfaces;
  }

  public Map<String, String> getServiceMetadata() {
    return serviceMetadata;
  }

  public void setServiceMetadata(Map<String, String> metaData) {
    this.serviceMetadata = metaData;
  }

  boolean isValid() {
    return serviceGroup != null && serviceDefinition != null;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((serviceDefinition == null) ? 0 : serviceDefinition.hashCode());
    result = prime * result + ((serviceGroup == null) ? 0 : serviceGroup.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    ArrowheadService other = (ArrowheadService) obj;
    if (serviceDefinition == null) {
      if (other.serviceDefinition != null) {
        return false;
      }
    } else if (!serviceDefinition.equals(other.serviceDefinition)) {
      return false;
    }
    if (serviceGroup == null) {
      if (other.serviceGroup != null) {
        return false;
      }
    } else if (!serviceGroup.equals(other.serviceGroup)) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "(" + serviceGroup + ":" + serviceDefinition + ")";
  }

}