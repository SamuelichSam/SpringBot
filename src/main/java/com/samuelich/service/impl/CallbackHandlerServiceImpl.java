package com.samuelich.service.impl;

import com.samuelich.model.enums.CallbackType;
import com.samuelich.model.enums.UserState;
import com.samuelich.service.CallbackHandlerService;
import com.samuelich.service.KeyboardService;
import com.samuelich.service.MessageHandlerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import java.util.Map;

@RequiredArgsConstructor
@Service
public class CallbackHandlerServiceImpl implements CallbackHandlerService {

    private final MessageHandlerService messageHandlerService;

    @Override
    public SendMessage handleCallback(Long chatId, String callbackData, Map<Long, UserState> userStates) {
        try {
            CallbackType callbackType = CallbackType.fromValue(callbackData);

            return switch (callbackType) {
                case NEW_QUESTION -> handleNewQuestion(chatId, userStates);
                case GENERATE_IMAGE -> handleGenerateImage(chatId, userStates);
                case ASTROLOGY -> handleAstrology(chatId, userStates);
                case SETTINGS -> handleSettings(chatId);
            };
        } catch (IllegalArgumentException e) {
            return messageHandlerService.createSimpleMessage(chatId, "❌ Неизвестная команда");
        }
    }

    @Override
    public SendMessage handleNewQuestion(Long chatId, Map<Long, UserState> userStates) {
        userStates.put(chatId, UserState.DEFAULT);
        return messageHandlerService.createMessageWithKeyboard(chatId,
                "🎯 Отлично! Теперь просто напишите ваш вопрос, и я постараюсь на него ответить.");
    }

    @Override
    public SendMessage handleGenerateImage(Long chatId, Map<Long, UserState> userStates) {
        userStates.put(chatId, UserState.AWAITING_IMAGE_PROMPT);
        return messageHandlerService.createSimpleMessage(chatId,
                "🖼️ Опишите изображение, которое вы хотите сгенерировать. " +
                        "Будьте как можно более подробны в описании для лучшего результата!");
    }

    @Override
    public SendMessage handleSettings(Long chatId) {
        return messageHandlerService.createMessageWithKeyboard(chatId,
                """
                        ⚙️ Настройки
                        
                        Эта функция находится в разработке. Скоро здесь появятся дополнительные настройки бота.""");
    }

    @Override
    public SendMessage handleAstrology(Long chatId, Map<Long, UserState> userStates) {
        userStates.put(chatId, UserState.AWAITING_ZODIAC_SIGN);
        return messageHandlerService.createSimpleMessage(chatId,
                "🔮 Введите ваш знак зодиака (например: Овен, Телец, Близнецы и т.д.), " +
                        "и я составлю для вас персональный астрологический прогноз!");
    }
}
