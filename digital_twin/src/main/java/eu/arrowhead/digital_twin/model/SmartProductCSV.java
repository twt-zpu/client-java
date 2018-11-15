package eu.arrowhead.digital_twin.model;

public class SmartProductCSV {

  private String rfidParts;
  private String lifeCycle;
  private String lastKnownPosition;

  public SmartProductCSV() {
  }

  public SmartProductCSV(String rfidParts, String lifeCycle, String lastKnownPosition) {
    this.rfidParts = rfidParts;
    this.lifeCycle = lifeCycle;
    this.lastKnownPosition = lastKnownPosition;
  }

  public String getRfidParts() {
    return rfidParts;
  }

  public void setRfidParts(String rfidParts) {
    this.rfidParts = rfidParts;
  }

  public String getLifeCycle() {
    return lifeCycle;
  }

  public void setLifeCycle(String lifeCycle) {
    this.lifeCycle = lifeCycle;
  }

  public String getLastKnownPosition() {
    return lastKnownPosition;
  }

  public void setLastKnownPosition(String lastKnownPosition) {
    this.lastKnownPosition = lastKnownPosition;
  }
}
