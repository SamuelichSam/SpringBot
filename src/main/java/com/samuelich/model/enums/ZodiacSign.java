package com.samuelich.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.time.Month;

@Getter
@RequiredArgsConstructor
public enum ZodiacSign {

    ARIES("Овен", "♈", "Огонь", 21, Month.MARCH, 19, Month.APRIL),
    TAURUS("Телец", "♉", "Земля", 20, Month.APRIL, 20, Month.MAY),
    GEMINI("Близнецы", "♊", "Воздух", 21, Month.MAY, 20, Month.JUNE),
    CANCER("Рак", "♋", "Вода", 21, Month.JUNE, 22, Month.JULY),
    LEO("Лев", "♌", "Огонь", 23, Month.JULY, 22, Month.AUGUST),
    VIRGO("Дева", "♍", "Земля", 23, Month.AUGUST, 22, Month.SEPTEMBER),
    LIBRA("Весы", "♎", "Воздух", 23, Month.SEPTEMBER, 22, Month.OCTOBER),
    SCORPIO("Скорпион", "♏", "Вода", 23, Month.OCTOBER, 21, Month.NOVEMBER),
    SAGITTARIUS("Стрелец", "♐", "Огонь", 22, Month.NOVEMBER, 21, Month.DECEMBER),
    CAPRICORN("Козерог", "♑", "Земля", 22, Month.DECEMBER, 19, Month.JANUARY),
    AQUARIUS("Водолей", "♒", "Воздух", 20, Month.JANUARY, 18, Month.FEBRUARY),
    PISCES("Рыбы", "♓", "Вода", 19, Month.FEBRUARY, 20, Month.MARCH);

    private final String name;
    private final String symbol;
    private final String element;
    private final Integer startDay;
    private final Month startMonth;
    private final Integer endDay;
    private final Month endMonth;

    public static ZodiacSign fromDate(LocalDate date) {
        Integer day = date.getDayOfMonth();
        Month month = date.getMonth();
        for (ZodiacSign sign : values()) {
            if ((month == sign.startMonth && day >= sign.startDay) ||
                    (month == sign.endMonth && day <= sign.endDay)) {
                return sign;
            }
        }
        return CAPRICORN;
    }

    public String getFullName() {
        return name + " " + symbol;
    }
}
