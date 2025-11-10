package com.samuelich.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserState {
    DEFAULT(""),
    AWAITING_IMAGE_PROMPT("AWAITING_IMAGE_PROMPT"),
    AWAITING_ZODIAC_SIGN("AWAITING_ZODIAC_SIGN"),
    AWAITING_BIRTH_DATE("AWAITING_BIRTH_DATE");

    private final String value;
}
