package eu.arrowhead.BasicConsumer.model;

import java.util.ArrayList;
import java.util.List;

public class OrchestrationResponse {

  private List<OrchestrationForm> response = new ArrayList<>();

  public OrchestrationResponse() {
    super();
  }

  public OrchestrationResponse(List<OrchestrationForm> response) {
    this.response = response;
  }

  public List<OrchestrationForm> getResponse() {
    return response;
  }

  public void setResponse(List<OrchestrationForm> response) {
    this.response = response;
  }

}
