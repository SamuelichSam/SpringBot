package com.samuelich.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CallbackType {
    NEW_QUESTION("new_question"),
    GENERATE_IMAGE("generate_image"),
    SETTINGS("settings"),
    ASTROLOGY("astrology");

    private final String value;

    public static CallbackType fromValue(String value) {
        for (CallbackType type : values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown callback type: " + value);
    }
}