package com.telegrambot.SpringBot.yandexgpt.model;

import lombok.Data;

import java.util.List;

@Data
public class YandexGptRequest {
    private String modelUri;
    private CompletionOptions completionOptions;
    private List<Message> messages;

    @Data
    public static class CompletionOptions {
        private boolean stream;
        private double temperature;
        private int maxTokens;
    }

    @Data
    public static class Message {
        private String role;
        private String text;

        public Message(String role, String text) {
            this.role = role;
            this.text = text;
        }
    }
}
