package eu.arrowhead.ArrowheadProvider.common.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.bind.annotation.XmlTransient;

/**
 * Entity class for storing Arrowhead Services in the database. The "service_group" and service_definition" columns must be unique together.
 */
public class ArrowheadService {

  private int id;
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

  @XmlTransient
  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
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

  /*
   * @note  ArrowheadServices cannot contain the character "_" in any fields.
   */
  @JsonIgnore
  public boolean isValid() {

    boolean areInterfacesClean = true;
    for (String interf : interfaces) {
      if (interf.contains("_")) {
        areInterfacesClean = false;
      }
    }

    return (serviceDefinition != null && !interfaces.isEmpty() && !serviceDefinition.contains("_") && areInterfacesClean);
  }

  @JsonIgnore
  public boolean isValidForDatabase() {
    return serviceDefinition != null;
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
