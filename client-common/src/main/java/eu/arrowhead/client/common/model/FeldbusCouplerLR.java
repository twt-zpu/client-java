package eu.arrowhead.client.common.model;

import java.util.ArrayList;
import java.util.List;

public class FeldbusCouplerLR {
  private String bn;
  private double bt;
  private String bu;
  private int ver;
  private List<FeldbusCouplerLRMeasurementEntry> e = new ArrayList<>();

  public FeldbusCouplerLR() {
  }

  public FeldbusCouplerLR(String bn, double bt, String bu, int ver) {
    this.bn = bn;
    this.bt = bt;
    this.bu = bu;
    this.ver = ver;
  }

  public FeldbusCouplerLR(String bn, double bt, String bu, int ver, List<FeldbusCouplerLRMeasurementEntry> e) {
    super();
    this.bn = bn;
    this.bt = bt;
    this.bu = bu;
    this.ver = ver;
    this.e = e;
  }

  public String getBn() {
    return bn;
  }

  public void setBn(String bn) {
    this.bn = bn;
  }

  public double getBt() {
    return bt;
  }

  public void setBt(double bt) {
    this.bt = bt;
  }

  public String getBu() {
    return bu;
  }

  public void setBu(String bu) {
    this.bu = bu;
  }

  public int getVer() {
    return ver;
  }

  public void setVer(int ver) {
    this.ver = ver;
  }

  public List<FeldbusCouplerLRMeasurementEntry> getE() {
    return e;
  }

  public void setE(List<FeldbusCouplerLRMeasurementEntry> e) {
    this.e = e;
  }
}
