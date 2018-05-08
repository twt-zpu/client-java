/*
 *  Copyright (c) 2018 AITIA International Inc.
 *
 *  This work is part of the Productive 4.0 innovation project, which receives grants from the
 *  European Commissions H2020 research and innovation programme, ECSEL Joint Undertaking
 *  (project no. 737459), the free state of Saxony, the German Federal Ministry of Education and
 *  national funding authorities from involved countries.
 */

package eu.arrowhead.ArrowheadSubscriber.common.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EventFilter {

  private String eventType;
  private ArrowheadSystem consumer;
  private List<ArrowheadSystem> sources = new ArrayList<>();
  private LocalDateTime startDate;
  private LocalDateTime endDate;
  private Map<String, String> filterMetadata = new HashMap<>();
  private Integer port;
  private String notifyUri;
  private Boolean matchMetadata;

  public EventFilter() {
  }

  public EventFilter(String eventType, ArrowheadSystem consumer, String notifyUri) {
    this.eventType = eventType;
    this.consumer = consumer;
    this.notifyUri = notifyUri;
  }

  public EventFilter(String eventType, ArrowheadSystem consumer, List<ArrowheadSystem> sources, LocalDateTime startDate, LocalDateTime endDate,
                     Map<String, String> filterMetadata, Integer port, String notifyUri, Boolean matchMetadata) {
    this.eventType = eventType;
    this.consumer = consumer;
    this.sources = sources;
    this.startDate = startDate;
    this.endDate = endDate;
    this.filterMetadata = filterMetadata;
    this.port = port;
    this.notifyUri = notifyUri;
    this.matchMetadata = matchMetadata;
  }

  public String getEventType() {
    return eventType;
  }

  public void setEventType(String eventType) {
    this.eventType = eventType;
  }

  public ArrowheadSystem getConsumer() {
    return consumer;
  }

  public void setConsumer(ArrowheadSystem consumer) {
    this.consumer = consumer;
  }

  public List<ArrowheadSystem> getSources() {
    return sources;
  }

  public void setSources(List<ArrowheadSystem> sources) {
    this.sources = sources;
  }

  public LocalDateTime getStartDate() {
    return startDate;
  }

  public void setStartDate(LocalDateTime startDate) {
    this.startDate = startDate;
  }

  public LocalDateTime getEndDate() {
    return endDate;
  }

  public void setEndDate(LocalDateTime endDate) {
    this.endDate = endDate;
  }

  public Map<String, String> getFilterMetadata() {
    return filterMetadata;
  }

  public void setFilterMetadata(Map<String, String> filterMetadata) {
    this.filterMetadata = filterMetadata;
  }

  public Integer getPort() {
    return port;
  }

  public void setPort(Integer port) {
    this.port = port;
  }

  public String getNotifyUri() {
    return notifyUri;
  }

  public void setNotifyUri(String notifyUri) {
    this.notifyUri = notifyUri;
  }

  public Boolean getMatchMetadata() {
    return matchMetadata;
  }

  public void setMatchMetadata(Boolean matchMetadata) {
    this.matchMetadata = matchMetadata;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    EventFilter eventFilter = (EventFilter) o;

    if (eventType != null ? !eventType.equals(eventFilter.eventType) : eventFilter.eventType != null) {
      return false;
    }
    if (consumer != null ? !consumer.equals(eventFilter.consumer) : eventFilter.consumer != null) {
      return false;
    }
    if (startDate != null ? !startDate.equals(eventFilter.startDate) : eventFilter.startDate != null) {
      return false;
    }
    return endDate != null ? endDate.equals(eventFilter.endDate) : eventFilter.endDate == null;
  }

  @Override
  public int hashCode() {
    int result = eventType != null ? eventType.hashCode() : 0;
    result = 31 * result + (consumer != null ? consumer.hashCode() : 0);
    result = 31 * result + (startDate != null ? startDate.hashCode() : 0);
    result = 31 * result + (endDate != null ? endDate.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    final StringBuffer sb = new StringBuffer("EventFilter{");
    sb.append("eventType='").append(eventType).append('\'');
    sb.append(", consumer=").append(consumer);
    sb.append('}');
    return sb.toString();
  }

}
