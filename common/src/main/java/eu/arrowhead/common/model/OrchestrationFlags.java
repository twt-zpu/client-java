package eu.arrowhead.common.model;

import java.util.HashMap;

public class OrchestrationFlags extends HashMap<String, Boolean> {
  public enum Flags {
    TRIGGER_INTER_CLOUD("triggerInterCloud"),
    EXTERNAL_SERVICE_REQUEST("externalServiceRequest"),
    ENABLE_INTER_CLOUD("enableInterCloud"),
    METADATA_SEARCH("metadataSearch"),
    PING_PROVIDERS("pingProviders"),
    OVERRIDE_STORE("overrideStore"),
    MATCHMAKING("matchmaking"),
    ONLY_PREFERRED("onlyPreferred"),
    ENABLE_QOS("enableQoS"),
    ;

    private final String flag;

    Flags(final String flag) {
      this.flag = flag;
    }

    @Override
    public String toString() {
      return flag;
    }
  }

  public OrchestrationFlags() {
    for (Flags flag : Flags.values()) {
      put(flag, false);
    }
  }

  public Boolean get(Flags flag) {
    return get(flag.toString());
  }

  public Boolean put(Flags flag, Boolean value) {
    return super.put(flag.toString(), value);
  }
}
