package eu.arrowhead.client.consumer;

import eu.arrowhead.client.common.model.FeldbusCouplerLMeasurementEntry;

public class FeldbusCouplerLMeasurement {
	public static FeldbusCouplerLMeasurementEntry entry = new FeldbusCouplerLMeasurementEntry();
	private static boolean existence = false;
	
	public void setExistence(boolean existence){
		this.existence = existence;
	}
	
	public boolean isExist(){
		return existence;
	}
}