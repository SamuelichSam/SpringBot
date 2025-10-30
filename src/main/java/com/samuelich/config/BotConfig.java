package com.samuelich.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Getter
@Configuration
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

    @Value("${yandex.art.model}")
    private String yandexArtModel;

    @Value("${yandex.temperature}")
    private Double temperature;

    @Value("${yandex.max-tokens}")
    private Integer maxTokens;
}
