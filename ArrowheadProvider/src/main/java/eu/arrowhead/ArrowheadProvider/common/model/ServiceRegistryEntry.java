package eu.arrowhead.ArrowheadProvider.common.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.List;
import java.util.Map;

public class ServiceRegistryEntry {

  //mandatory fields for JSON
  private ArrowheadService providedService;
  private ArrowheadSystem provider;

  //non-mandatory fields for JSON
  private int port;
  private String serviceURI;
  @JsonIgnore
  private String metadata;
  private int version = 1;
  private boolean UDP = false;

  //only for backwards compatibility, used by DNS-SD
  private List<String> interfaces;
  private String TSIG_key;

  public ServiceRegistryEntry() {
  }

  public ServiceRegistryEntry(ArrowheadService providedService, ArrowheadSystem provider) {
    this.providedService = providedService;
    this.provider = provider;
  }

  public ServiceRegistryEntry(ArrowheadService providedService, ArrowheadSystem provider, String serviceURI) {
    this.providedService = providedService;
    this.provider = provider;
    this.serviceURI = serviceURI;
  }

  public ServiceRegistryEntry(ArrowheadService providedService, ArrowheadSystem provider, int port, String serviceURI) {
    this.providedService = providedService;
    this.provider = provider;
    this.port = port;
    this.serviceURI = serviceURI;
  }

  public ServiceRegistryEntry(ArrowheadService providedService, ArrowheadSystem provider, int port, String serviceURI, String metadata, int version,
                              boolean UDP) {
    this.providedService = providedService;
    this.provider = provider;
    this.port = port;
    this.serviceURI = serviceURI;
    this.metadata = metadata;
    this.version = version;
    this.UDP = UDP;
  }

  public ArrowheadService getProvidedService() {
    return providedService;
  }

  public void setProvidedService(ArrowheadService providedService) {
    this.providedService = providedService;
  }

  public ArrowheadSystem getProvider() {
    return provider;
  }

  public void setProvider(ArrowheadSystem provider) {
    this.provider = provider;
  }

  public int getPort() {
    return port;
  }

  public void setPort(int port) {
    this.port = port;
  }

  public String getServiceURI() {
    return serviceURI;
  }

  public void setServiceURI(String serviceURI) {
    this.serviceURI = serviceURI;
  }

  public String getMetadata() {
    return metadata;
  }

  public void setMetadata(String metadata) {
    this.metadata = metadata;
  }

  public int getVersion() {
    return version;
  }

  public void setVersion(int version) {
    this.version = version;
  }

  public boolean isUDP() {
    return UDP;
  }

  public void setUDP(boolean UDP) {
    this.UDP = UDP;
  }

  public List<String> getInterfaces() {
    return interfaces;
  }

  public void setInterfaces(List<String> interfaces) {
    this.interfaces = interfaces;
  }

  public String getTSIG_key() {
    return TSIG_key;
  }

  public void setTSIG_key(String TSIG_key) {
    this.TSIG_key = TSIG_key;
  }

  @JsonIgnore
  public boolean isValid() {
    return provider != null && provider.isValid() && providedService != null && providedService.isValid();
  }

  @Override
  public String toString() {
    return providedService.getServiceDefinition() + ":" + provider.getSystemName();
  }

  @JsonIgnore
  public void toStringMetadata() {
    if (!providedService.getServiceMetadata().isEmpty()) {
      StringBuilder sb = new StringBuilder();
      for (Map.Entry<String, String> entry : providedService.getServiceMetadata().entrySet()) {
        sb.append(entry.getKey()).append("=").append(entry.getValue()).append(",");
      }
      sb.substring(0, sb.length() - 1);
      this.metadata = sb.toString();
    }
  }

  @JsonIgnore
  public void toHashmapMetadata() {
    if (metadata != null) {
      String[] parts = metadata.split(",");
      for (String part : parts) {
        String[] pair = part.split("=");
        this.providedService.getServiceMetadata().put(pair[0], pair[1]);
      }
    }
  }

}
