package com.samuelich.service;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

public interface CommandHandlerService {

    SendMessage handleStartCommand(Long chatId, String firstName);

    SendMessage handleHelpCommand(Long chatId);

    SendMessage handleHideCommand(Long chatId);

    SendMessage handleUnknownCommand(Long chatId);
}
