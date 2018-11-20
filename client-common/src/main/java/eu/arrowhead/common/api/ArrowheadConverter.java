package eu.arrowhead.common.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import eu.arrowhead.common.exception.ArrowheadRuntimeException;
import eu.arrowhead.common.misc.JacksonJsonProviderAtRest;

import javax.ws.rs.client.Entity;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ArrowheadConverter {
    public static final String JSON = "JSON";
    public static final String XML = "XML";

    private static final Map<String, Converter> DEFAULT_CONVERTERS = new HashMap<>();
    private static final ObjectMapper jsonMapper = JacksonJsonProviderAtRest.getMapper();
    private static final XmlMapper xmlMapper = new XmlMapper();

    static {
        addDefaultEntityConverter(new Converter(JSON, "application/json") {
            @Override
            public <T> Entity<T> toEntity(T object) {
                return Entity.json(object);
            }

            @Override
            public <T> String toString(T object) {
                try {
                    return jsonMapper.writeValueAsString(object);
                } catch (JsonProcessingException e) {
                    throw new ArrowheadRuntimeException("Jackson library threw IOException during JSON serialization! " +
                            "Wrapping it in RuntimeException. Exception message: " + e.getMessage(), e);
                }
            }

            @Override
            public <T> T fromString(String string, Class<T> aClass) {
                try {
                    return jsonMapper.readValue(string.trim(), aClass);
                } catch (IOException e) {
                    throw new ArrowheadRuntimeException("Jackson library threw exception during JSON parsing!", e);
                }
            }
        });
        addDefaultEntityConverter(new Converter(XML, "application/xml") {
            @Override
            public <T> Entity<T> toEntity(T object) {
                return Entity.xml(object);
            }

            @Override
            public <T> String toString(T object) {
                try {
                    return xmlMapper.writeValueAsString(object);
                } catch (JsonProcessingException e) {
                    throw new ArrowheadRuntimeException("Jackson library threw IOException during XML serialization! " +
                            "Wrapping it in RuntimeException. Exception message: " + e.getMessage(), e);
                }
            }

            @Override
            public <T> T fromString(String string, Class<T> aClass) {
                try {
                    return xmlMapper.readValue(string.trim(), aClass);
                } catch (IOException e) {
                    throw new ArrowheadRuntimeException("Jackson library threw exception during JSON parsing!", e);
                }
            }
        });
    }

    public static void addDefaultEntityConverter(Converter converter) {
        DEFAULT_CONVERTERS.put(converter.getInterface(), converter);
    }

    public static boolean contains(String anInterface) {
        return DEFAULT_CONVERTERS.containsKey(anInterface);
    }

    public static <T> Entity<T> toEntity(String anInterface, T object) {
        return DEFAULT_CONVERTERS.get(anInterface).toEntity(object);
    }

    public static <T> String toString(String anInterface, T object) {
        return DEFAULT_CONVERTERS.get(anInterface).toString(object);
    }

    public static <T> T fromString(String anInterface, String string, Class<T> aClass) {
        return DEFAULT_CONVERTERS.get(anInterface).fromString(string, aClass);
    }

    public static Converter get(String anInterface) {
        return DEFAULT_CONVERTERS.get(anInterface);
    }

    public static Converter json() {
        return get(JSON);
    }

    public static abstract class Converter {
        private String anInterface, mediaType;

        public Converter(String anInterface, String mediaType) {
            this.anInterface = anInterface;
            this.mediaType = mediaType;
        }

        public abstract <T> Entity<T> toEntity(T object);

        public abstract <T> String toString(T object);

        public abstract <T> T fromString(String string, Class<T> aClass);

        public String getInterface() {
            return anInterface;
        }

        public String getMediaType() {
            return mediaType;
        }
    }
}
