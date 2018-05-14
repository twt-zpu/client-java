/*
 *  Copyright (c) 2018 AITIA International Inc.
 *
 *  This work is part of the Productive 4.0 innovation project, which receives grants from the
 *  European Commissions H2020 research and innovation programme, ECSEL Joint Undertaking
 *  (project no. 737459), the free state of Saxony, the German Federal Ministry of Education and
 *  national funding authorities from involved countries.
 */

package eu.arrowhead.client.publisher;

import eu.arrowhead.client.common.ArrowheadClientMain;
import eu.arrowhead.client.common.Utility;
import eu.arrowhead.client.common.misc.ClientType;
import eu.arrowhead.client.common.model.ArrowheadSystem;
import eu.arrowhead.client.common.model.Event;
import eu.arrowhead.client.common.model.PublishEvent;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class PublisherMain extends ArrowheadClientMain {

  private PublisherMain(String[] args) {
    Set<Class<?>> classes = new HashSet<>(Collections.singleton(PublisherResource.class));
    String[] packages = {"eu.arrowhead.client.common"};
    init(ClientType.PUBLISHER, args, classes, packages);

    publishEvent();
    listenForInput();
  }

  public static void main(String[] args) {
    new PublisherMain(args);
  }

  private void publishEvent() {
    String ehAddress = props.getProperty("eh_address", "0.0.0.0");
    int ehPort = isSecure ? props.getIntProperty("eh_secure_port", 8455) : props.getIntProperty("eh_insecure_port", 8454);
    String ehUri = Utility.getUri(ehAddress, ehPort, "eventhandler/publish", isSecure, false);

    String systemName = isSecure ? props.getProperty("secure_system_name") : props.getProperty("insecure_system_name");
    String address = props.getProperty("address", "0.0.0.0");
    int insecurePort = props.getIntProperty("insecure_port", ClientType.PUBLISHER.getInsecurePort());
    int securePort = props.getIntProperty("secure_port", ClientType.PUBLISHER.getSecurePort());
    int usedPort = isSecure ? securePort : insecurePort;
    String type = props.getProperty("event_type");
    String payload = props.getProperty("event_payload");

    ArrowheadSystem source = new ArrowheadSystem(systemName, address, usedPort, base64PublicKey);
    Event event = new Event(type, payload, LocalDateTime.now(), null);
    PublishEvent eventPublishing = new PublishEvent(source, event, "publisher/feedback");
    Utility.sendRequest(ehUri, "POST", eventPublishing);
    System.out.println("Event published to EH.");
  }

}
