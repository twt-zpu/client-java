package eu.arrowhead.ArrowheadConsumer.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import javax.xml.bind.annotation.XmlTransient;

/**
 * Entity class for storing Arrowhead Systems in the database. The "system_group" and "system_name" columns must be unique together.
 */
public class ArrowheadSystem {

  private int id;
  private String systemName;
  private String address;
  private int port;
  private String authenticationInfo;

  public ArrowheadSystem() {
  }

  public ArrowheadSystem(String systemName, String address, int port, String authenticationInfo) {
    this.systemName = systemName;
    this.address = address;
    this.port = port;
    this.authenticationInfo = authenticationInfo;
  }

  @XmlTransient
  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public String getSystemName() {
    return systemName;
  }

  public void setSystemName(String systemName) {
    this.systemName = systemName;
  }

  public String getAddress() {
    return address;
  }

  public void setAddress(String address) {
    this.address = address;
  }

  public int getPort() {
    return port;
  }

  public void setPort(int port) {
    this.port = port;
  }

  public String getAuthenticationInfo() {
    return authenticationInfo;
  }

  public void setAuthenticationInfo(String authenticationInfo) {
    this.authenticationInfo = authenticationInfo;
  }

  @JsonIgnore
  public boolean isValid() {
    return systemName != null && address != null;
  }

  @JsonIgnore
  public boolean isValidForDatabase() {
    return systemName != null;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    ArrowheadSystem that = (ArrowheadSystem) o;

    if (port != that.port) {
      return false;
    }
    if (!systemName.equals(that.systemName)) {
      return false;
    }
    return address.equals(that.address);
  }

  @Override
  public int hashCode() {
    int result = systemName.hashCode();
    result = 31 * result + address.hashCode();
    result = 31 * result + port;
    return result;
  }

  @Override
  public String toString() {
    return "\"" + systemName + "\"";
  }

}
