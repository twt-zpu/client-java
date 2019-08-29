package eu.arrowhead.client.consumer;

import eu.arrowhead.client.common.model.ModbusMeasurementEntry;

public class ModbusData {
	private static ModbusMeasurementEntry entry = new ModbusMeasurementEntry();
	private static boolean existence = false;
	
	public static void setExistence(boolean existence){
		ModbusData.existence = existence;
	}
	
	public static boolean isExist(){
		return existence;
	}
	
	public static void setEntry(ModbusMeasurementEntry entry){
		ModbusData.entry = entry;
	}
	
	public static ModbusMeasurementEntry getEntry(){
		return entry;
	}
}
