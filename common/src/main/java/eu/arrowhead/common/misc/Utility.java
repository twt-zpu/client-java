/*
 *  Copyright (c) 2018 AITIA International Inc.
 *
 *  This work is part of the Productive 4.0 innovation project, which receives grants from the
 *  European Commissions H2020 research and innovation programme, ECSEL Joint Undertaking
 *  (project no. 737459), the free state of Saxony, the German Federal Ministry of Education and
 *  national funding authorities from involved countries.
 */

package eu.arrowhead.common.misc;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.arrowhead.common.exception.ArrowheadRuntimeException;
import org.apache.log4j.Logger;

import javax.ws.rs.core.UriBuilder;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

//Contains static utility methods for the project, most important one is the sendRequest method!
public final class Utility {
    private static final ObjectMapper mapper = JacksonJsonProviderAtRest.getMapper();

    private Utility() throws AssertionError {
        throw new AssertionError("Arrowhead Common:Utility is a non-instantiable class");
    }

    public static String getUri(String address, int port, String serviceUri, boolean isSecure, boolean serverStart) {
        if (address == null) {
            throw new NullPointerException("Address can not be null (Utility:getUri throws NPE)");
        }

        UriBuilder ub = UriBuilder.fromPath("").host(address);
        if (isSecure) {
            ub.scheme("https");
        } else {
            ub.scheme("http");
        }
        if (port > 0) {
            ub.port(port);
        }
        if (serviceUri != null) {
            ub.path(serviceUri);
        }

        String url = ub.toString();
        try {
            new URI(url);
        } catch (URISyntaxException e) {
            if (serverStart) {
                throw new ServiceConfigurationError(url + " is not a valid URL to start a HTTP server! Please fix the address field in the properties file.");
            } else {
                throw new ArrowheadRuntimeException(url + " is not a valid URL!");
            }
        }

        return url;
    }

    public static String stripEndSlash(String uri) {
        if (uri != null && uri.endsWith("/")) {
            return uri.substring(0, uri.length() - 1);
        }
        return uri;
    }
    //Fetch the request payload directly from the InputStream without a JSON serializer

    public static String getRequestPayload(InputStream is) {
        StringBuilder sb = new StringBuilder();
        String line;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
        } catch (UnsupportedEncodingException e) {
            throw new AssertionError("getRequestPayload InputStreamReader has unsupported character set! Code needs to be changed!", e);
        } catch (IOException e) {
            throw new RuntimeException("IOException occured while reading an incoming request payload", e);
        }

        if (!sb.toString().isEmpty()) {
            String payload = toPrettyJson(sb.toString(), null);
            return payload != null ? payload : "";
        } else {
            return "";
        }
    }

    public static String toPrettyJson(String jsonString, Object obj) {
        try {
            if (jsonString != null) {
                jsonString = jsonString.trim();
                if (jsonString.startsWith("{")) {
                    Object tempObj = mapper.readValue(jsonString, Object.class);
                    return mapper.writeValueAsString(tempObj);
                } else {
                    Object[] tempObj = mapper.readValue(jsonString, Object[].class);
                    return mapper.writeValueAsString(tempObj);
                }
            }
            if (obj != null) {
                return mapper.writeValueAsString(obj);
            }
        } catch (IOException e) {
            throw new ArrowheadRuntimeException(
                    "Jackson library threw IOException during JSON serialization! Wrapping it in RuntimeException. Exception message: " + e.getMessage(), e);
        }
        return null;
    }

    public static <T> T fromJson(String json, Class<T> parsedClass) {
        try {
            return mapper.readValue(json, parsedClass);
        } catch (IOException e) {
            throw new ArrowheadRuntimeException("Jackson library threw exception during JSON parsing!", e);
        }
    }

    public static String loadJsonFromFile(String pathName) {
        StringBuilder sb;
        try {
            File file = new File(pathName);
            FileInputStream is = new FileInputStream(file);

            BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
            br.close();
        } catch (IOException e) {
            throw new RuntimeException(e.getClass().toString() + ": " + e.getMessage(), e);
        }

        if (!sb.toString().isEmpty()) {
            return sb.toString();
        } else {
            return null;
        }
    }

    public static ArrowheadProperties getProp(String fileName) {
        ArrowheadProperties prop = new ArrowheadProperties();
        prop.loadFromFile(fileName);
        return prop;
    }

    public static ArrowheadProperties getProp() {
        final String confDir = ArrowheadProperties.getConfDir();

        ArrowheadProperties prop = new ArrowheadProperties();
        prop.loadFromFile((confDir != null ? confDir + File.separator : "") + "default.conf");
        final String appConf = (confDir != null ? confDir + File.separator : "") + "app.conf";
        if (Files.isReadable(Paths.get(appConf))) {
            prop.loadFromFile(appConf);
        }

        return prop;
    }

    public static void checkProperties(Set<String> propertyNames, List<String> mandatoryProperties) {
        if (mandatoryProperties == null || mandatoryProperties.isEmpty()) {
            return;
        }
        //Arrays.asList() returns immutable lists, so we have to copy it first
        List<String> properties = new ArrayList<>(mandatoryProperties);
        if (!propertyNames.containsAll(mandatoryProperties)) {
            properties.removeIf(propertyNames::contains);
            throw new ServiceConfigurationError("Missing field(s) from config file: " + properties.toString());
        }
    }

    public static boolean isHostAvailable(String host, int port, int timeout) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), timeout);
            return true;
        } catch (IOException e) {
            return false; // Either timeout or unreachable or failed DNS lookup.
        }
    }

    public static String getRandomPassword() {
        PasswordGenerator generator = new PasswordGenerator.Builder().useDigits(true).useLower(true).useUpper(true).usePunctuation(false).build();
        return generator.generate(12);
    }

    public static boolean isBlank(final String str) {
        return (str == null || "".equals(str.trim()));
    }

    /**
     Updates the given properties file with the given key-value pairs.
     */
    public static void updateConfigurationFiles(String configLocation, Map<String, String> configValues) {
        try {
            final Path path = Paths.get(configLocation);
            ArrowheadProperties props = new ArrowheadProperties();

            if (Files.exists(path)) {
                FileInputStream in = new FileInputStream(configLocation);
                props.load(in);
                in.close();
            } else {
                Files.createFile(path);
            }

            FileOutputStream out = new FileOutputStream(configLocation);
            for (Map.Entry<String, String> entry : configValues.entrySet()) {
                props.setProperty(entry.getKey(), entry.getValue());
            }
            props.store(out, null);
            out.close();
        } catch (IOException e) {
            throw new ArrowheadRuntimeException("IOException during configuration file update", e);
        }
    }
}
