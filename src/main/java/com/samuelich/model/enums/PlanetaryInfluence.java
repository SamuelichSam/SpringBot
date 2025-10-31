package com.samuelich.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PlanetaryInfluence {

    NEW_MOON("Новолуние", 1, 7,
            "Время новых начинаний и постановки целей. Энергия для старта проектов."),
    WAXING_MOON("Растущая луна", 8, 15,
            "Период роста и развития. Благоприятное время для расширения дел."),
    FULL_MOON("Полнолуние", 16, 22,
            "Пик энергии. Время завершений и осознаний. Эмоции могут быть усилены."),
    WANING_MOON("Убывающая луна", 23, 31,
            "Время подведения итогов, очищения и отдыха.");

    private final String name;
    private final int startDay;
    private final int endDay;
    private final String description;

    public static PlanetaryInfluence fromDayOfMonth(Integer dayOfMonth) {
        for (PlanetaryInfluence influence : values()) {
            if (dayOfMonth >= influence.startDay && dayOfMonth <= influence.endDay) {
                return influence;
            }
        }
        return NEW_MOON;
    }
}
