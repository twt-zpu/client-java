package eu.arrowhead.ArrowheadConsumer.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class MeasurementEntry implements Serializable
{

    @SerializedName("n")
    @Expose
    private String n;
    @SerializedName("v")
    @Expose
    private double v;
    @SerializedName("t")
    @Expose
    private double t;
    private final static long serialVersionUID = -5996554298375793293L;

    /**
     * No args constructor for use in serialization
     * 
     */
    public MeasurementEntry() {
    }

    /**
     * 
     * @param v
     * @param t
     * @param n
     */
    public MeasurementEntry(String n, double v, double t) {
        super();
        this.n = n;
        this.v = v;
        this.t = t;
    }

    public String getN() {
        return n;
    }

    public void setN(String n) {
        this.n = n;
    }

    public double getV() {
        return v;
    }

    public void setV(double v) {
        this.v = v;
    }

    public double getT() {
        return t;
    }

    public void setT(double t) {
        this.t = t;
    }

}
