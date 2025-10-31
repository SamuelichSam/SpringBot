package com.samuelich.service.impl;

import com.samuelich.model.UserAstroData;
import com.samuelich.model.enums.UserState;
import com.samuelich.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@Service
public class MessageHandlerServiceImpl implements MessageHandlerService {

    private final YandexGptService yandexGptService;
    private final KeyboardService keyboardService;
    private final ImageService imageService;
    private final AstrologyCalculationService astrologyService;

    private final Map<Long, UserAstroData> userAstroDataMap = new HashMap<>();

    @Override
    public SendMessage handleRegularMessage(String message, Long chatId) {
        String response = yandexGptService.generateResponse(message);
        return createMessageWithKeyboard(chatId, response);
    }

    @Override
    public SendMessage handleAstrologyRequest(Long chatId, String firstName, Map<Long, UserState> userStates) {
        UserAstroData userData = userAstroDataMap.get(chatId);
        if (userData == null) {
            userStates.put(chatId, UserState.DEFAULT);
            return createMessageWithKeyboard(chatId,
                    "❌ Не найдены данные для составления прогноза. Начните заново с команды /start");
        }

        userStates.put(chatId, UserState.DEFAULT);

        return generateAstrologyReport(userData, chatId, firstName);
    }

    private SendMessage generateAstrologyReport(UserAstroData userData, Long chatId, String firstName) {
        try {
            String detailedPrompt = createAstrologyPrompt(userData);
            String response = yandexGptService.generateResponse(detailedPrompt);

            userAstroDataMap.remove(chatId);

            String finalReport = String.format(
                    "✨ *Ваш астрологический прогноз %s ✨\n\n" +
                            "📊 Ваши данные:\n" +
                            "• ♈ Знак зодиака: %s\n" +
                            "• 📅 Дата рождения: %s\n" +
                            "• ⏰ Время рождения: %s\n" +
                            "• 📍 Место рождения: %s\n\n" +
                            "%s",
                    firstName,
                    userData.getZodiacSign(),
                    userData.getBirthDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
                    userData.getBirthTime().format(DateTimeFormatter.ofPattern("HH:mm")),
                    userData.getBirthPlace(),
                    response
            );

            return createMessageWithKeyboard(chatId, finalReport);

        } catch (Exception e) {
            log.error("Error generating astrology report", e);
            userAstroDataMap.remove(chatId);
            return createMessageWithKeyboard(chatId,
                    "❌ Произошла ошибка при генерации прогноза. Пожалуйста, попробуйте позже.");
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

        return switch (userState) {
            case AWAITING_IMAGE_PROMPT -> handleImagePrompt(chatId, userStates);
            case AWAITING_BIRTH_DATE -> handleBirthDateInput(message, chatId, userStates);
            case AWAITING_BIRTH_TIME -> handleBirthTimeInput(message, chatId, userStates);
            case AWAITING_BIRTH_PLACE -> handleBirthPlaceInput(message, chatId, userStates, firstName);
            default -> handleRegularMessage(message, chatId);
        };
    }

    private SendMessage handleImagePrompt(Long chatId, Map<Long, UserState> userStates) {
        userStates.put(chatId, UserState.DEFAULT);
        return createMessageWithKeyboard(chatId, "🖼️ Запрос на генерацию изображения принят! Обрабатываю...");
    }

    private SendMessage handleBirthDateInput(String message, Long chatId, Map<Long, UserState> userStates) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
            LocalDate birthDate = LocalDate.parse(message, formatter);

            if (birthDate.isAfter(LocalDate.now())) {
                return createSimpleMessage(chatId,
                        "❌ Дата рождения не может быть в будущем. Пожалуйста, введите корректную дату:");
            }

            UserAstroData userData = new UserAstroData(chatId);
            userData.setBirthDate(birthDate);
            userAstroDataMap.put(chatId, userData);

            userStates.put(chatId, UserState.AWAITING_BIRTH_TIME);
            return createSimpleMessage(chatId,
                    "⏰ Отлично! Теперь введите *время рождения* (в формате ЧЧ:ММ):\n" +
                            "Например: 14:30\n\n" +
                            "Если точное время неизвестно, введите 'не знаю'");

        } catch (DateTimeParseException e) {
            return createSimpleMessage(chatId,
                    "❌ Неверный формат даты. Пожалуйста, введите дату в формате ДД.ММ.ГГГГ:\n" +
                            "Например: 15.05.1990");
        }
    }

    private SendMessage handleBirthTimeInput(String message, Long chatId, Map<Long, UserState> userStates) {
        UserAstroData userData = userAstroDataMap.get(chatId);

        if ("не знаю".equalsIgnoreCase(message)) {
            userData.setBirthTime(LocalTime.NOON);
        } else {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
                LocalTime birthTime = LocalTime.parse(message, formatter);
                userData.setBirthTime(birthTime);
            } catch (DateTimeParseException e) {
                return createSimpleMessage(chatId,
                        "❌ Неверный формат времени. Пожалуйста, введите время в формате ЧЧ:ММ:\n" +
                                "Например: 14:30\n\n" +
                                "Или введите 'не знаю'");
            }
        }

        userStates.put(chatId, UserState.AWAITING_BIRTH_PLACE);
        return createSimpleMessage(chatId,
                "📍 Теперь введите *место рождения* (название населенного пункта):\n" +
                        "Например: Москва");
    }

    private SendMessage handleBirthPlaceInput(String message, Long chatId, Map<Long, UserState> userStates, String firstName) {
        UserAstroData userData = userAstroDataMap.get(chatId);
        userData.setBirthPlace(message);

        String zodiacSign = astrologyService.calculateZodiacSign(userData.getBirthDate());
        userData.setZodiacSign(zodiacSign);

        String preparationMessage = String.format(
                "✨ *Отлично, %s! Собираю вашу астрологическую карту...\n\n" +
                        "📊 Ваши данные:\n" +
                        "• 📅 Дата: %s\n" +
                        "• ⏰ Время: %s\n" +
                        "• 📍 Место: %s\n" +
                        "• ♈ Знак зодиака: %s\n\n" +
                        "⏳ Составляю подробный персональный прогноз...",
                firstName,
                userData.getBirthDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
                userData.getBirthTime().format(DateTimeFormatter.ofPattern("HH:mm")),
                userData.getBirthPlace(),
                zodiacSign
        );

        return createSimpleMessage(chatId, preparationMessage);
    }

    private String createAstrologyPrompt(UserAstroData userData) {
        String zodiacSign = userData.getZodiacSign();
        String planetaryInfluence = astrologyService.getPlanetaryInfluence(userData.getBirthDate());
        String element = astrologyService.getElement(zodiacSign);
        String zodiacSymbol = astrologyService.getZodiacSymbol(zodiacSign);

        return String.format(
                "Составь подробный персональный астрологический прогноз на ближайший месяц на основе следующих данных:\n\n" +
                        "Основные данные:\n" +
                        "- Знак зодиака: %s %s (стихия %s)\n" +
                        "- Дата рождения: %s\n" +
                        "- Время рождения: %s\n" +
                        "- Место рождения: %s\n" +
                        "- Планетарное влияние: %s\n\n" +
                        "Требования к прогнозу:\n" +
                        "1. Дай прогноз по сферам: любовь и отношения, карьера и финансы, здоровье, личностный рост\n" +
                        "2. Укажи благоприятные и сложные периоды\n" +
                        "3. Дай практические рекомендации\n" +
                        "4. Упомни влияние текущих планетарных аспектов\n" +
                        "5. Будь реалистичным, но позитивным\n" +
                        "6. Учитывай характеристику знака %s\n" +
                        "7. Прогноз должен быть персонализированным\n\n" +
                        "Формат: структурированный текст с эмодзи, но без markdown-разметки",
                zodiacSign, zodiacSymbol, element,
                userData.getBirthDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
                userData.getBirthTime().format(DateTimeFormatter.ofPattern("HH:mm")),
                userData.getBirthPlace(),
                planetaryInfluence,
                zodiacSign.replaceAll("[♈♉♊♋♌♍♎♏♐♑♒♓]", "").trim()
        );
    }

    @Override
    public SendPhoto handleImageGeneration(String message, Long chatId) {
        try {
            String imageBase64 = yandexGptService.generateImage(message);
            if (imageBase64 != null) {
                InputStream imageStream = imageService.processBase64Image(imageBase64);
                if (imageStream != null) {
                    return imageService.createImageMessage(chatId, imageStream, message);
                }
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }
}
