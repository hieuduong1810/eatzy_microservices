package com.eatzy.common.config;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import feign.Response;
import feign.codec.Decoder;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.Map;

/**
 * Custom Feign Decoder that automatically unwraps RestResponse{statusCode, data, message}.
 * 
 * When a Feign client receives a JSON response wrapped in RestResponse format,
 * this decoder extracts the "data" field and deserializes it into the declared return type.
 * 
 * If the response is NOT wrapped (no "data" field or not a JSON object), 
 * it falls back to standard deserialization.
 * 
 * NOTE: This is NOT a Spring @Component. It is instantiated by FeignConfig
 * which is provided as defaultConfiguration to @EnableFeignClients.
 */
public class RestResponseDecoder implements Decoder {

    private final ObjectMapper objectMapper;

    public RestResponseDecoder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Object decode(Response response, Type type) throws IOException, FeignException {
        if (response.body() == null) {
            return null;
        }

        // Read the response body as bytes so we can process it
        byte[] bodyBytes;
        try (InputStream is = response.body().asInputStream()) {
            bodyBytes = is.readAllBytes();
        }

        if (bodyBytes.length == 0) {
            return null;
        }

        // If return type is void, nothing to decode
        if (type == void.class || type == Void.class) {
            return null;
        }

        try {
            // Try to parse as a generic Map first to check for RestResponse wrapper
            @SuppressWarnings("unchecked")
            Map<String, Object> wrapper = objectMapper.readValue(bodyBytes, Map.class);

            // Check if this looks like a RestResponse wrapper (has "data" and "statusCode" keys)
            if (wrapper.containsKey("data") && wrapper.containsKey("statusCode")) {
                Object data = wrapper.get("data");

                // If data is null, return null
                if (data == null) {
                    return null;
                }

                // Convert the data field to the target type
                JavaType javaType = objectMapper.getTypeFactory().constructType(type);
                return objectMapper.convertValue(data, javaType);
            }

            // Not a RestResponse wrapper — deserialize the whole body as the target type
            JavaType javaType = objectMapper.getTypeFactory().constructType(type);
            return objectMapper.readValue(bodyBytes, javaType);

        } catch (Exception e) {
            // If parsing as Map fails (e.g. body is an array or primitive),
            // fall back to direct deserialization
            JavaType javaType = objectMapper.getTypeFactory().constructType(type);
            return objectMapper.readValue(bodyBytes, javaType);
        }
    }
}
