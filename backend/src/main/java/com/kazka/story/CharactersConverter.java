package com.kazka.story;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.List;

@Converter
class CharactersConverter implements AttributeConverter<List<String>, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(List<String> attribute) {
        try {
            return MAPPER.writeValueAsString(attribute);
        } catch (JsonProcessingException jsonException) {
            throw new IllegalArgumentException("Cannot serialize characters", jsonException);
        }
    }

    @Override
    public List<String> convertToEntityAttribute(String dbData) {
        try {
            return MAPPER.readValue(dbData, new TypeReference<>() {});
        } catch (JsonProcessingException jsonException) {
            throw new IllegalArgumentException("Cannot deserialize characters", jsonException);
        }
    }
}
