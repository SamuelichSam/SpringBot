package com.samuelich.service.impl;

import com.samuelich.model.enums.PlanetaryInfluence;
import com.samuelich.model.enums.ZodiacSign;
import com.samuelich.service.AstrologyCalculationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Slf4j
@Service
public class AstrologyCalculationServiceImpl implements AstrologyCalculationService {


    @Override
    public String calculateZodiacSign(LocalDate birthDate) {
        try {
            ZodiacSign sign = ZodiacSign.fromDate(birthDate);
            return sign.getFullName();
        } catch (Exception e) {
            log.error("Error calculating zodiac sign for date: {}", birthDate, e);
            return "Неизвестно";
        }
    }

    @Override
    public String getPlanetaryInfluence(LocalDate birthDate) {
        try {
            Integer dayOfMonth = birthDate.getDayOfMonth();
            PlanetaryInfluence influence = PlanetaryInfluence.fromDayOfMonth(dayOfMonth);
            return String.format("%s: %s", influence.getName(), influence.getDescription());
        } catch (Exception e) {
            log.error("Error calculating planetary influence for date: {}", birthDate, e);
            return "Стандартное планетарное влияние";
        }
    }

    @Override
    public String getElement(String zodiacSign) {
        try {
            String cleanSign = zodiacSign.replaceAll("[♈♉♊♋♌♍♎♏♐♑♒♓]", "").trim();
            for (ZodiacSign sign : ZodiacSign.values()) {
                if (sign.getName().equalsIgnoreCase(cleanSign)) {
                    return sign.getElement();
                }
            }
            return "Неизвестно";
        } catch (Exception e) {
            log.error("Error getting element for zodiac sign: {}", zodiacSign, e);
            return "Неизвестно";
        }
    }

    @Override
    public String getZodiacSymbol(String zodiacSign) {
        try {
            String cleanSign = zodiacSign.replaceAll("[♈♉♊♋♌♍♎♏♐♑♒♓]", "").trim();
            for (ZodiacSign sign : ZodiacSign.values()) {
                if (sign.getName().equalsIgnoreCase(cleanSign)) {
                    return sign.getSymbol();
                }
            }
            return "☆";
        } catch (Exception e) {
            log.error("Error getting element for zodiac sign: {}", zodiacSign, e);
            return "☆";
        }
    }

    @Override
    public String getHouseInfluence(LocalDate birthDate) {
        return "";
    }
}
