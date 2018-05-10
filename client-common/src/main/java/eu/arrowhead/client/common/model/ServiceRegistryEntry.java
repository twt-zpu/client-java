/*
 *  Copyright (c) 2018 AITIA International Inc.
 *
 *  This work is part of the Productive 4.0 innovation project, which receives grants from the
 *  European Commissions H2020 research and innovation programme, ECSEL Joint Undertaking
 *  (project no. 737459), the free state of Saxony, the German Federal Ministry of Education and
 *  national funding authorities from involved countries.
 */

package eu.arrowhead.client.common.model;

public class ServiceRegistryEntry {

  //mandatory fields for JSON
  private ArrowheadService providedService;
  private ArrowheadSystem provider;

  //non-mandatory fields for JSON
  private int port;
  private String serviceURI;
  private int version = 1;
  private boolean UDP = false;

  //Time to live in seconds - service validity length
  private int ttl;

  public ServiceRegistryEntry() {
  }

  public ServiceRegistryEntry(ArrowheadService providedService, ArrowheadSystem provider) {
    this.providedService = providedService;
    this.provider = provider;
  }

  public ServiceRegistryEntry(ArrowheadService providedService, ArrowheadSystem provider, String serviceURI) {
    this.providedService = providedService;
    this.provider = provider;
    this.port = provider.getPort();
    this.serviceURI = serviceURI;
  }

  public ServiceRegistryEntry(ArrowheadService providedService, ArrowheadSystem provider, int port, String serviceURI) {
    this.providedService = providedService;
    this.provider = provider;
    this.port = port;
    this.serviceURI = serviceURI;
  }

  public ServiceRegistryEntry(ArrowheadService providedService, ArrowheadSystem provider, Integer port, String serviceURI, Integer version,
                              boolean UDP, int ttl) {
    this.providedService = providedService;
    this.provider = provider;
    this.port = port;
    this.serviceURI = serviceURI;
    this.version = version;
    this.UDP = UDP;
    this.ttl = ttl;
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

  public int getTtl() {
    return ttl;
  }

  public void setTtl(int ttl) {
    this.ttl = ttl;
  }

  public boolean isValid() {
    return provider != null && provider.isValid() && providedService != null && providedService.isValid();
  }

  @Override
  public String toString() {
    if (providedService != null && providedService.getServiceDefinition() != null && provider != null && provider.getSystemName() != null) {
      return providedService.getServiceDefinition() + ":" + provider.getSystemName();
    } else {
      return "ServiceRegistryEntry not initialized yet";
    }
  }

}
