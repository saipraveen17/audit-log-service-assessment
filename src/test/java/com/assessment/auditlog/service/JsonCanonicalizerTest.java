package com.assessment.auditlog.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class JsonCanonicalizerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final JsonCanonicalizer canonicalizer = new JsonCanonicalizer(objectMapper);

    @Test
    void canonicalizesObjectsWithDifferentFieldOrderEqually() throws Exception {
        JsonNode first = objectMapper.readTree("""
                {"z":true,"a":{"b":2,"a":1},"items":[{"d":4,"c":3}]}
                """);
        JsonNode second = objectMapper.readTree("""
                {"items":[{"c":3,"d":4}],"a":{"a":1,"b":2},"z":true}
                """);

        assertThat(canonicalizer.canonicalize(first))
                .isEqualTo(canonicalizer.canonicalize(second))
                .isEqualTo("{\"a\":{\"a\":1,\"b\":2},\"items\":[{\"c\":3,\"d\":4}],\"z\":true}");
    }
}
