package com.telegrambot.SpringBot;

import com.telegrambot.SpringBot.yandexgpt.model.service.YandexGptService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;

@Component
public class TelegramBot extends TelegramLongPollingBot {

    private final BotConfig botConfig;
    private final YandexGptService yandexGptService;

    public TelegramBot(BotConfig botConfig, YandexGptService yandexGptService) {
        super(botConfig.getBotToken());
        this.botConfig = botConfig;
        this.yandexGptService = yandexGptService;
    }

    @Override
    public String getBotUsername() {
        return botConfig.getBotName();
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            if (messageText.startsWith("/")) {
                handleCommand(messageText, chatId);
            } else {
                handleUserMessage(messageText, chatId);
            }
        }
    }

    private void handleCommand(String command, long chatId) {
        switch (command) {
            case "/start":
                SendMessage welcomeMessage = new SendMessage();
                welcomeMessage.setChatId(chatId);
                welcomeMessage.setText("Привет! Я бот с интегрированным Yandex GPT. Задайте мне любой вопрос!");
                welcomeMessage.setReplyMarkup(createMainKeyboard());
                try {
                    execute(welcomeMessage);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
                break;
            case "/help":
                sendMessageWithKeyboard(chatId,
                        "Просто напишите мне сообщение, и я постараюсь на него ответить с помощью Yandex GPT!\n\n" +
                        "• Используйте кнопки для быстрых действий\n" +
                        "• Напишите любой вопрос для получения ответа\n" +
                        "• /hide - скрыть кнопки");
                break;
            case "/hide":
                ReplyKeyboardRemove keyboardRemove = new ReplyKeyboardRemove();
                keyboardRemove.setRemoveKeyboard(true);

                SendMessage hideMessage = new SendMessage();
                hideMessage.setChatId(chatId);
                hideMessage.setText("Клавиатура скрыта. Напишите /start для возврата меню.");
                hideMessage.setReplyMarkup(keyboardRemove);
                try {
                    execute(hideMessage);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
                break;
            default:
                sendMessageWithKeyboard(chatId, "Неизвестная команда. Используйте /help для справки.");
        }
    }

    private void handleUserMessage(String message, long chatId) {
        if (handleButtonActions(message, chatId)) {
            return;
        }

        try {
            SendMessage typingMessage = new SendMessage();
            typingMessage.setChatId(String.valueOf(chatId));
            typingMessage.setText("Думаю...");
            execute(typingMessage);

            String response = yandexGptService.generateResponse(message);
            sendMessageWithKeyboard(chatId, response);

        } catch (Exception e) {
            sendMessageWithKeyboard(chatId, "Извините, произошла ошибка. Попробуйте еще раз.");
            e.printStackTrace();
        }
    }

    private boolean handleButtonActions(String messageText, long chatId) {
        switch (messageText) {
            case "🎯 Новый вопрос":
                sendMessageWithKeyboard(chatId, "Задайте ваш вопрос! Я готов помочь.");
                return true;
            case "💡 Примеры запросов":
                sendMessageWithKeyboard(chatId,
                        "Вот примеры запросов:\n\n" +
                                "• \"Объясни квантовую физику просто\"\n" +
                                "• \"Напиши план обучения Python\"\n" +
                                "• \"Помоги с идеей для проекта\"\n" +
                                "• \"Объясни эту концепцию: [твоя тема]\"");
                return true;
            case "📊 Статистика":
                sendMessageWithKeyboard(chatId, "Функция статистики в разработке 🚧");
                return true;
            case "⚙️ Настройки":
                sendMessageWithKeyboard(chatId,
                        "Настройки:\n\n" +
                                "• Температура GPT: 0.9\n" +
                                "• Макс. токенов: 2000\n" +
                                "• Модель: " + botConfig.getYandexModel() + "\n\n" +
                                "Используйте /help для дополнительной информации");
                return true;
            case "❌ Скрыть кнопки":
                // Скрываем клавиатуру
                ReplyKeyboardRemove keyboardRemove = new ReplyKeyboardRemove();
                keyboardRemove.setRemoveKeyboard(true);

                SendMessage hideMessage = new SendMessage();
                hideMessage.setChatId(String.valueOf(chatId));
                hideMessage.setText("Клавиатура скрыта. Напишите /start для возврата меню.");
                hideMessage.setReplyMarkup(keyboardRemove);

                try {
                    execute(hideMessage);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
                return true;
            default:
                return false;
        }
    }

    private void sendMessageWithKeyboard(long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        message.setReplyMarkup(createMainKeyboard());

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private ReplyKeyboardMarkup createMainKeyboard() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(false);
        keyboardMarkup.setSelective(true);

        List<KeyboardRow> keyboard = new ArrayList<>();

        // Первый ряд кнопок
        KeyboardRow row1 = new KeyboardRow();
        row1.add("🎯 Новый вопрос");
        row1.add("💡 Примеры запросов");

        // Второй ряд кнопок
        KeyboardRow row2 = new KeyboardRow();
        row2.add("📊 Статистика");
        row2.add("⚙️ Настройки");

        // Третий ряд кнопок
        KeyboardRow row3 = new KeyboardRow();
        row3.add("❌ Скрыть кнопки");

        keyboard.add(row1);
        keyboard.add(row2);
        keyboard.add(row3);

        keyboardMarkup.setKeyboard(keyboard);
        return keyboardMarkup;
    }
}
