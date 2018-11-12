package eu.arrowhead.digital_twin.model;

public enum SmartProductLifeCycle {
  CREATED, MILLED, ASSEMBLED, STORED, PURCHASED, FINISHED;

  private static SmartProductLifeCycle[] vals = values();

  public SmartProductLifeCycle next() {
    return vals[(this.ordinal() + 1) % vals.length];
  }
}
