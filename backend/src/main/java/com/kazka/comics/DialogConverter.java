package com.kazka.comics;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.List;

/**
 * Persists a panel's dialog as a JSON array (list of {speaker, line}).
 * Mirrors the project pattern used by {@code CharactersConverter} — the
 * project doesn't use hypersistence-utils or {@code @JdbcTypeCode}, so JSON
 * columns are bound via JPA AttributeConverter to a String column.
 *
 * Null dialog → null in DB. Empty list also persists as null to keep the
 * column tidy ("no dialog" and "empty dialog" are the same thing).
 */
@Converter
class DialogConverter implements AttributeConverter<List<StoryPanel.Dialog>, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<List<StoryPanel.Dialog>> TYPE = new TypeReference<>() {};

    @Override
    public String convertToDatabaseColumn(List<StoryPanel.Dialog> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(attribute);
        } catch (JsonProcessingException jsonException) {
            throw new IllegalArgumentException("Cannot serialize panel dialog", jsonException);
        }
    }

    @Override
    public List<StoryPanel.Dialog> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return List.of();
        }
        try {
            return MAPPER.readValue(dbData, TYPE);
        } catch (JsonProcessingException jsonException) {
            throw new IllegalArgumentException("Cannot deserialize panel dialog", jsonException);
        }
    }
}
