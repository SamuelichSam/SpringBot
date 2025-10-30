package com.samuelich.service.impl;

import com.samuelich.service.ImageService;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Base64;

@Service
public class ImageServiceImpl implements ImageService {


    @Override
    public SendPhoto createImageMessage(Long chatId, InputStream inputStream, String prompt) {
        SendPhoto sendPhoto = new SendPhoto();
        sendPhoto.setChatId(String.valueOf(chatId));
        sendPhoto.setPhoto(new InputFile(inputStream, "generated_image.jpg"));
        sendPhoto.setCaption("🎨 Ваше изображение по запросу:\n" + prompt);
        return sendPhoto;
    }

    @Override
    public InputStream processBase64Image(String base64Response) {
        if (base64Response != null && base64Response.contains("base64,")) {
            String base64Data = base64Response.split("base64,", 2)[1];
            byte[] imageBytes = Base64.getDecoder().decode(base64Data);
            return new ByteArrayInputStream(imageBytes);
        }
        return null;
    }
}
