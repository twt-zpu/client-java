package eu.arrowhead.common.misc;

import eu.arrowhead.common.model.ServiceMetadata;

import java.io.File;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class ArrowheadProperties extends TypeSafeProperties {
    public static String getConfDir() {
        return System.getProperty("confDir");
    }

    public static boolean getDefaultIsSecure() {
        return true;
    }

    public static String getDefaultKeyStore(String systemName) {
        return getDefaultCertDir() + File.separator + systemName + ".p12";
    }

    public static String getDefaultTruststore(String systemName) {
        return getDefaultCertDir() + File.separator + systemName + ".p12";
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
        return getDefaultCertDir() + File.separator + "authorization.pub";
    }

    public static String createDefaultSystemName() {
        return "unknown-" + ZonedDateTime.now().toEpochSecond();
    }

    public static String getDefaultNotifyUri() {
        return "subscriber/notify";
    }

    public static String getDefaultCertDir() {
        return "config/certificates";
    }

    public boolean isSecure() {
        return getBooleanProperty("secure", getDefaultIsSecure());
    }

    public String getKeystore() {
        final String certDir = getCertDir();
        return (certDir != null ? certDir + File.separator : "") + getProperty("keystore", getDefaultKeyStore(getSystemName()));
    }

    public String getKeystorePass() {
        return getProperty("keystorepass");
    }

    public String getKeyPass() {
        return getProperty("keypass");
    }

    public String getTruststore() {
        final String certDir = getCertDir();
        return (certDir != null ? certDir + File.separator : "") + getProperty("truststore", getDefaultTruststore(getSystemName()));
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

    public String getAuthKey() {
        final String certDir = getCertDir();
        return (certDir != null ? certDir + File.separator : "") + getProperty("auth_pub", getDefaultAuthKey());
    }

    public String getServiceUri() {
        return getProperty("service_uri");
    }

    public String getServiceName() {
        return getProperty("service_name");
    }

    public Set<String> getInterfaces() {
        String interfaceList = getProperty("interfaces");
        Set<String> interfaces = new HashSet<>();
        if (interfaceList != null && !interfaceList.isEmpty()) {
            interfaces.addAll(Arrays.asList(interfaceList.replaceAll("\\s+", "").split(",")));
        }
        return interfaces;
    }

    public ServiceMetadata getServiceMetadata() {
        ServiceMetadata metadata = new ServiceMetadata();
        String metadataString = getProperty("service_metadata");
        if (metadataString != null && !metadataString.isEmpty()) {
            String[] parts = metadataString.split(",");
            for (String part : parts) {
                String[] pair = part.split("-");
                metadata.put(pair[0], pair[1]);
            }
        }
        return metadata;
    }

    public String getEventType() {
        return getProperty("event_type");
    }

    public String getNotifyUri() {
        return getProperty("notify_uri", getDefaultNotifyUri());
    }

    public String getCertDir() {
        return getProperty("cert_dir", getDefaultCertDir());
    }
}
