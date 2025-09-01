package com.telegrambot.SpringBot;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource("classpath:application.properties")
public class BotConfig {

    @Value("${bot.token}")
    private String botToken;

    @Value("${bot.name}")
    private String botName;

    @Value("${yandex.api.key}")
    private String yandexApiKey;

    @Value("${yandex.folder-id}")
    private String yandexFolderId;

    @Value("${yandex.api.url}")
    private String yandexApiUrl;

    @Value("${yandex.model}")
    private String yandexModel;

    // Getters
    public String getBotToken() { return botToken; }
    public String getBotName() { return botName; }
    public String getYandexApiKey() { return yandexApiKey; }
    public String getYandexFolderId() { return yandexFolderId; }
    public String getYandexApiUrl() { return yandexApiUrl; }
    public String getYandexModel() { return yandexModel; }
}
