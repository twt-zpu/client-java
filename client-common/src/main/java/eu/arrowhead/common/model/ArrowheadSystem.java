/*
 *  Copyright (c) 2018 AITIA International Inc.
 *
 *  This work is part of the Productive 4.0 innovation project, which receives grants from the
 *  European Commissions H2020 research and innovation programme, ECSEL Joint Undertaking
 *  (project no. 737459), the free state of Saxony, the German Federal Ministry of Education and
 *  national funding authorities from involved countries.
 */

package eu.arrowhead.common.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import eu.arrowhead.common.api.server.ArrowheadHttpServer;
import eu.arrowhead.common.misc.ArrowheadProperties;
import java.net.URI;
import java.net.URISyntaxException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ArrowheadSystem {
    @JsonIgnore
    protected final Logger log = LogManager.getLogger(getClass());
    private Long id;
    private String systemName;
    private String address;
    private Integer port;
    private String authenticationInfo;

    public static ArrowheadSystem createFromProperties(ArrowheadHttpServer server) {
        return createFromProperties(ArrowheadProperties.loadDefault(), server);
    }

    public static ArrowheadSystem createFromProperties() {
        return createFromProperties(ArrowheadProperties.loadDefault(), null);
    }

    public static ArrowheadSystem createFromProperties(ArrowheadProperties props) {
        return createFromProperties(ArrowheadProperties.loadDefault(), null);
    }

    public static ArrowheadSystem createFromProperties(ArrowheadProperties props, ArrowheadHttpServer server) {
        boolean isSecure = props.isSecure();

        String host = props.getAddress();
        int port = server != null ? server.getPort() : props.getPort();
        if (server != null) {
            try {
                URI uri = new URI(server.getBaseUri());
                host = uri.getHost();
                port = uri.getPort();
            } catch (URISyntaxException e) {
                throw new AssertionError("Parsing the BASE_URI resulted in an error.", e);
            }
        }

        String systemName = props.getSystemName();
        final String authInfo = isSecure && server != null ? server.getBase64PublicKey() : null;

        return new ArrowheadSystem(systemName, host, port, authInfo);
    }

    public ArrowheadSystem() {
    }

    public ArrowheadSystem(String systemName, String address, Integer port, String authenticationInfo) {
        this.systemName = systemName;
        this.address = address;
        this.port = port;
        this.authenticationInfo = authenticationInfo;
    }

    public ArrowheadSystem(String json) {
        String[] fields = json.split(",");
        this.systemName = fields[0].equals("null") ? null : fields[0];

        if (fields.length == 4) {
            this.address = fields[1].equals("null") ? null : fields[1];
            this.port = Integer.valueOf(fields[2]);
            this.authenticationInfo = fields[3].equals("null") ? null : fields[3];
        }
    }

    @SuppressWarnings("CopyConstructorMissesField")
    public ArrowheadSystem(ArrowheadSystem system) {
        this.systemName = system.systemName;
        this.address = system.address;
        this.port = system.port;
        this.authenticationInfo = system.authenticationInfo;
    }

    public Long getId() {
        return id;
    }

    public ArrowheadSystem setId(Long id) {
        this.id = id;
        return this;
    }

    public String getSystemName() {
        return systemName;
    }

    public ArrowheadSystem setSystemName(String systemName) {
        this.systemName = systemName;
        return this;
    }

    public String getAddress() {
        return address;
    }

    public ArrowheadSystem setAddress(String address) {
        this.address = address;
        return this;
    }

    public Integer getPort() {
        return port;
    }

    public ArrowheadSystem setPort(Integer port) {
        this.port = port;
        return this;
    }

    public String getAuthenticationInfo() {
        return authenticationInfo;
    }

    public ArrowheadSystem setAuthenticationInfo(String authenticationInfo) {
        this.authenticationInfo = authenticationInfo;
        return this;
    }

    public String toArrowheadCommonName(String operator, String cloudName) {
        if (systemName.contains(".") || operator.contains(".") || cloudName.contains(".")) {
            throw new IllegalArgumentException("The string fields can not contain dots!");
        }
        //throws NPE if any of the fields are null
        return systemName.concat(".").concat(cloudName).concat(".").concat(operator).concat(".").concat("arrowhead.eu");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ArrowheadSystem)) {
            return false;
        }

        ArrowheadSystem that = (ArrowheadSystem) o;

        if (systemName != null ? !systemName.equals(that.systemName) : that.systemName != null) {
            return false;
        }
        if (address != null ? !address.equals(that.address) : that.address != null) {
            return false;
        }
        return port != null ? port.equals(that.port) : that.port == null;
    }

    @Override
    public int hashCode() {
        int result = systemName != null ? systemName.hashCode() : 0;
        result = 31 * result + (address != null ? address.hashCode() : 0);
        result = 31 * result + (port != null ? port.hashCode() : 0);
        return result;
    }

    //NOTE ArrowheadSystemKeyDeserializer relies on this implementation, do not change it without changing the (String json) constructor
    @Override
    public String toString() {
        return systemName + "," + address + "," + port + "," + authenticationInfo;
    }

    public void partialUpdate(ArrowheadSystem other) {
        this.systemName = other.getSystemName() != null ? other.getSystemName() : this.systemName;
        this.address = other.getAddress() != null ? other.getAddress() : this.address;
        this.port = other.getPort() != null ? other.getPort() : this.port;
        this.authenticationInfo = other.getAuthenticationInfo() != null ? other.getAuthenticationInfo() : this.authenticationInfo;
    }

}
