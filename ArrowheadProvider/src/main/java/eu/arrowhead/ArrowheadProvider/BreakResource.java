package eu.arrowhead.ArrowheadProvider;

import eu.arrowhead.ArrowheadProvider.common.Utility;
import eu.arrowhead.ArrowheadProvider.common.model.ErrorMessage;
import eu.arrowhead.ArrowheadProvider.common.model.RequestVerifying;
import eu.arrowhead.ArrowheadProvider.common.security.AuthenticationException;
import java.io.IOException;
import java.util.Random;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.SecurityContext;
import org.glassfish.jersey.media.sse.EventOutput;
import org.glassfish.jersey.media.sse.OutboundEvent;
import org.glassfish.jersey.media.sse.SseFeature;

import java.io.*;
import java.net.*;


@Path("break")
@Produces(SseFeature.SERVER_SENT_EVENTS)
public class BreakResource {

  @GET
  public EventOutput getServerSentEvents(@Context SecurityContext context, @QueryParam("token") String token, @QueryParam("signature") String signature) {
    final EventOutput eventOutput = new EventOutput();

    if (context.isSecure()) {
      RequestVerifying verifyResponse = Utility.requesterVerified(context, token, signature);
      if (!verifyResponse.isVerified()) {
        ErrorMessage error = new ErrorMessage(verifyResponse.getErrorMessage(), verifyResponse.getStatusCode(), null);
        if (verifyResponse.getStatusCode() == 401) {
          error.setExceptionType(AuthenticationException.class.toString());
        }
        final OutboundEvent.Builder eventBuilder = new OutboundEvent.Builder();
        eventBuilder.name("message-to-client");
        eventBuilder.data(ErrorMessage.class, error);
        final OutboundEvent event = eventBuilder.build();
        try {
          eventOutput.write(event);
        } catch (IOException e) {
          e.printStackTrace();
        }

        return eventOutput;
      }
    }

    new Thread(new Runnable() {
      @Override
      public void run() {
		 DatagramSocket serverSocket = null;	
				   try {
		serverSocket = new DatagramSocket(9876);
	  } catch (Exception e) {}
            byte[] receiveData = new byte[1024];
            while(true)
               {
				   try {
                  DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                  serverSocket.receive(receivePacket);
                //  java.util.Date date= new java.util.Date();
//            System.out.print(new Timestamp(date.getTime()));
//				  String message = date.toString();
				long startTime = System.currentTimeMillis();
				  String message = ""+startTime+"\n";

				  File outfile = new File("timestampEND.txt");
				  if (!outfile.exists()) {
					outfile.createNewFile();
				  }
                  FileWriter fw = new FileWriter(outfile.getAbsoluteFile(), true);
                  BufferedWriter bw = new BufferedWriter(fw);
                  bw.write(message+"\n");
				  bw.flush(); 
				  bw.close();
				   } catch (Exception e) {}
               }
    }}).start();

	
    new Thread(new Runnable() {
      @Override
      public void run() {
        final OutboundEvent.Builder eventBuilder = new OutboundEvent.Builder();
        eventBuilder.name("message-to-client");
        try {
          Random rand = new Random();
          for (int i = 0; i < 10; i++) {
            // Generate a random number between 2 and 5
            int value = rand.nextInt((5 - 2) + 1) + 2;
            System.out.println("delay: " + value);
            Thread.sleep(value * 1000);

				long startTime = System.currentTimeMillis();
				  String message = ""+startTime+"\n";

				  File outfile = new File("timestampSTART.txt");
				  if (!outfile.exists()) {
					outfile.createNewFile();
				  }
                  FileWriter fw = new FileWriter(outfile.getAbsoluteFile(), true);
                  BufferedWriter bw = new BufferedWriter(fw);
                  bw.write(message+"\n");
				  bw.flush(); 
				  bw.close();

//            java.util.Date date= new java.util.Date();
//            System.out.println(date.toString());
            eventBuilder.data(Integer.class, value);
            OutboundEvent breakEvent = eventBuilder.build();
            eventOutput.write(breakEvent);
          }
        } catch (IOException | InterruptedException e) {
          throw new RuntimeException("Error when writing the event.", e);
        } finally {
          try {
            eventOutput.close();
          } catch (IOException ioClose) {
            throw new RuntimeException("Error when closing the event output.", ioClose);
          }
        }
      }
    }).start();

    return eventOutput;
  }

}
