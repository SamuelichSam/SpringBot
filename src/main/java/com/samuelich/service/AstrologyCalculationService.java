package com.samuelich.service;

import java.time.LocalDate;

public interface AstrologyCalculationService {

    String calculateZodiacSign(LocalDate birthDate);

    String getPlanetaryInfluence(LocalDate birthDate);

    String getElement(String zodiacSign);

    String getZodiacSymbol(String zodiacSign);

    String getHouseInfluence(LocalDate birthDate);
}
