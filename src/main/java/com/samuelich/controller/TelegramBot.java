package com.samuelich.controller;

import com.samuelich.config.BotConfig;
import com.samuelich.model.enums.UserState;
import com.samuelich.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.*;

@Slf4j
@RequiredArgsConstructor
@Component
public class TelegramBot extends TelegramLongPollingBot {

    private final BotConfig botConfig;
    private final CommandHandlerService commandHandlerService;
    private final CallbackHandlerService callbackHandlerService;
    private final MessageHandlerService messageHandlerService;

    private final Map<Long, UserState> userStates = new HashMap<>();

    @Override
    public String getBotUsername() {
        return botConfig.getBotName();
    }

    @Override
    public String getBotToken() {
        return botConfig.getBotToken();
    }

    @Override
    public void onUpdateReceived(Update update) {
        try {
            if (update.hasMessage() && update.getMessage().hasText()) {
                handleMessageUpdate(update);
            } else if (update.hasCallbackQuery()) {
                handleCallbackUpdate(update);
            }
        } catch (Exception e) {
            log.error("Error processing update", e);
        }
    }

    private void handleCallbackUpdate(Update update) {
        Long chatId = update.getCallbackQuery().getMessage().getChatId();
        String callbackData = update.getCallbackQuery().getData();

        SendMessage response = callbackHandlerService.handleCallback(chatId, callbackData, userStates);
        executeMessage(response);
    }

    private void handleMessageUpdate(Update update) {
        String messageText = update.getMessage().getText();
        Long chatId = update.getMessage().getChatId();
        String firstName = update.getMessage().getChat().getFirstName();

        if (messageText.startsWith("/")) {
            handleCommand(messageText, chatId, firstName);
        } else {
            handleUserMessage(messageText, chatId, firstName);
        }
    }

    private void handleCommand(String command, Long chatId, String firstName) {
        SendMessage response = switch (command) {
            case "/start" -> commandHandlerService.handleStartCommand(chatId, firstName);
            case "/help" -> commandHandlerService.handleHelpCommand(chatId);
            case "/hide" -> commandHandlerService.handleHideCommand(chatId);
            default -> commandHandlerService.handleUnknownCommand(chatId);
        };
        executeMessage(response);
    }

    private void handleUserMessage(String message, Long chatId, String firstName) {
        UserState userState = userStates.getOrDefault(chatId, UserState.DEFAULT);

        try {
            if (userState == UserState.AWAITING_ZODIAC_SIGN ||
                    userState == UserState.AWAITING_BIRTH_DATE ||
                    userState == UserState.AWAITING_IMAGE_PROMPT) {

                SendMessage instantConfirmation = createInstantConfirmation(userState, message, chatId);
                executeMessage(instantConfirmation);
            }

            SendMessage response = messageHandlerService.handleUserMessage(message, chatId, firstName, userStates);
            executeMessage(response);

            if (userState == UserState.AWAITING_IMAGE_PROMPT) {
                handleImageGeneration(message, chatId);
            }
        } catch (Exception e) {
            log.error("Error handling user message", e);
            sendErrorMessage(chatId);
        }
    }

    private SendMessage createInstantConfirmation(UserState userState, String message, Long chatId) {
        String confirmationText = switch (userState) {
            case AWAITING_IMAGE_PROMPT -> "🖼️ Генерирую картинку...";
            case AWAITING_ZODIAC_SIGN -> "♈️ Ваш знак зодиака: " + message + "\n⏳ Составляю астрологический прогноз...";
            case AWAITING_BIRTH_DATE ->
                    "📅 Ваша дата рождения: " + message + "\n⏳ Определяю знак зодиака и составляю прогноз...";
            default -> "";
        };

        SendMessage response = new SendMessage();
        response.setChatId(String.valueOf(chatId));
        response.setText(confirmationText);
        return response;
    }

    private void handleImageGeneration(String message, Long chatId) {

        try {
            SendPhoto sendPhoto = messageHandlerService.handleImageGeneration(message, chatId);
            if (sendPhoto != null) {
                execute(sendPhoto);
                SendMessage successMessage = messageHandlerService.createMessageWithKeyboard(chatId,
                        "Отлично! Опишите следующую картину, или нажмите 'Новый вопрос' для выхода.");
                executeMessage(successMessage);
            } else {
                SendMessage errorMessage = messageHandlerService.createMessageWithKeyboard(chatId,
                        "❌ Не удалось сгенерировать изображение. Возможно, запрос нарушает правила контента. " +
                                "Попробуйте описать что-то другое.");
                executeMessage(errorMessage);
            }
        } catch (Exception e) {
            log.error("Error generating image", e);
            SendMessage errorMessage = messageHandlerService.createMessageWithKeyboard(chatId,
                    "Не удалось обработать изображение. Попробуйте ещё раз.");
            executeMessage(errorMessage);
        }
    }

    private void executeMessage(SendMessage message) {
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error sending message", e);
        }
    }

    private void sendErrorMessage(Long chatId) {
        SendMessage errorMessage = messageHandlerService.createMessageWithKeyboard(chatId,
                "Извините, произошла ошибка. Попробуйте еще раз.");
        executeMessage(errorMessage);
    }
}