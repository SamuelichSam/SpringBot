package com.samuelich.service.impl;

import com.samuelich.model.enums.UserState;
import com.samuelich.service.ImageService;
import com.samuelich.service.KeyboardService;
import com.samuelich.service.MessageHandlerService;
import com.samuelich.service.YandexGptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@Service
public class MessageHandlerServiceImpl implements MessageHandlerService {

    private final YandexGptService yandexGptService;
    private final KeyboardService keyboardService;
    private final ImageService imageService;

    @Override
    public SendMessage handleRegularMessage(String message, Long chatId) {
        String response = yandexGptService.generateResponse(message);
        return createMessageWithKeyboard(chatId, response);
    }

    @Override
    public SendMessage handleAstrologyRequest(String zodiacSign, Long chatId, String firstName) {
        LocalDate now = LocalDate.now();
        String currentMonth = now.format(DateTimeFormatter.ofPattern("MMMM yyyy"));

        String prompt = "Составь подробный астрологический прогноз на " + currentMonth + " для знака " + zodiacSign +
                ". Учитывай текущие астрологические транзиты и аспекты. " +
                "Включи прогноз в сферах: любовь, карьера, здоровье, финансы. " +
                "Будь позитивным и мотивирующим, но реалистичным. " +
                "Используй только актуальную информацию для текущего периода. " +
                "Ответ на русском языке." +
                "И не пиши заголовок. ";

        String response = yandexGptService.generateResponse(prompt);

        String messageText = "✨ Астрологический прогноз для знака зодиака - " +
                zodiacSign.toUpperCase() + ":\n\n" + response;
        return createMessageWithKeyboard(chatId, messageText);
    }

    @Override
    public SendMessage handleAstrologyByDate(String birthDate, Long chatId, String firstName) {
        try {
            LocalDate now = LocalDate.now();
            String currentMonth = now.format(DateTimeFormatter.ofPattern("MMMM yyyy"));

            LocalDate date = LocalDate.parse(birthDate, DateTimeFormatter.ofPattern("dd.MM.yyyy"));
            String zodiacSign = determineZodiacSign(date);
            String prompt = "Составь подробный астрологический прогноз на " + currentMonth + " для человека, родившегося " +
                    birthDate + " (знак зодиака: " + zodiacSign + "). " +
                    "Учитывай текущие астрологические транзиты и аспекты для этого знака. " +
                    "Включи прогноз в сферах: любовь, карьера, здоровье, финансы. " +
                    "Учитывай особенности этого знака зодиака и текущий астрологический период. " +
                    "Будь позитивным и мотивирующим, но реалистичным. " +
                    "Используй только актуальную информацию для текущего месяца. " +
                    "Ответ на русском языке. " +
                    "И не пиши заголовок. ";

            String response = yandexGptService.generateResponse(prompt);

            String messageText = "✨ Ваш знак зодиака - " + zodiacSign.toUpperCase() + ":\n\n" + response;
            return createMessageWithKeyboard(chatId, messageText);
        } catch (DateTimeParseException e) {
            return createSimpleMessage(chatId,
                    "❌ Неверный формат даты. Пожалуйста, введите дату в формате ДД.ММ.ГГГГ " +
                            "(например: 15.03.1990)");
        }
    }

    @Override
    public SendMessage createMessageWithKeyboard(Long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        message.setReplyMarkup(keyboardService.createMainKeyboard());
        return message;
    }

    @Override
    public SendMessage createSimpleMessage(Long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        return message;
    }

    @Override
    public SendMessage handleUserMessage(String message, Long chatId, String firstName, Map<Long, UserState> userStates) {
        UserState userState = userStates.getOrDefault(chatId, UserState.DEFAULT);

        try {
            SendMessage response = switch (userState) {
                case AWAITING_IMAGE_PROMPT -> {
                    userStates.put(chatId, UserState.DEFAULT);
                    yield createSimpleMessage(chatId, "");
                }
                case AWAITING_ZODIAC_SIGN -> {
                    userStates.put(chatId, UserState.DEFAULT);
                    yield handleAstrologyRequest(message, chatId, firstName);
                }
                case AWAITING_BIRTH_DATE -> {
                    userStates.put(chatId, UserState.DEFAULT);
                    yield handleAstrologyByDate(message, chatId, firstName);
                }
                default -> handleRegularMessage(message, chatId);
            };
            return response;
        } catch (Exception e) {
            log.error("Error handling user message for chat {}: {}", chatId, e.getMessage());
            userStates.put(chatId, UserState.DEFAULT); // Сбрасываем состояние при ошибке
            return createMessageWithKeyboard(chatId,
                    "❌ Произошла ошибка при обработке сообщения. Попробуйте еще раз.");
        }
    }

    @Override
    public SendPhoto handleImageGeneration(String message, Long chatId) {
        try {
            String imageBase64 = yandexGptService.generateImage(message);
            if (imageBase64 == null) {
                throw new RuntimeException("Failed to generate image");
            }

            InputStream imageStream = imageService.processBase64Image(imageBase64);
            if (imageStream == null) {
                throw new RuntimeException("Failed to process image data");
            }

            return imageService.createImageMessage(chatId, imageStream, message);
        } catch (Exception e) {
            log.error("Image generation failed for chat {}: {}", chatId, e.getMessage());
            throw new RuntimeException("Image generation failed: " + e.getMessage(), e);
        }
    }

    private String determineZodiacSign(LocalDate birthDate) {
        int day = birthDate.getDayOfMonth();
        int month = birthDate.getMonthValue();

        if ((month == 3 && day >= 21) || (month == 4 && day <= 19)) return "Овен";
        if ((month == 4 && day >= 20) || (month == 5 && day <= 20)) return "Телец";
        if ((month == 5 && day >= 21) || (month == 6 && day <= 20)) return "Близнецы";
        if ((month == 6 && day >= 21) || (month == 7 && day <= 22)) return "Рак";
        if ((month == 7 && day >= 23) || (month == 8 && day <= 22)) return "Лев";
        if ((month == 8 && day >= 23) || (month == 9 && day <= 22)) return "Дева";
        if ((month == 9 && day >= 23) || (month == 10 && day <= 22)) return "Весы";
        if ((month == 10 && day >= 23) || (month == 11 && day <= 21)) return "Скорпион";
        if ((month == 11 && day >= 22) || (month == 12 && day <= 21)) return "Стрелец";
        if ((month == 12 && day >= 22) || (month == 1 && day <= 19)) return "Козерог";
        if ((month == 1 && day >= 20) || (month == 2 && day <= 18)) return "Водолей";
        return "Рыбы";
    }

}
