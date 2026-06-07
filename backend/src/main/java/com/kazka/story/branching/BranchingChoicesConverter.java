package com.kazka.story.branching;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kazka.story.branching.dto.BranchingChoice;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.List;

@Converter
public class BranchingChoicesConverter implements AttributeConverter<List<BranchingChoice>, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(List<BranchingChoice> attribute) {
        if (attribute == null) return null;
        try { return MAPPER.writeValueAsString(attribute); }
        catch (JsonProcessingException jsonException) { throw new IllegalArgumentException("Cannot serialize choices", jsonException); }
    }

    @Override
    public List<BranchingChoice> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) return null;
        try { return MAPPER.readValue(dbData, new TypeReference<>() {}); }
        catch (JsonProcessingException jsonException) { throw new IllegalArgumentException("Cannot deserialize choices", jsonException); }
    }
}
