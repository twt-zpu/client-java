package eu.arrowhead.client.provider;

import eu.arrowhead.client.common.model.FeldbusCouplerLMeasurementEntry;

public class FeldbusCouplerLMeasurement {
	public static final FeldbusCouplerLMeasurementEntry entry = new FeldbusCouplerLMeasurementEntry();
	public static boolean existence = false;
	
	public static void setExistence(boolean existence){
		FeldbusCouplerLMeasurement.existence = existence;
	}
	
	public static boolean isExist(){
		return existence;
	}
}
