/*
 *  Copyright (c) 2018 AITIA International Inc.
 *
 *  This work is part of the Productive 4.0 innovation project, which receives grants from the
 *  European Commissions H2020 research and innovation programme, ECSEL Joint Undertaking
 *  (project no. 737459), the free state of Saxony, the German Federal Ministry of Education and
 *  national funding authorities from involved countries.
 */

package eu.arrowhead.demo.model;

//Sample model class to demonstrate REST capabilities in the RestResource class
public class Car {

  private String brand;
  private String color;

  //JSON libraries will use the empty constructor to create a new object during deserialization, so it is important to have one
  public Car() {
  }

  public Car(String brand, String color) {
    this.brand = brand;
    this.color = color;
  }

  //Getter methods are used during serialization
  public String getBrand() {
    return brand;
  }

  //After creating the object, JSON libraries use the setter methods during deserialization
  public void setBrand(String brand) {
    this.brand = brand;
  }

  public String getColor() {
    return color;
  }

  public void setColor(String color) {
    this.color = color;
  }

}
