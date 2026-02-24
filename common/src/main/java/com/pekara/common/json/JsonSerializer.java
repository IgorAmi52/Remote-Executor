package com.pekara.common.json;

public interface JsonSerializer {

    String toJson(Object object);

    <T> T fromJson(String json, Class<T> clazz);

    class JsonSerializationException extends RuntimeException {
        public JsonSerializationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
