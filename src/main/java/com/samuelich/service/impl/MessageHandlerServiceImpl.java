package com.samuelich.service.impl;

import com.samuelich.model.enums.UserState;
import com.samuelich.service.ImageService;
import com.samuelich.service.KeyboardService;
import com.samuelich.service.MessageHandlerService;
import com.samuelich.service.YandexGptService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;

import java.io.InputStream;
import java.util.Map;

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
        String prompt = "Составь подробный астрологический прогноз на месяц для знака " + zodiacSign +
                ". Включи прогноз в сферах: любовь, карьера, здоровье, финансы. " +
                "Будь позитивным и мотивирующим, но реалистичным. Ответ на русском языке.";

        String response = yandexGptService.generateResponse(prompt);

        String messageText = "✨ Астрологический прогноз для " + zodiacSign + ":\n\n" + response;
        return createMessageWithKeyboard(chatId, messageText);
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
            case AWAITING_IMAGE_PROMPT -> {
                userStates.put(chatId, UserState.DEFAULT);
                yield createMessageWithKeyboard(chatId, "🖼️ Запрос на генерацию изображения принят! Обрабатываю...");
            }
            case AWAITING_ZODIAC_SIGN -> handleAstrologyRequest(message, chatId, firstName);
            default -> handleRegularMessage(message, chatId);
        };
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
