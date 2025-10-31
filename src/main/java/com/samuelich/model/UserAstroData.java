package com.samuelich.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@Service
@AllArgsConstructor
@NoArgsConstructor
public class UserAstroData {
    private Long chatId;
    private LocalDate birthDate;
    private LocalTime birthTime;
    private String birthPlace;
    private String zodiacSign;
    private String moonSign;
    private String risingSign;

    public UserAstroData(Long chatId) {
        this.chatId = chatId;
    }
}
