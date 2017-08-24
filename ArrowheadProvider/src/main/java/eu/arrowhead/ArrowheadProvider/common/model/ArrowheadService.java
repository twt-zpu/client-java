package eu.arrowhead.ArrowheadProvider.common.model;

import java.util.ArrayList;
import java.util.List;

public class ArrowheadService {

  private String serviceGroup;
  private String serviceDefinition;
  private List<String> interfaces = new ArrayList<>();
  private List<ServiceMetadata> serviceMetadata;

  public ArrowheadService() {
  }

  public ArrowheadService(String serviceGroup, String serviceDefinition, List<String> interfaces, List<ServiceMetadata> serviceMetadata) {
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

  public List<ServiceMetadata> getServiceMetadata() {
    return serviceMetadata;
  }

  public void setServiceMetadata(List<ServiceMetadata> metaData) {
    this.serviceMetadata = metaData;
  }

  public boolean isValid() {
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
