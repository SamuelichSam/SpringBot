package com.samuelich.service.impl;

import com.samuelich.model.enums.CallbackType;
import com.samuelich.service.KeyboardService;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;

@Service
public class KeyboardServiceImpl implements KeyboardService {


    @Override
    public InlineKeyboardMarkup createMainKeyboard() {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

        List<InlineKeyboardButton> rowInline1 = new ArrayList<>();
        rowInline1.add(createButton("🎯 Новый вопрос", CallbackType.NEW_QUESTION));
        rowInline1.add(createButton("🖼️ Сгенерировать картинку", CallbackType.GENERATE_IMAGE));

        List<InlineKeyboardButton> rowInline2 = new ArrayList<>();
        rowInline2.add(createButton("🔮 Астрологический прогноз", CallbackType.ASTROLOGY));

        rowsInline.add(rowInline1);
        rowsInline.add(rowInline2);

        inlineKeyboardMarkup.setKeyboard(rowsInline);
        return inlineKeyboardMarkup;
    }

    private InlineKeyboardButton createButton(String text, CallbackType callbackType) {
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText(text);
        button.setCallbackData(callbackType.getValue());
        return button;
    }
}
