package eu.arrowhead.BasicConsumer.model;

public class TemperatureReadout {

  private double temperature;

  public TemperatureReadout() {
  }

  public TemperatureReadout(double temperature) {
    this.temperature = temperature;
  }

  public double getTemperature() {
    return temperature;
  }

  public void setTemperature(double temperature) {
    this.temperature = temperature;
  }

}
