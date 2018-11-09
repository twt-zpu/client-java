package eu.arrowhead.common.api.clients;

import eu.arrowhead.common.api.ArrowheadSecurityContext;
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

public class EventHandlerClient extends StaticRestClient {
    private static final Map<EventHandlerClient, Map<ArrowheadSystem, Set<String>>> subscriptions = new HashMap<>();
    private StaticRestClient subscriptionClient;
    private StaticRestClient publishClient;

    public static EventHandlerClient createFromProperties(ArrowheadSecurityContext securityContext) {
        return createFromProperties(ArrowheadProperties.loadDefault(), securityContext);
    }

    public static EventHandlerClient createFromProperties(ArrowheadProperties props, ArrowheadSecurityContext securityContext) {
        final boolean isSecure = props.isSecure();
        return new EventHandlerClient(isSecure)
                .setAddress(props.getEhAddress())
                .setPort(props.getEhPort())
                .setSecurityContext(securityContext)
                .replacePath("eventhandler");
    }

    public static EventHandlerClient createDefault(ArrowheadSecurityContext securityContext) {
        final boolean isSecure = ArrowheadProperties.getDefaultIsSecure();
        return new EventHandlerClient(isSecure)
                .setAddress(ArrowheadProperties.getDefaultEhAddress())
                .setPort(ArrowheadProperties.getDefaultEhPort(isSecure))
                .setSecurityContext(securityContext)
                .replacePath("eventhandler");
    }

    private EventHandlerClient(boolean secure) {
        super(secure);
    }

    @Override
    protected EventHandlerClient setAddress(String address) {
        super.setAddress(address);
        return this;
    }

    @Override
    protected EventHandlerClient setPort(int port) {
        super.setPort(port);
        return this;
    }

    @Override
    protected EventHandlerClient setUri(String uri) {
        super.setUri(uri);
        return this;
    }

    @Override
    protected EventHandlerClient setSecure(boolean secure) {
        super.setSecure(secure);
        return this;
    }

    @Override
    protected EventHandlerClient setSecurityContext(ArrowheadSecurityContext securityContext) {
        super.setSecurityContext(securityContext);
        return this;
    }

    public void publish(Event event, ArrowheadSystem eventSource) {
        PublishEvent eventPublishing = new PublishEvent(eventSource, event, "publisher/feedback");
        publishClient.post().send(eventPublishing);
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
        subscriptionClient.post().send(filter);

        if (!subscriptions.containsKey(this)) subscriptions.put(this, new HashMap<>());
        handlerSubscriptions = subscriptions.get(this);
        if (!handlerSubscriptions.containsKey(consumer)) handlerSubscriptions.put(consumer, new HashSet<>());
        handlerSubscriptions.get(consumer).add(eventType);

        log.info("Subscribed to " + eventType + " event types.");
    }

    public void unsubscribe(ArrowheadSystem consumer, String eventType) {
        String consumerName = consumer.getSystemName();

        subscriptionClient.clone("type", eventType, "consumer", consumerName).delete().send();

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

    @Override
    protected EventHandlerClient replacePath(String path) {
        super.replacePath(path);
        subscriptionClient = clone("subscription");
        publishClient = clone("publish");
        return this;
    }

    @Override
    protected EventHandlerClient addPath(String path) {
        super.addPath(path);
        subscriptionClient = clone("subscription");
        publishClient = clone("publish");
        return this;
    }
}