package com.samuelich.service.impl;

import com.samuelich.service.CommandHandlerService;
import com.samuelich.service.KeyboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

@RequiredArgsConstructor
@Service
public class CommandHandlerServiceImpl implements CommandHandlerService {

    private final KeyboardService keyboardService;

    @Override
    public SendMessage handleStartCommand(Long chatId, String firstName) {
        String welcomeText = "👋 Привет, " + firstName + "!\n\n" +
                "Я - ваш умный помощник с искусственным интеллектом. Вот что я умею:\n\n" +
                "🎯 *Новый вопрос* - задайте любой вопрос\n" +
                "🖼️ *Сгенерировать картинку* - создам изображение по вашему описанию\n" +
                "🔮 *Астрологический прогноз* - получите прогноз для вашего знака зодиака\n\n" +
                "Выберите действие ниже или просто напишите мне сообщение!";

        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(welcomeText);
        message.setParseMode("Markdown");
        message.setReplyMarkup(keyboardService.createMainKeyboard());
        return message;
    }

    @Override
    public SendMessage handleHelpCommand(Long chatId) {
        String helpText = """
                📖 *Помощь по боту*
                
                Доступные команды:
                • /start - начать работу с ботом
                • /help - показать эту справку
                • /hide - скрыть клавиатуру
                
                Основные функции:
                • *Задавайте вопросы* - просто напишите мне
                • *Генерация изображений* - нажмите кнопку и опишите картинку
                • *Астрология* - получите персональный прогноз""";

        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(helpText);
        message.setParseMode("Markdown");
        message.setReplyMarkup(keyboardService.createMainKeyboard());
        return message;
    }

    @Override
    public SendMessage handleHideCommand(Long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Клавиатура скрыта. Используйте /start чтобы вернуть меню.");
        return message;
    }

    @Override
    public SendMessage handleUnknownCommand(Long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("❌ Неизвестная команда. Используйте /help для списка доступных команд.");
        message.setReplyMarkup(keyboardService.createMainKeyboard());
        return message;
    }
}
