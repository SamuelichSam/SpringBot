package com.samuelich.service;

public interface YandexGptService {

    String generateResponse(String message);

    String generateImage(String prompt);
}
