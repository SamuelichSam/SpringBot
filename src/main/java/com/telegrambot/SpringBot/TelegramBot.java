package com.telegrambot.SpringBot;

import com.telegrambot.SpringBot.yandexgpt.model.service.YandexGptService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.*;

@Component
public class TelegramBot extends TelegramLongPollingBot {

    private final BotConfig botConfig;
    private final YandexGptService yandexGptService;
    private final Map<Long, String> userStates = new HashMap<>();
    private static final String AWAITING_IMAGE_PROMPT = "AWAITING_IMAGE_PROMPT";

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
        if (userStates.getOrDefault(chatId, "").equals(AWAITING_IMAGE_PROMPT)) {

            String base64Response = yandexGptService.generateImage(message);

            if (base64Response != null && base64Response.contains("base64,")) {
                try {
                    String base64Data = base64Response.split("base64,",2)[1];
                    byte[] imageBytes = Base64.getDecoder().decode(base64Data);
                    InputStream inputStream = new ByteArrayInputStream(imageBytes);
                    SendPhoto sendPhoto = new SendPhoto();
                    sendPhoto.setChatId(chatId);
                    sendPhoto.setPhoto(new InputFile(inputStream, "generated_image.jpg"));
                    sendPhoto.setCaption("🎨 Ваше изображение по запросу:\n" + message);
                    execute(sendPhoto);
                    sendMessageWithKeyboard(chatId,
                            "Отлично! Опишите следующую картину, или нажмите 'Новый вопрос' для выхода.");
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                    sendMessageWithKeyboard(chatId, "Не удалось обработать изображение. Попробуйте ещё раз.");
                }
            } else {
                sendMessageWithKeyboard(chatId,
                        "❌ Не удалось сгенерировать изображение.\n\n" +
                                "Возможные причины:\n" +
                                "• Слишком длинный или сложный запрос\n" +
                                "• Ограничение API\n" +
                                "Попробуйте ещё раз с более простым описанием.");
            }
            return;
        }

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
                userStates.remove(chatId);
                sendMessageWithKeyboard(chatId, "Задайте ваш вопрос! Я готов помочь.");
                return true;
            case "🖼️ Сгенерировать картинку":
                userStates.put(chatId, AWAITING_IMAGE_PROMPT);
                SendMessage message = new SendMessage();
                message.setChatId(chatId);
                message.setText("Опишите картину, которую хотите сгенерировать");
                try {
                    execute(message);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
                return true;
            case "⚙️ Настройки":
                sendMessageWithKeyboard(chatId,
                        "Настройки:\n\n" +
                                "• Температура GPT: 0.9\n" +
                                "• Макс. токенов: 2000\n" +
                                "• Текстовая модель: " + botConfig.getYandexModel() + "\n" +
                                "• Модель генерации изображений: " + botConfig.getYandexArtModel() + "\n\n" +
                                "Используйте /help для дополнительной информации");
                return true;
            case "❌ Скрыть кнопки":
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
        row1.add("🖼️ Сгенерировать картинку");

        // Второй ряд кнопок
        KeyboardRow row2 = new KeyboardRow();
        row2.add("⚙️ Настройки");
        row2.add("❌ Скрыть кнопки");

        keyboard.add(row1);
        keyboard.add(row2);

        keyboardMarkup.setKeyboard(keyboard);
        return keyboardMarkup;
    }
}
