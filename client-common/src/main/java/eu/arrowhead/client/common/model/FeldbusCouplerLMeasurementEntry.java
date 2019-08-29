package eu.arrowhead.client.common.model;

import java.util.ArrayList;
import java.util.List;

public class FeldbusCouplerLMeasurementEntry {
	private boolean SF11;
	private boolean SF12;
	private boolean SF2;
	private boolean SF3;
	private boolean SF4;
	private boolean SPD11;
	private boolean SPD12;
	private boolean SPD21;
	private boolean SPD22;
	private boolean M1;
	private boolean M2;
	private boolean C1;
	private boolean C2;
	private boolean C3;
	private boolean C4;
	
	public FeldbusCouplerLMeasurementEntry() {
	}
	
	public void setSF11(boolean SF11){
		this.SF11 = SF11;
	}
	
	public boolean getSF11(){
		return SF11;
	}

	public void setSF12(boolean SF12){
		this.SF12 = SF12;
	}
	
	public boolean getSF12(){
		return SF12;
	}
	
	public void setSF2(boolean SF2){
		this.SF2 = SF2;
	}
	
	public boolean getSF2(){
		return SF2;
	}
	
	public void setSF3(boolean SF3){
		this.SF3 = SF3;
	}
	
	public boolean getSF3(){
		return SF3;
	}
	
	public void setSF4(boolean SF4){
		this.SF4 = SF4;
	}
	
	public boolean getSF4(){
		return SF4;
	}

	public void setSPD11(boolean SPD11){
		this.SPD11 = SPD11;
	}
	
	public boolean getSPD11(){
		return SPD11;
	}
	
	public void setSPD12(boolean SPD12){
		this.SPD12 = SPD12;
	}
	
	public boolean getSPD12(){
		return SPD12;
	}
	
	public void setSPD21(boolean SPD21){
		this.SPD21 = SPD21;
	}
	
	public boolean getSPD21(){
		return SPD21;
	}
	
	public void setSPD22(boolean SPD22){
		this.SPD22 = SPD22;
	}
	
	public boolean getSPD22(){
		return SPD22;
	}
	
	public void setM1(boolean M1){
		this.M1 = M1;
	}
	
	public boolean getM1(){
		return M1;
	}
	
	public void setM2(boolean M2){
		this.M2 = M2;
	}
	
	public boolean getM2(){
		return M2;
	}
	
	public void setC1(boolean C1){
		this.C1 = C1;
	}
	
	public boolean getC1(){
		return C1;
	}
	
	public void setC2(boolean C2){
		this.C2 = C2;
	}
	
	public boolean getC2(){
		return C2;
	}
	
	public void setC3(boolean C3){
		this.C3 = C3;
	}
	
	public boolean getC3(){
		return C3;
	}
	
	public void setC4(boolean C4){
		this.C4 = C4;
	}
	
	public boolean getC4(){
		return C4;
	}
	
	public void setSF(boolean SF11, boolean SF12, boolean SF2, boolean SF3, boolean SF4){
		this.SF11 = SF11;
		this.SF12 = SF12;
		this.SF2 = SF2;
		this.SF3 = SF3;
		this.SF4 = SF4;
	}
	
	public List<Boolean> getSF(){
		List<Boolean> sf = new ArrayList<>();
		sf.add(SF11);
		sf.add(SF12);
		sf.add(SF2);
		sf.add(SF3);
		sf.add(SF4);
		return sf;
	}
	
	public void setSPD(boolean SPD11, boolean SPD12, boolean SPD21, boolean SPD22){
		this.SPD11 = SPD11;
		this.SPD12 = SPD12;
		this.SPD21 = SPD21;
		this.SPD22 = SPD22;
	}
	
	public List<Boolean> getSPD(){
		List<Boolean> spd = new ArrayList<>();
		spd.add(SPD11);
		spd.add(SPD12);
		spd.add(SPD21);
		spd.add(SPD22);
		return spd;
	}
	
	public void setM(boolean M1, boolean M2){
		this.M1 = M1;
		this.M2 = M2;
	}
	
	public List<Boolean> getM(){
		List<Boolean> m = new ArrayList<>();
		m.add(M1);
		m.add(M2);
		return m;
	}
	
	public void setC(boolean C1, boolean C2, boolean C3, boolean C4){
		this.C1 = C1;
		this.C2 = C2;
		this.C3 = C3;
		this.C4 = C4;
	}
	
	public List<Boolean> getC(){
		List<Boolean> c = new ArrayList<>();
		c.add(C1);
		c.add(C2);
		c.add(C3);
		c.add(C4);
		return c;
	}
	
	public List<Boolean> getOutput(){
		List<Boolean> output = new ArrayList<>();
		output.add(C4);
		output.add(M2);
		output.add(C3);
		output.add(M1);
		output.add(C2);
		output.add(C1);
		output.add(SPD22);
		output.add(SPD21);
		output.add(SPD12);
		output.add(SPD11);
		return output;
	}
	
	public boolean setOutput(boolean[] coils){
		if (coils.length != 10)
			return false;
		C4 = coils[0];
		M2 = coils[1];
		C3 = coils[2];
		M1 = coils[3];
		C2 = coils[4];
		C1 = coils[5];
		SPD22 = coils[6];
		SPD21 = coils[7];
		SPD12 = coils[8];
		SPD11 = coils[9];
		return true;
	}
	
	public List<Boolean> getInput(){
		List<Boolean> input = new ArrayList<>();
		input.add(SF4);
		input.add(SF3);
		input.add(SF11);
		input.add(SF2);
		input.add(SF12);
		input.add(SPD22);
		input.add(SPD21);
		input.add(SPD12);
		input.add(SPD11);
		return input;
	}
	
	public boolean setInput(boolean[] coils){
		if (coils.length != 9)
			return false;
		SF4 = coils[0];
		SF3 = coils[1];
		SF11 = coils[2];
		SF2 = coils[3];
		SF12 = coils[4];
		SPD22 = coils[5];
		SPD21 = coils[6];
		SPD12 = coils[7];
		SPD11 = coils[8];
		return true;
	}
}
