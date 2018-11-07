package eu.arrowhead.common.api.clients;

import eu.arrowhead.common.api.ArrowheadSecurityContext;
import eu.arrowhead.common.exception.ArrowheadRuntimeException;
import eu.arrowhead.common.misc.ArrowheadProperties;
import eu.arrowhead.common.misc.Utility;
import eu.arrowhead.common.model.ArrowheadSystem;
import eu.arrowhead.common.model.Event;
import eu.arrowhead.common.model.EventFilter;
import eu.arrowhead.common.model.PublishEvent;

import javax.ws.rs.core.UriBuilder;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class EventHandlerClient extends RestClient {
    private static final Map<EventHandlerClient, Map<ArrowheadSystem, Set<String>>> subscriptions = new HashMap<>();
    private String publishUri;
    private String subscribeUri;
    private boolean isSecure;

    public static EventHandlerClient createFromProperties(ArrowheadSecurityContext securityContext) {
        return createFromProperties(Utility.getProp(), securityContext);
    }

    public static EventHandlerClient createFromProperties(ArrowheadProperties props, ArrowheadSecurityContext securityContext) {
        final boolean isSecure = props.isSecure();
        return new EventHandlerClient()
                .setSecure(isSecure)
                .setAddress(props.getEhAddress())
                .setPort(props.getEhPort())
                .setSecurityContext(securityContext);
    }

    public static EventHandlerClient createDefault(ArrowheadSecurityContext securityContext) {
        final boolean isSecure = ArrowheadProperties.getDefaultIsSecure();
        return new EventHandlerClient()
                .setSecure(isSecure)
                .setAddress(ArrowheadProperties.getDefaultEhAddress())
                .setPort(ArrowheadProperties.getDefaultEhPort(isSecure))
                .setSecurityContext(securityContext);
    }

    private EventHandlerClient() {
        super("0.0.0.0", 80);
        isSecure = false;
    }

    public boolean isSecure() {
        return isSecure;
    }

    public EventHandlerClient setSecure(boolean secure) {
        isSecure = secure;
        updateUris();
        return this;
    }

    @Override
    public EventHandlerClient setAddress(String address) {
        super.setAddress(address);
        updateUris();
        return this;
    }

    @Override
    public EventHandlerClient setPort(Integer port) {
        super.setPort(port);
        updateUris();
        return this;
    }

    @Override
    public EventHandlerClient setSecurityContext(ArrowheadSecurityContext securityContext) {
        super.setSecurityContext(securityContext);
        return this;
    }

    private void updateUris() {
        String baseUri = Utility.getUri(getAddress(), getPort(), "eventhandler", isSecure, false);
        publishUri = UriBuilder.fromPath(baseUri).path("publish").toString();
        subscribeUri = UriBuilder.fromPath(baseUri).path("subscription").toString();

    }

    public void publish(Event event, ArrowheadSystem eventSource) {
        PublishEvent eventPublishing = new PublishEvent(eventSource, event, "publisher/feedback");
        sendRequest(publishUri, "POST", eventPublishing);
        log.info("Event published to EH.");
    }

    public void subscribe(String eventType, ArrowheadSystem consumer) {
        subscribe(eventType, consumer, Utility.getProp());
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
        sendRequest(subscribeUri, "POST", filter);

        if (!subscriptions.containsKey(this)) subscriptions.put(this, new HashMap<>());
        handlerSubscriptions = subscriptions.get(this);
        if (!handlerSubscriptions.containsKey(consumer)) handlerSubscriptions.put(consumer, new HashSet<>());
        handlerSubscriptions.get(consumer).add(eventType);

        log.info("Subscribed to " + eventType + " event types.");
    }

    public void unsubscribe(ArrowheadSystem consumer, String eventType) {
        String consumerName = consumer.getSystemName();

        String url = UriBuilder.fromPath(subscribeUri).path("type").path(eventType).path("consumer").path(consumerName).toString();
        sendRequest(url, "DELETE", null);

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