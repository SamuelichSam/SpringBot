package com.samuelich.service;

import com.samuelich.model.enums.UserState;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import java.util.Map;

public interface CallbackHandlerService {

    SendMessage handleCallback(Long chatId, String callbackData, Map<Long, UserState> userStates);

    SendMessage handleNewQuestion(Long chatId, Map<Long, UserState> userStates);

    SendMessage handleGenerateImage(Long chatId, Map<Long, UserState> userStates);

    SendMessage handleSettings(Long chatId);

    SendMessage handleAstrologyByDate(Long chatId, Map<Long, UserState> userStates);

    SendMessage handleAstrologyBySign(Long chatId, Map<Long, UserState> userStates);
}
