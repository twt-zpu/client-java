/*
 *  Copyright (c) 2018 AITIA International Inc.
 *
 *  This work is part of the Productive 4.0 innovation project, which receives grants from the
 *  European Commissions H2020 research and innovation programme, ECSEL Joint Undertaking
 *  (project no. 737459), the free state of Saxony, the German Federal Ministry of Education and
 *  national funding authorities from involved countries.
 */

package eu.arrowhead.client.provider.common.model;

public class ArrowheadCloud {

  private String operator;
  private String cloudName;
  private String address;
  private Integer port;
  private String gatekeeperServiceURI;
  private String authenticationInfo;
  private Boolean secure;

  public ArrowheadCloud() {
  }

  public ArrowheadCloud(String operator, String cloudName, String address, Integer port, String gatekeeperServiceURI, String authenticationInfo,
                        Boolean secure) {
    this.operator = operator;
    this.cloudName = cloudName;
    this.address = address;
    this.port = port;
    this.gatekeeperServiceURI = gatekeeperServiceURI;
    this.authenticationInfo = authenticationInfo;
    this.secure = secure;
  }

  public String getOperator() {
    return operator;
  }

  public void setOperator(String operator) {
    this.operator = operator;
  }

  public String getCloudName() {
    return cloudName;
  }

  public void setCloudName(String cloudName) {
    this.cloudName = cloudName;
  }

  public String getAddress() {
    return address;
  }

  public void setAddress(String address) {
    this.address = address;
  }

  public Integer getPort() {
    return port;
  }

  public void setPort(Integer port) {
    this.port = port;
  }

  public String getGatekeeperServiceURI() {
    return gatekeeperServiceURI;
  }

  public void setGatekeeperServiceURI(String gatekeeperServiceURI) {
    this.gatekeeperServiceURI = gatekeeperServiceURI;
  }

  public String getAuthenticationInfo() {
    return authenticationInfo;
  }

  public void setAuthenticationInfo(String authenticationInfo) {
    this.authenticationInfo = authenticationInfo;
  }

  public Boolean isSecure() {
    return secure;
  }

  public void setSecure(Boolean secure) {
    this.secure = secure;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    ArrowheadCloud that = (ArrowheadCloud) o;

    if (!operator.equals(that.operator)) {
      return false;
    }
    if (!cloudName.equals(that.cloudName)) {
      return false;
    }
    if (address != null ? !address.equals(that.address) : that.address != null) {
      return false;
    }
    if (port != null ? !port.equals(that.port) : that.port != null) {
      return false;
    }
    if (gatekeeperServiceURI != null ? !gatekeeperServiceURI.equals(that.gatekeeperServiceURI) : that.gatekeeperServiceURI != null) {
      return false;
    }
    if (authenticationInfo != null ? !authenticationInfo.equals(that.authenticationInfo) : that.authenticationInfo != null) {
      return false;
    }
    return secure != null ? secure.equals(that.secure) : that.secure == null;
  }

  @Override
  public int hashCode() {
    int result = operator.hashCode();
    result = 31 * result + cloudName.hashCode();
    result = 31 * result + (address != null ? address.hashCode() : 0);
    result = 31 * result + (port != null ? port.hashCode() : 0);
    result = 31 * result + (gatekeeperServiceURI != null ? gatekeeperServiceURI.hashCode() : 0);
    result = 31 * result + (authenticationInfo != null ? authenticationInfo.hashCode() : 0);
    result = 31 * result + (secure != null ? secure.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "(" + operator + ":" + cloudName + ")";
  }

}