package com.kazka.child;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.List;

@Converter
public class TraitsConverter implements AttributeConverter<List<String>, String> {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(List<String> attribute) {
        if (attribute == null) return "[]";
        try { return MAPPER.writeValueAsString(attribute); }
        catch (JsonProcessingException jsonException) { throw new IllegalArgumentException("Cannot serialize traits", jsonException); }
    }

    @Override
    public List<String> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) return List.of();
        try { return MAPPER.readValue(dbData, new TypeReference<>() {}); }
        catch (JsonProcessingException jsonException) { throw new IllegalArgumentException("Cannot deserialize traits", jsonException); }
    }
}
