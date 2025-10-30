package com.samuelich.service;

import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;

import java.io.InputStream;

public interface ImageService {

    SendPhoto createImageMessage(Long chatId, InputStream inputStream, String prompt);

    InputStream processBase64Image(String base64Response);
}
