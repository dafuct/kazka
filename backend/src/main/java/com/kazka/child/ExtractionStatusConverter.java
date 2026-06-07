package com.kazka.child;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class ExtractionStatusConverter implements AttributeConverter<ExtractionStatus, String> {
    @Override
    public String convertToDatabaseColumn(ExtractionStatus attr) {
        return attr == null ? null : attr.name().toLowerCase();
    }

    @Override
    public ExtractionStatus convertToEntityAttribute(String dbValue) {
        return dbValue == null ? null : ExtractionStatus.valueOf(dbValue.toUpperCase());
    }
}
