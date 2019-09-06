package eu.arrowhead.client.provider;

public class FeldbusCouplerProviderApp {
	private static FeldbusProviderMain provider;
	private static SimpleMasterTCP master;
	
	public static void main(String[] args){
		provider = new FeldbusProviderMain(args);
		master = new SimpleMasterTCP();
		
		// start provider
		new ProviderThread().start();
		
		// start master
		master.setModbusMaster();
		//new MasterThread().start();
		
	}
	
	
	public static class ProviderThread extends Thread{
		@Override public void run(){
			provider.startProvider();
		}
	}
	
	public static class MasterThread extends Thread{
		@Override public void run(){
			while(true){
				long startTime = System.currentTimeMillis();
				master.readMaster();
				master.writeMaster();
				long endTime = System.currentTimeMillis();
				long time = endTime - startTime;
				if (time > 10){
					continue;
				}
				try {
		            Thread.sleep(10 - time);
		        } catch (InterruptedException ie)
		        {
		            System.out.println("Thread: MasterThread can not be delayed.");
		        }
			}
		}
	}
	
}
