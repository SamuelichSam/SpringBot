package com.samuelich.service;

import com.samuelich.model.enums.UserState;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;

import java.util.Map;

public interface MessageHandlerService {

    SendMessage handleRegularMessage(String message, Long chatId);

    SendMessage handleAstrologyRequest(String zodiacSign, Long chatId, String firstName);

    SendMessage handleAstrologyByDate(String birthDate, Long chatId, String firstName);

    SendMessage createMessageWithKeyboard(Long chatId, String text);

    SendMessage createSimpleMessage(Long chatId, String text);

    SendMessage handleUserMessage(String message, Long chatId, String firstName, Map<Long, UserState> userStates);

    SendPhoto handleImageGeneration(String message, Long chatId);
}
