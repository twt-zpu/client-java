package eu.arrowhead.common.api.clients.core;

import eu.arrowhead.common.api.ArrowheadSecurityContext;
import eu.arrowhead.common.api.clients.StaticHttpClient;
import eu.arrowhead.common.exception.ArrowheadRuntimeException;
import eu.arrowhead.common.misc.ArrowheadProperties;
import eu.arrowhead.common.model.ArrowheadSystem;
import eu.arrowhead.common.model.Event;
import eu.arrowhead.common.model.EventFilter;
import eu.arrowhead.common.model.PublishEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class EventHandlerClient extends StaticHttpClient {
    private static final Map<EventHandlerClient, Map<ArrowheadSystem, Set<String>>> subscriptions = new HashMap<>();

    public static EventHandlerClient createFromProperties(ArrowheadSecurityContext securityContext) {
        return createFromProperties(ArrowheadProperties.loadDefault(), securityContext);
    }

    public static EventHandlerClient createFromProperties(ArrowheadProperties props, ArrowheadSecurityContext securityContext) {
        final boolean secure = props.isSecure();
        return new EventHandlerClient(secure, securityContext, props.getEhAddress(),props.getEhPort());
    }

    public static EventHandlerClient createDefault(ArrowheadSecurityContext securityContext) {
        final boolean isSecure = ArrowheadProperties.getDefaultIsSecure();
        return new EventHandlerClient(isSecure, securityContext, ArrowheadProperties.getDefaultEhAddress(),
                ArrowheadProperties.getDefaultEhPort(isSecure));
    }

    public EventHandlerClient(boolean secure, ArrowheadSecurityContext securityContext, String host, int port) {
        super(secure, securityContext, host, port, "eventhandler");
    }

    public void publish(Event event, ArrowheadSystem eventSource) {
        PublishEvent eventPublishing = new PublishEvent(eventSource, event, "publisher/feedback");
        post().path("publish").send(eventPublishing);
        log.info("Event published to EH.");
    }

    public void subscribe(String eventType, ArrowheadSystem consumer) {
        subscribe(eventType, consumer, ArrowheadProperties.loadDefault());
    }

    public void subscribe(String eventType, ArrowheadSystem consumer, ArrowheadProperties props) {
        subscribe(eventType, consumer, props.getNotifyUri());
    }

    public void subscribe(String eventType, ArrowheadSystem consumer, String notifyPath) {
        Map<ArrowheadSystem, Set<String>> handlerSubscriptions = subscriptions.get(this);
        final Set<String> consumerSubscriptions = handlerSubscriptions != null ? handlerSubscriptions.get(consumer) : null;

        if (consumerSubscriptions != null && consumerSubscriptions.contains(eventType))
            throw new ArrowheadRuntimeException("Already subscribed to " + eventType);

        EventFilter filter = new EventFilter(eventType, consumer, notifyPath);
        post().path("subscription").send(filter);

        if (!subscriptions.containsKey(this)) subscriptions.put(this, new HashMap<>());
        handlerSubscriptions = subscriptions.get(this);
        if (!handlerSubscriptions.containsKey(consumer)) handlerSubscriptions.put(consumer, new HashSet<>());
        handlerSubscriptions.get(consumer).add(eventType);

        log.info("Subscribed to " + eventType + " event types.");
    }

    public void unsubscribe(ArrowheadSystem consumer, String eventType) {
        String consumerName = consumer.getSystemName();

        delete().path("subscription").path("type").path(eventType).path("consumer").path(consumerName).send();

        final Map<ArrowheadSystem, Set<String>> handlerSubscriptions = subscriptions.get(this);
        final Set<String> consumerSubscriptions = handlerSubscriptions != null ? handlerSubscriptions.get(consumer) : null;
        if (consumerSubscriptions != null) consumerSubscriptions.remove(eventType);

        log.info("Unsubscribed from " + eventType + " event types.");
    }

    public void unsubscribe(ArrowheadSystem consumer, Set<String> subscriptions) {
        subscriptions.forEach(eventType -> this.unsubscribe(consumer, eventType));
    }

    public void unsubscribe(Map<ArrowheadSystem, Set<String>> subscriptions) {
        subscriptions.forEach(this::unsubscribe);
    }

    public static void unsubscribeAll() {
        subscriptions.forEach(EventHandlerClient::unsubscribe);
    }
}