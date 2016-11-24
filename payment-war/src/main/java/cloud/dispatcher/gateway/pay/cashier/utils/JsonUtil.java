package cloud.dispatcher.gateway.pay.cashier.utils;

import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;

public class JsonUtil {

    private static final ObjectMapper mapper = new ObjectMapper();

    static {
        mapper.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true);
        mapper.configure(DeserializationConfig.Feature.AUTO_DETECT_FIELDS, true);
    }

    public static String encode(Object o) {
        try {
            return mapper.writeValueAsString(o);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("rawtypes")
    public static <T> T decode(String json, TypeReference type) {
        try {
            return mapper.readValue(json, type);
        } catch (Exception error) {
            throw new RuntimeException(error);
        }
    }

    public static <T> T decode(String json, Class<T> clazz) {
        try {
            return mapper.readValue(json, clazz);
        } catch (Exception error) {
            throw new RuntimeException(error);
        }
    }
}
