package com.taskmind.infrastructure.db;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.taskmind.domain.model.DiscussionBlock;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.List;

@Converter
public class DiscussionListConverter implements AttributeConverter<List<DiscussionBlock>, String> {

    private static final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Override
    public String convertToDatabaseColumn(List<DiscussionBlock> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error serializing discussion blocks to JSON", e);
        }
    }

    @Override
    public List<DiscussionBlock> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank() || dbData.equals("[]")) {
            return List.of();
        }
        try {
            return objectMapper.readValue(dbData, new TypeReference<List<DiscussionBlock>>() {});
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error deserializing discussion blocks from JSON", e);
        }
    }
}
