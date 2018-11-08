package eu.arrowhead.common.model;

import java.util.HashMap;

public class ServiceMetadata extends HashMap<String, String> {

    public static ServiceMetadata createDefault(boolean secure) {
        return secure ?
        new ServiceMetadata().setSecurity(Security.TOKEN) :
        new ServiceMetadata();
    }

    public enum Keys {
        UNIT("unit"),
        SECURITY("security");

        private final String key;

        Keys(final String key) {
            this.key = key;
        }

        @Override
        public String toString() {
            return key;
        }
    }

    public enum Security {
        TOKEN("token");

        private final String value;

        Security(final String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }
    }

    public String get(Keys key) {
        return get(key.toString());
    }

    public ServiceMetadata put(Keys key, String value) {
        put(key.toString(), value);
        return this;
    }

    public boolean containsKey(Keys key) {
        return containsKey(key.toString());
    }

    public ServiceMetadata setSecurity(Security value) {
        put(Keys.SECURITY, value.toString());
        return this;
    }
}
