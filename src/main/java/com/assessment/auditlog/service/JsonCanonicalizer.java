package com.assessment.auditlog.service;

import java.util.Comparator;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.springframework.stereotype.Component;

@Component
public class JsonCanonicalizer {

    private final ObjectMapper objectMapper;

    public JsonCanonicalizer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String canonicalize(JsonNode value) {
        try {
            return objectMapper.writeValueAsString(sortRecursively(value));
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Payload could not be canonicalized", exception);
        }
    }

    private JsonNode sortRecursively(JsonNode value) {
        if (value == null || value.isNull() || value.isValueNode()) {
            return value;
        }
        if (value.isArray()) {
            ArrayNode sortedArray = objectMapper.createArrayNode();
            value.forEach(element -> sortedArray.add(sortRecursively(element)));
            return sortedArray;
        }
        if (value.isObject()) {
            ObjectNode sortedObject = objectMapper.createObjectNode();
            value.properties().stream()
                    .sorted(Comparator.comparing(Map.Entry::getKey))
                    .forEach(entry -> sortedObject.set(entry.getKey(), sortRecursively(entry.getValue())));
            return sortedObject;
        }
        return value;
    }
}
