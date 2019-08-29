package eu.arrowhead.client.consumer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class ConsumerPLCApp {
	private static SimpleSlaveTCP simpleSlave = new SimpleSlaveTCP();
	private static ConsumerMain consumer;
	private static FeldbusCouplerLMeasurement measurement = new FeldbusCouplerLMeasurement();
	
	
	public static void main(String[] args) {
		simpleSlave.startSlave();
		new SlaveThread().start();
		consumer = new ConsumerMain(args);
		new ArrowheadThread().start();
		
		System.out.println("Type \"stop\" to shutdown Server...");
	    BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		String input = "";
		try {
	        while (!input.equals("stop")) {
	          input = br.readLine();
	        }
	        br.close();
	      } catch (IOException e) {
	        e.printStackTrace();
	      }
		
	}
	
	public static class SlaveThread extends Thread{
		@Override public void run(){
			simpleSlave.startSlave();
		}
	}
	
	public static class ArrowheadThread extends Thread{
		@Override public void run(){
			// consumer.startConsumerGetCoils();
			// boolean[] coils = {false, false, false, false, false, false, false, false, false, false};
            // measurement.entry.setOutput(coils);
			while(true){
				long startTime = System.currentTimeMillis();
				consumer.startConsumerGetCoils();
				consumer.startConsumerSetCoils();
				long endTime = System.currentTimeMillis();
				long time = endTime - startTime;
				if (time > 10){
					continue;
				}
				try {
		            Thread.sleep(10 - time);
		        } catch (InterruptedException ie)
		        {
		            System.out.println("Thread: ArrowheadThread can not be delayed.");
		        }
			}
            
			
		}
	}
}
