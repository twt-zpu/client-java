package eu.arrowhead.common.misc;

import eu.arrowhead.common.model.ServiceMetadata;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.util.*;

public class ArrowheadProperties extends TypeSafeProperties {

    private enum Keys {
        SECURE("secure"),
        KEYSTORE("keystore"),
        KEYSTOREPASS("keystorepass"),
        KEYPASS("keypass"),
        TRUSTSTORE("truststore"),
        TRUSTSTOREPASS("truststorepass"),
        SYSTEM_NAME("system_name"),
        ADDRESS("address"),
        PORT("port"),
        SR_ADDRESS("sr_address"),
        SR_PORT("sr_port"),
        ORCH_ADDRESS("orch_address"),
        ORCH_PORT("orch_port"),
        EH_ADDRESS("eh_address"),
        EH_PORT("eh_port"),
        CA_ADDRESS("ca_address"),
        CA_PORT("ca_port"),
        AUTH_PUB("auth_pub"),
        SERVICE_URI("service_uri"),
        SERVICE_NAME("service_name"),
        INTERFACES("interfaces"),
        SERVICE_METADATA("service_metadata"),
        EVENT_TYPE("event_type"),
        NOTIFY_URI("notify_uri"),
        CERT_DIR("cert_dir"),
        BOOTSTRAP("bootstrap"),
        ;
        private final String key;

        Keys(final String key) {
            this.key = key;
        }

        @Override
        public String toString() {
            return key;
        }

    }
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
        return "authorization.pub";
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

    public static boolean getDefaultBootstrap() {
        return false;
    }

    public static ArrowheadProperties load(String fileName) {
        ArrowheadProperties prop = new ArrowheadProperties();
        prop.loadFromFile(fileName);
        return prop;
    }

    public static ArrowheadProperties loadDefault() {
        final String confDir = getConfDir();

        ArrowheadProperties prop = new ArrowheadProperties();
        prop.loadFromFile((confDir != null ? confDir + File.separator : "") + "default.conf");
        final String appConf = (confDir != null ? confDir + File.separator : "") + "app.conf";
        if (Files.isReadable(Paths.get(appConf))) {
            prop.loadFromFile(appConf);
        }

        return prop;
    }

    private int getIntProperty(Keys key, int defaultValue) {
        return super.getIntProperty(key.toString(), defaultValue);
    }

    private boolean getBooleanProperty(Keys key, boolean defaultValue) {
        return super.getBooleanProperty(key.toString(), defaultValue);
    }

    private String getProperty(Keys key) {
        return super.getProperty(key.toString());
    }

    private String getProperty(Keys key, String defaultValue) {
        return super.getProperty(key.toString(), defaultValue);
    }

    private synchronized Object setProperty(Keys key, String value) {
        return super.setProperty(key.toString(), value);
    }

    public boolean containsKey(Keys key) {
        return super.containsKey(key.toString());
    }

    public boolean isSecure() {
        return getBooleanProperty(Keys.SECURE, getDefaultIsSecure());
    }

    public String getKeystore() {
        final String certDir = getCertDir();
        return (certDir != null ? certDir + File.separator : "") + getProperty(Keys.KEYSTORE, getDefaultKeyStore(getSystemName()));
    }

    public ArrowheadProperties setKeystore(String keystore) {
        setProperty(Keys.KEYSTORE, keystore);
        return this;
    }

    public String getKeystorePass() {
        return getProperty(Keys.KEYSTOREPASS);
    }

    public ArrowheadProperties setKeystorePass(String keystorePass) {
        setProperty(Keys.KEYSTOREPASS, keystorePass);
        return this;
    }

    public String getKeyPass() {
        return getProperty(Keys.KEYPASS);
    }

    public ArrowheadProperties setKeyPass(String keyPass) {
        setProperty(Keys.KEYPASS, keyPass);
        return this;
    }

    public String getTruststore() {
        final String certDir = getCertDir();
        return (certDir != null ? certDir + File.separator : "") + getProperty(Keys.TRUSTSTORE, getDefaultTruststore(getSystemName()));
    }

    public ArrowheadProperties setTruststore(String truststore) {
        setProperty(Keys.TRUSTSTORE, truststore);
        return this;
    }

    public String getTruststorePass() {
        return getProperty(Keys.TRUSTSTOREPASS);
    }

    public ArrowheadProperties setTruststorePass(String truststorePass) {
        setProperty(Keys.TRUSTSTOREPASS, truststorePass);
        return this;
    }

    public String getSystemName() {
        return getProperty(Keys.SYSTEM_NAME);
    }

    public String getAddress() {
        return getProperty(Keys.ADDRESS, getDefaultAddress());
    }

    public int getPort() {
        return getIntProperty(Keys.PORT, getDefaultPort(isSecure()));
    }

    public String getSrAddress() {
        return getProperty(Keys.SR_ADDRESS, getDefaultSrAddress());
    }

    public int getSrPort() {
        return getIntProperty(Keys.SR_PORT, getDefaultSrPort(isSecure()));
    }

    public String getOrchAddress() {
        return getProperty(Keys.ORCH_ADDRESS, getDefaultOrchAddress());
    }

    public int getOrchPort() {
        return getIntProperty(Keys.ORCH_PORT, getDefaultOrchPort(isSecure()));
    }

    public String getEhAddress() {
        return getProperty(Keys.EH_ADDRESS, getDefaultEhAddress());
    }

    public int getEhPort() {
        return getIntProperty(Keys.EH_PORT, getDefaultEhPort(isSecure()));
    }

    public String getCaAddress() {
        return getProperty(Keys.CA_ADDRESS, getDefaultCaAddress());
    }

    public int getCaPort() {
        return getIntProperty(Keys.CA_PORT, getDefaultCaPort(isSecure()));
    }

    public String getAuthKey() {
        final String certDir = getCertDir();
        return (certDir != null ? certDir + File.separator : "") + getProperty(Keys.AUTH_PUB, getDefaultAuthKey());
    }

    public ArrowheadProperties setAuthKey(String authKey) {
        setProperty(Keys.AUTH_PUB, authKey);
        return this;
    }

    public String getServiceUri() {
        return getProperty(Keys.SERVICE_URI);
    }

    public String getServiceName() {
        return getProperty(Keys.SERVICE_NAME);
    }

    public Set<String> getInterfaces() {
        String interfaceList = getProperty(Keys.INTERFACES);
        Set<String> interfaces = new HashSet<>();
        if (interfaceList != null && !interfaceList.isEmpty()) {
            interfaces.addAll(Arrays.asList(interfaceList.replaceAll("\\s+", "").split(",")));
        }
        return interfaces;
    }

    public ServiceMetadata getServiceMetadata() {
        ServiceMetadata metadata = new ServiceMetadata();
        String metadataString = getProperty(Keys.SERVICE_METADATA);
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
        return getProperty(Keys.EVENT_TYPE);
    }

    public String getNotifyUri() {
        return getProperty(Keys.NOTIFY_URI, getDefaultNotifyUri());
    }

    public String getCertDir() {
        return getProperty(Keys.CERT_DIR, getDefaultCertDir());
    }

    public boolean isBootstrap() {
        return getBooleanProperty(Keys.BOOTSTRAP, getDefaultBootstrap());
    }

    public void checkProperties(List<String> mandatoryProperties) {
        if (mandatoryProperties == null || mandatoryProperties.isEmpty()) {
            return;
        }

        final Set<Object> propertyNames = keySet();

        //Arrays.asList() returns immutable lists, so we have to copy it first
        List<String> properties = new ArrayList<>(mandatoryProperties);
        if (!propertyNames.containsAll(mandatoryProperties)) {
            properties.removeIf(propertyNames::contains);
            throw new ServiceConfigurationError("Missing field(s) from config file: " + properties.toString());
        }
    }

}
