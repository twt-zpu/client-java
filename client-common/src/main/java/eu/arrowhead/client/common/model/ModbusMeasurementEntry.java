package eu.arrowhead.client.common.model;

import java.util.HashMap;

public class ModbusMeasurementEntry {
	private HashMap<Integer, Boolean> coilsInput = new HashMap<Integer, Boolean>();
	private HashMap<Integer, Boolean> coilsOutput = new HashMap<Integer, Boolean>();
	private HashMap<Integer, Integer> registersInput = new HashMap<Integer, Integer>();
	private HashMap<Integer, Integer> registersOutput = new HashMap<Integer, Integer>();
	
	public void setCoilInput(int address, boolean value){
		coilsInput.put(address, value);
	}
	
	public void setCoilsInput(int address, boolean[] values){
		int index = address;
		for(boolean value : values)
			coilsInput.put(index++, value);
	}
	
	public void setCoilsInput(HashMap<Integer, Boolean> coilsInput){
		this.coilsInput = coilsInput;
	}
	
	public HashMap<Integer, Boolean> getCoilsInput(){
		return coilsInput;
	}
	
	public void setCoilOutput(int address, boolean value){
		coilsOutput.put(address, value);
	}
	
	public void setCoilsOutput(int address, boolean[] values){
		int index = address;
		for(boolean value : values)
			coilsOutput.put(index++, value);
	}
	
	public void setCoilsOutput(HashMap<Integer, Boolean> coilsOutput){
		this.coilsOutput = coilsOutput;
	}
	
	public HashMap<Integer, Boolean> getCoilsOutput(){
		return coilsOutput;
	}
	
	public void setRegisterInput(int address, int value){
		registersInput.put(address, value);
	}
	
	public void setRegistersInput(int address, int[] values){
		int index = address;
		for(int value : values)
			registersInput.put(index++, value);
	}
	
	public void setRegistersInput(HashMap<Integer, Integer> registersInput){
		this.registersInput = registersInput;
	}
	
	public HashMap<Integer, Integer> getRegistersInput(){
		return registersInput;
	}
	
	public void setRegisterOutput(int address, int value){
		registersOutput.put(address, value);
	}
	
	public void setRegistersOutput(int address, int[] values){
		int index = address;
		for(int value : values)
			registersOutput.put(index++, value);
	}
	
	public HashMap<Integer, Integer> getRegistersOutput(){
		return registersOutput;
	}
	
	public void setRegistersOutput(HashMap<Integer, Integer> registersOutput){
		this.registersOutput = registersOutput;
	}
}
