package com.samuelich;

import com.samuelich.controller.TelegramBot;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@SpringBootApplication
public class SpringBotApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringBotApplication.class, args);
    }

    @Bean
    public CommandLineRunner registerBot(TelegramBot telegramBot) {
        return args -> {
            try {
                TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
                botsApi.registerBot(telegramBot);
                System.out.println("Бот успешно запущен!");
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        };
    }

}
