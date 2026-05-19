package com.taskmind.infrastructure.db;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmind.domain.model.Action;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Converter
public class ActionSetConverter implements AttributeConverter<Set<Action>, String> {
    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(Set<Action> actions) {
        if (actions == null) return "[]";
        try {
            Set<String> values = actions.stream().map(Action::getValue).collect(Collectors.toSet());
            return mapper.writeValueAsString(values);
        } catch (IOException e) {
            throw new RuntimeException("Error converting Action set to JSON", e);
        }
    }

    @Override
    public Set<Action> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) return new HashSet<>();
        try {
            Set<String> values = mapper.readValue(dbData, new TypeReference<Set<String>>() {});
            return values.stream().map(Action::fromValue).collect(Collectors.toSet());
        } catch (IOException e) {
            throw new RuntimeException("Error converting JSON to Action set", e);
        }
    }
}
