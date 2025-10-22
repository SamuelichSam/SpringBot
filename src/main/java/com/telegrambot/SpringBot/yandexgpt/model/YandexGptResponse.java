package com.telegrambot.SpringBot.yandexgpt.model;

import lombok.Data;

import java.util.List;

@Data
public class YandexGptResponse {
    private Result result;

    @Data
    public static class Result {
        private List<Alternative> alternatives;
        private Usage usage;

        @Data
        public static class Alternative {
            private Message message;
            private String status;

            @Data
            public static class Message {
                private String role;
                private String text;
            }
        }

        @Data
        public static class Usage {
            private String inputTextTokens;
            private String completionTokens;
            private String totalTokens;
        }
    }
}
