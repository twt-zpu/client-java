package eu.arrowhead.common.api;

import javax.ws.rs.client.Entity;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class ArrowheadConverter {
    private static final Map<String, Converter> DEFAULT_CONVERTERS = new HashMap<>();

    public static final String JSON = "JSON";
    public static final String XML = "XML";

    static {
        addDefaultEntityConverter(new FunctionConverter(JSON, "application/json", Entity::json));
        addDefaultEntityConverter(new FunctionConverter(XML, "application/xml", Entity::xml));
    }

    public static void addDefaultEntityConverter(Converter converter) {
        DEFAULT_CONVERTERS.put(converter.getInterface(), converter);
    }

    public static boolean contains(String anInterface) {
        return DEFAULT_CONVERTERS.containsKey(anInterface);
    }

    public static Entity<?> toEntity(String anInterface, Object obj) {
        return DEFAULT_CONVERTERS.get(anInterface).toEntity(obj);
    }

    public static Converter get(String anInterface) {
        return DEFAULT_CONVERTERS.get(anInterface);
    }

    public static abstract class Converter {
        private String anInterface, mediaType;

        public Converter(String anInterface, String mediaType) {
            this.anInterface = anInterface;
            this.mediaType = mediaType;
        }

        public abstract Entity<?> toEntity(Object obj);

        public String getInterface() {
            return anInterface;
        }

        public String getMediaType() {
            return mediaType;
        }
    }

    public static class FunctionConverter extends Converter {

        private final Function<Object, Entity<?>> toEntity;

        public FunctionConverter(String anInterface, String mediaType, Function<Object, Entity<?>> toEntity) {
            super(anInterface, mediaType);
            this.toEntity = toEntity;
        }

        @Override
        public Entity<?> toEntity(Object obj) {
            return toEntity.apply(obj);
        }
    }
}
