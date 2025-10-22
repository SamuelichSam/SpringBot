package com.telegrambot.SpringBot;

import com.telegrambot.SpringBot.yandexgpt.model.service.YandexGptService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
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
    private static final String AWAITING_ZODIAC_SIGN = "AWAITING_ZODIAC_SIGN";
    private static final String CALLBACK_NEW_QUESTION = "new_question";
    private static final String CALLBACK_GENERATE_IMAGE = "generate_image";
    private static final String CALLBACK_SETTINGS = "settings";
    private static final String CALLBACK_ASTROLOGY = "astrology";

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
            String firstName = update.getMessage().getChat().getFirstName();

            if (messageText.startsWith("/")) {
                handleCommand(messageText, chatId, firstName);
            } else {
                handleUserMessage(messageText, chatId, firstName);
            }
        } else if (update.hasCallbackQuery()) {
            handleCallbackQuery(update.getCallbackQuery().getMessage().getChatId(),
                    update.getCallbackQuery().getData(),
                    update.getCallbackQuery().getMessage().getMessageId());
        }
    }

    private void handleCommand(String command, long chatId, String firstName) {
        switch (command) {
            case "/start":
                SendMessage welcomeMessage = new SendMessage();
                welcomeMessage.setChatId(chatId);
                welcomeMessage.setText(
                        "Привет, " + firstName + "! Я бот с интегрированным искусственным интеллектом. Задайте мне любой вопрос!");
                welcomeMessage.setReplyMarkup(createInlineKeyboard());
                try {
                    execute(welcomeMessage);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
                break;
            case "/help":
                sendMessageWithInlineKeyboard(chatId,
                        "Просто напишите мне сообщение, и я постараюсь на него ответить!\n\n" +
                                "• Используйте кнопки для быстрых действий\n" +
                                "• Напишите любой вопрос для получения ответа\n" +
                                "• /hide - скрыть кнопки");
                break;
            case "/hide":
                SendMessage hideMessage = new SendMessage();
                hideMessage.setChatId(chatId);
                hideMessage.setText("Клавиатура скрыта. Напишите /start для возврата меню.");
                try {
                    execute(hideMessage);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
                break;
            default:
                sendMessageWithInlineKeyboard(chatId, "Неизвестная команда. Используйте /help для справки.");
        }
    }

    private void handleCallbackQuery(long chatId, String callbackData, Integer messageId) {
        switch (callbackData) {
            case CALLBACK_NEW_QUESTION:
                userStates.remove(chatId);
                sendMessageWithInlineKeyboard(chatId, "Задайте ваш вопрос! Я готов помочь.");
                break;
            case CALLBACK_GENERATE_IMAGE:
                userStates.put(chatId, AWAITING_IMAGE_PROMPT);
                SendMessage message = new SendMessage();
                message.setChatId(chatId);
                message.setText("Опишите картину, которую хотите сгенерировать");
                try {
                    execute(message);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
                break;
            case CALLBACK_SETTINGS:
                sendMessageWithInlineKeyboard(chatId,
                        "Настройки:\n\n" +
                                "• Температура GPT: 0.9\n" +
                                "• Макс. токенов: 2000\n" +
                                "• Текстовая модель: " + botConfig.getYandexModel() + "\n" +
                                "• Модель генерации изображений: " + botConfig.getYandexArtModel() + "\n\n" +
                                "Используйте /help для дополнительной информации");
                break;
            case CALLBACK_ASTROLOGY:
                userStates.put(chatId, AWAITING_ZODIAC_SIGN);
                SendMessage astrologyMessage = new SendMessage();
                astrologyMessage.setChatId(chatId);
                astrologyMessage.setText("✨ Введите ваш знак зодиака для составления прогноза:\n\n" +
                        "• Овен\n• Телец\n• Близнецы\n• Рак\n• Лев\n• Дева\n" +
                        "• Весы\n• Скорпион\n• Стрелец\n• Козерог\n• Водолей\n• Рыбы");
                try {
                    execute(astrologyMessage);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
                break;
        }
    }

    private void handleUserMessage(String message, long chatId, String firstName) {
        String userState = userStates.getOrDefault(chatId, "");

        if (userState.equals(AWAITING_IMAGE_PROMPT)) {
            handleImageGeneration(message, chatId);
            return;
        }

        if (userState.equals(AWAITING_ZODIAC_SIGN)) {
            handleAstrologyRequest(message, chatId,  firstName);
            return;
        }

        handleRegularMessage(message, chatId);
    }

    private void handleImageGeneration(String message, long chatId) {
        String base64Response = yandexGptService.generateImage(message);

        if (base64Response != null && base64Response.contains("base64,")) {
            try {
                String base64Data = base64Response.split("base64,", 2)[1];
                byte[] imageBytes = Base64.getDecoder().decode(base64Data);
                InputStream inputStream = new ByteArrayInputStream(imageBytes);
                SendPhoto sendPhoto = new SendPhoto();
                sendPhoto.setChatId(chatId);
                sendPhoto.setPhoto(new InputFile(inputStream, "generated_image.jpg"));
                sendPhoto.setCaption("🎨 Ваше изображение по запросу:\n" + message);
                execute(sendPhoto);
                sendMessageWithInlineKeyboard(chatId,
                        "Отлично! Опишите следующую картину, или нажмите 'Новый вопрос' для выхода.");
            } catch (TelegramApiException e) {
                e.printStackTrace();
                sendMessageWithInlineKeyboard(chatId, "Не удалось обработать изображение. Попробуйте ещё раз.");
            }
        } else {
            sendMessageWithInlineKeyboard(chatId,
                    "❌ Не удалось сгенерировать изображение.\n\n" +
                            "Возможные причины:\n" +
                            "• Слишком длинный или сложный запрос\n" +
                            "• Ограничение API\n" +
                            "Попробуйте ещё раз с более простым описанием.");
        }
    }

    private void handleAstrologyRequest(String zodiacSign, long chatId, String firstName) {
        try {
            SendMessage typingMessage = new SendMessage();
            typingMessage.setChatId(String.valueOf(chatId));
            if (firstName.equals("Ксения")) {
                typingMessage.setText("🔮 Астрологический прогноз составлен при " +
                        "поддержке ВЕЛИКОЙ и УЖАСНОЙ астрологини Величко!");

            } else {
                typingMessage.setText("🔮 Составляю астрологический прогноз...");
            }
            execute(typingMessage);

            String prompt = "Составь подробный астрологический прогноз на месяц для знака " + zodiacSign +
                    ". Включи прогноз в сферах: любовь, карьера, здоровье, финансы. " +
                    "Будь позитивным и мотивирующим, но реалистичным. Ответ на русском языке.";

            String response = yandexGptService.generateResponse(prompt);
            sendMessageWithInlineKeyboard(chatId,
                    "✨ Астрологический прогноз для " + zodiacSign + ":\n\n" + response);

        } catch (Exception e) {
            sendMessageWithInlineKeyboard(chatId, "Извините, не удалось составить прогноз. Попробуйте еще раз.");
            e.printStackTrace();
        } finally {
            userStates.remove(chatId);
        }
    }

    private void handleRegularMessage(String message, long chatId) {
        try {
            SendMessage typingMessage = new SendMessage();
            typingMessage.setChatId(String.valueOf(chatId));
            typingMessage.setText("Думаю...");
            execute(typingMessage);

            String response = yandexGptService.generateResponse(message);
            sendMessageWithInlineKeyboard(chatId, response);

        } catch (Exception e) {
            sendMessageWithInlineKeyboard(chatId, "Извините, произошла ошибка. Попробуйте еще раз.");
            e.printStackTrace();
        }
    }

    private void sendMessageWithInlineKeyboard(long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        message.setReplyMarkup(createInlineKeyboard());

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private InlineKeyboardMarkup createInlineKeyboard() {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

        List<InlineKeyboardButton> rowInline1 = new ArrayList<>();
        InlineKeyboardButton newQuestionButton = new InlineKeyboardButton();
        newQuestionButton.setText("🎯 Новый вопрос");
        newQuestionButton.setCallbackData(CALLBACK_NEW_QUESTION);

        InlineKeyboardButton generateImageButton = new InlineKeyboardButton();
        generateImageButton.setText("🖼️ Сгенерировать картинку");
        generateImageButton.setCallbackData(CALLBACK_GENERATE_IMAGE);

        rowInline1.add(newQuestionButton);
        rowInline1.add(generateImageButton);

        List<InlineKeyboardButton> rowInline2 = new ArrayList<>();
        InlineKeyboardButton astrologyButton = new InlineKeyboardButton();
        astrologyButton.setText("🔮 Астрологический прогноз");
        astrologyButton.setCallbackData(CALLBACK_ASTROLOGY);

        rowInline2.add(astrologyButton);

        rowsInline.add(rowInline1);
        rowsInline.add(rowInline2);

        inlineKeyboardMarkup.setKeyboard(rowsInline);
        return inlineKeyboardMarkup;
    }
}