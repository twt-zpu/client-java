package eu.arrowhead.ArrowheadProvider;

import eu.arrowhead.ArrowheadProvider.common.Utility;
import eu.arrowhead.ArrowheadProvider.common.model.ErrorMessage;
import eu.arrowhead.ArrowheadProvider.common.model.RequestVerifying;
import eu.arrowhead.ArrowheadProvider.common.security.AuthenticationException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
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

@Path("break")
@Produces(SseFeature.SERVER_SENT_EVENTS)
public class BreakResource {

  @GET
  public EventOutput getServerSentEvents(@Context SecurityContext context, @QueryParam("token") String token,
                                         @QueryParam("signature") String signature) {
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
      }
    }

    SimpleDateFormat sdf = new SimpleDateFormat("yyyy MMM dd HH:mm:ss");

    new Thread(new Runnable() {
      public void run() {
        try {
          Random rand = new Random();
          while (true) {
            // Generate a random number between 5 and 30
            int value = rand.nextInt((30 - 5) + 1) + 5;
            Thread.sleep(value * 1000);

            Date timestamp = new Date(System.currentTimeMillis());
            final OutboundEvent.Builder eventBuilder = new OutboundEvent.Builder();
            eventBuilder.name("message-to-client");
            eventBuilder.data(String.class, "BREAK! " + sdf.format(timestamp));
            final OutboundEvent event = eventBuilder.build();
            eventOutput.write(event);

            System.out.println("BREAK! " + sdf.format(timestamp));
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
