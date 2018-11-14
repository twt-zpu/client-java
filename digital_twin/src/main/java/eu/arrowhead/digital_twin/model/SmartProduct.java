package eu.arrowhead.digital_twin.model;

import java.util.ArrayList;
import java.util.List;
import org.glassfish.jersey.internal.guava.MoreObjects;

public class SmartProduct {

  private List<String> rfidParts = new ArrayList<>();
  private SmartProductLifeCycle lifeCycle = SmartProductLifeCycle.CREATED;
  private SmartProductPosition lastKnownPosition = SmartProductPosition.OUTSIDE_OF_GEOFENCED_AREA;

  public SmartProduct() {
  }

  public SmartProduct(List<String> rfidParts) {
    this.rfidParts = rfidParts;
  }

  public SmartProduct(List<String> rfidParts, SmartProductLifeCycle lifeCycle, SmartProductPosition lastKnownPosition) {
    this.rfidParts = rfidParts;
    this.lifeCycle = lifeCycle;
    this.lastKnownPosition = lastKnownPosition;
  }

  public List<String> getRfidParts() {
    return rfidParts;
  }

  public void setRfidParts(List<String> rfidParts) {
    this.rfidParts = rfidParts;
  }

  public SmartProductLifeCycle getLifeCycle() {
    return lifeCycle;
  }

  public void setLifeCycle(SmartProductLifeCycle lifeCycle) {
    this.lifeCycle = lifeCycle;
  }

  public SmartProductPosition getLastKnownPosition() {
    return lastKnownPosition;
  }

  public void setLastKnownPosition(SmartProductPosition lastKnownPosition) {
    this.lastKnownPosition = lastKnownPosition;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("rfidParts", rfidParts).add("lifeCycle", lifeCycle).add("lastKnownPosition", lastKnownPosition)
                      .toString();
  }
}
