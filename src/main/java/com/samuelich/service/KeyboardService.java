package com.samuelich.service;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

public interface KeyboardService {

    InlineKeyboardMarkup createMainKeyboard();

    InlineKeyboardMarkup createAstrologyChoiceKeyboard();
}
