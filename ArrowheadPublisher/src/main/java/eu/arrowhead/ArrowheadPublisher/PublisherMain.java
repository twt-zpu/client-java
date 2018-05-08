/*
 *  Copyright (c) 2018 AITIA International Inc.
 *
 *  This work is part of the Productive 4.0 innovation project, which receives grants from the
 *  European Commissions H2020 research and innovation programme, ECSEL Joint Undertaking
 *  (project no. 737459), the free state of Saxony, the German Federal Ministry of Education and
 *  national funding authorities from involved countries.
 */

package eu.arrowhead.ArrowheadPublisher;

import eu.arrowhead.ArrowheadPublisher.common.model.ArrowheadSystem;
import eu.arrowhead.ArrowheadPublisher.common.model.PublishEvent;
import java.time.LocalDateTime;

public class PublisherMain {

  private static boolean isSecure;

  public static void main(String[] args) {
    System.out.println("Working directory: " + System.getProperty("user.dir"));

    String ehAddress = Utility.getProp().getProperty("eh_address", "0.0.0.0");
    int ehInsecurePort = Utility.getProp().getIntProperty("eh_insecure_port", 8454);
    int ehSecurePort = Utility.getProp().getIntProperty("eh_secure_port", 8455);

    for (String arg : args) {
      if (arg.equals("-tls")) {
        isSecure = true;
      }
    }

    String ehUri;
    if (isSecure) {
      ehUri = Utility.getUri(ehAddress, ehSecurePort, "eventhandler/publish", true);
    } else {
      ehUri = Utility.getUri(ehAddress, ehInsecurePort, "eventhandler/publish", false);
    }

    ArrowheadSystem source = new ArrowheadSystem("publisher", "localhost", 8080, null);
    PublishEvent event = new PublishEvent(source, "test", "péééééjlóóód", LocalDateTime.now(), null, "feedback");
    Utility.sendRequest(ehUri, "POST", event);
    System.out.println("Event published to EH.");
  }

}
