package eu.arrowhead.ArrowheadConsumer.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class TemperatureReadout implements Serializable
{

    @SerializedName("bn")
    @Expose
    private String bn;
    @SerializedName("bt")
    @Expose
    private double bt;
    @SerializedName("bu")
    @Expose
    private String bu;
    @SerializedName("ver")
    @Expose
    private int ver;
    @SerializedName("e")
    @Expose
    private List<MeasurementEntry> e = new ArrayList<>();
    private final static long serialVersionUID = 56004740894442796L;

    /**
     * No args constructor for use in serialization
     * 
     */
    public TemperatureReadout() {
    }

    public TemperatureReadout(String bn, double bt, String bu, int ver) {
        this.bn = bn;
        this.bt = bt;
        this.bu = bu;
        this.ver = ver;
    }

    /**
     * 
     * @param bn
     * @param e
     * @param ver
     * @param bu
     * @param bt
     */
    public TemperatureReadout(String bn, double bt, String bu, int ver, List<MeasurementEntry> e) {
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

    public List<MeasurementEntry> getE() {
        return e;
    }

    public void setE(List<MeasurementEntry> e) {
        this.e = e;
    }

}
