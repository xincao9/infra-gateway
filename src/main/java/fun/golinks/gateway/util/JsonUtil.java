package fun.golinks.gateway.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class JsonUtil {

    public static boolean isJson(String content) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.readTree(content);
            return true;
        } catch (JsonProcessingException e) {
            return false;
        }
    }

    public static String toJson(Object object) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            log.error("", e);
            return null;
        }
    }

    public static <T> T toObject(String content, Class<T> clazz) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(content, clazz);
        } catch (JsonProcessingException e) {
            log.error("", e);
            return null;
        }
    }

    public static <T> List<T> toArray(String content, Class<T> clazz) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(content, objectMapper.getTypeFactory().constructCollectionType(List.class, clazz));
        } catch (JsonProcessingException e) {
            log.error("", e);
            return null;
        }
    }
}
