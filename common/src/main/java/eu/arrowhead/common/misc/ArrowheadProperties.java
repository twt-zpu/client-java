package eu.arrowhead.common.misc;

import java.time.ZonedDateTime;

public class ArrowheadProperties extends TypeSafeProperties {
    public static boolean getDefaultIsSecure() {
        return true;
    }

    public static String getDefaultKeyStore(String systemName) {
        return systemName + ".p12";
    }

    public static String getDefaultTruststore(String systemName) {
        return systemName + ".p12";
    }

    public static String getDefaultAddress() {
        return "0.0.0.0";
    }

    public static int getDefaultPort(boolean secure) {
        return secure ? 443 : 80;
    }

    public static String getDefaultSrAddress() {
        return "0.0.0.0";
    }

    public static int getDefaultSrPort(boolean secure) {
        return secure ? 8443 : 8442;
    }

    public static String getDefaultOrchAddress() {
        return "0.0.0.0";
    }

    public static int getDefaultOrchPort(boolean secure) {
        return secure ? 8441 : 8440;
    }

    public static String getDefaultEhAddress() {
        return "0.0.0.0";
    }

    public static int getDefaultEhPort(boolean secure) {
        return secure ? 8455 : 8454;
    }

    public static String getDefaultCaAddress() {
        return "0.0.0.0";
    }

    public static int getDefaultCaPort(boolean secure) {
        return secure ? 8459 : 8458;
    }

    public static String getDefaultAuthKey() {
        return "authorization.pub";
    }

    public static String createDefaultSystemName() {
        return "unknown-" + ZonedDateTime.now().toEpochSecond();
    }

    public static String getDefaultNotifyUri() {
        return "subscriber/notify";
    }

    public boolean isSecure() {
        return getBooleanProperty("secure", getDefaultIsSecure());
    }

    public String getKeystore() {
        return getProperty("keystore", getDefaultKeyStore(getSystemName()));
    }

    public String getKeystorePass() {
        return getProperty("keystorepass");
    }

    public String getKeyPass() {
        return getProperty("keypass");
    }

    public String getTruststore() {
        return getProperty("truststore", getDefaultTruststore(getSystemName()));
    }

    public String getTruststorePass() {
        return getProperty("truststorepass");
    }

    public String getSystemName() {
        return getProperty("system_name");
    }

    public String getAddress() {
        return getProperty("address", getDefaultAddress());
    }

    public int getPort() {
        return getIntProperty("port", getDefaultPort(isSecure()));
    }

    public String getSrAddress() {
        return getProperty("sr_address", getDefaultSrAddress());
    }

    public int getSrPort() {
        return getIntProperty("sr_port", getDefaultSrPort(isSecure()));
    }

    public String getOrchAddress() {
        return getProperty("orch_address", getDefaultOrchAddress());
    }

    public int getOrchPort() {
        return getIntProperty("orch_port", getDefaultOrchPort(isSecure()));
    }

    public String getEhAddress() {
        return getProperty("eh_address", getDefaultEhAddress());
    }

    public int getEhPort() {
        return getIntProperty("eh_port", getDefaultEhPort(isSecure()));
    }

    public String getCaAddress() {
        return getProperty("ca_address", getDefaultCaAddress());
    }

    public int getCaPort() {
        return getIntProperty("ca_port", getDefaultCaPort(isSecure()));
    }

    public String getAuthKey() { return getProperty("auth_pub", getDefaultAuthKey()); }

    public String getServiceUri() {
        return getProperty("service_uri");
    }

    public String getServiceName() {
        return getProperty("service_name");
    }

    public String getInterfaces() {
        return getProperty("interfaces");
    }

    public String getMetadata() {
        return getProperty("metadata");
    }

    public String getEventType() {
        return getProperty("event_type");
    }

    public String getNotifyUri() {
        return getProperty("notify_uri", getDefaultNotifyUri());
    }
}
