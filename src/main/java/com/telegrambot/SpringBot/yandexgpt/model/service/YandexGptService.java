package com.telegrambot.SpringBot.yandexgpt.model.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.telegrambot.SpringBot.BotConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class YandexGptService {
    private static final Logger logger = LoggerFactory.getLogger(YandexGptService.class);

    private final BotConfig botConfig;
    private final ObjectMapper objectMapper;

    public YandexGptService(BotConfig botConfig) {
        this.botConfig = botConfig;
        this.objectMapper = new ObjectMapper();
    }

    public String generateResponse(String message) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            String url = botConfig.getYandexApiUrl() + "/foundationModels/v1/completion";

            logger.info("Sending request to Yandex GPT API: {}", url);

            HttpPost httpPost = new HttpPost(url);
            httpPost.setHeader("Authorization", "Api-Key " + botConfig.getYandexApiKey());
            httpPost.setHeader("Content-Type", "application/json");

            String jsonRequest = createRequestJson(message);
            logger.debug("Request JSON: {}", jsonRequest);

            httpPost.setEntity(new StringEntity(jsonRequest, "UTF-8"));

            String responseJson = httpClient.execute(httpPost, httpResponse -> {
                int statusCode = httpResponse.getStatusLine().getStatusCode();
                logger.info("Yandex GPT API response status: {}", statusCode);

                if (statusCode != 200) {
                    String errorResponse = EntityUtils.toString(httpResponse.getEntity());
                    logger.error("Yandex GPT API error: {}", errorResponse);
                    throw new RuntimeException("Yandex GPT API returned status: " + statusCode + ". Response: " + errorResponse);
                }
                return EntityUtils.toString(httpResponse.getEntity());
            });
            logger.debug("Response JSON: {}", responseJson);
            return parseResponse(responseJson);

        } catch (Exception e) {
            logger.error("Error while calling Yandex GPT API", e);
            return "Извините, произошла ошибка при обработке вашего запроса: " + e.getMessage();
        }
    }

    private String createRequestJson(String message) {
        try {
            ObjectNode requestJson = objectMapper.createObjectNode();

            requestJson.put("modelUri", "gpt://" + botConfig.getYandexFolderId() + "/" + botConfig.getYandexModel());

            ObjectNode completionOptions = objectMapper.createObjectNode();
            completionOptions.put("stream", false);
            completionOptions.put("temperature", 0.9);
            completionOptions.put("maxTokens", 2000);
            requestJson.set("completionOptions", completionOptions);

            ArrayNode messages = objectMapper.createArrayNode();
            ObjectNode messageNode = objectMapper.createObjectNode();
            messageNode.put("role", "user");
            messageNode.put("text", message);
            messages.add(messageNode);
            requestJson.set("messages", messages);

            return objectMapper.writeValueAsString(requestJson);

        } catch (Exception e) {
            throw new RuntimeException("Error creating JSON request", e);
        }
    }

    private String parseResponse(String responseJson) {
        try {
            JsonNode rootNode = objectMapper.readTree(responseJson);

            logger.info("Full response structure: {}", rootNode.toPrettyString());

            if (rootNode.has("error")) {
                String error = rootNode.get("error").asText();
                logger.error("Yandex GPT API error: {}", error);
                return "Ошибка Yandex GPT: " + error;
            }

            JsonNode resultNode = rootNode.path("result");
            if (resultNode.isMissingNode()) {
                logger.warn("No 'result' field in response");
                return "Не удалось получить ответ от Yandex GPT. Проверьте настройки API.";
            }

            JsonNode alternativesNode = resultNode.path("alternatives");
            if (alternativesNode.isMissingNode() || !alternativesNode.isArray() || alternativesNode.isEmpty()) {
                logger.warn("No 'alternatives' array in response");
                return "Не удалось получить альтернативы ответа от Yandex GPT.";
            }

            JsonNode firstAlternative = alternativesNode.get(0);
            JsonNode messageNode = firstAlternative.path("message");
            if (messageNode.isMissingNode()) {
                logger.warn("No 'message' field in alternative");
                // Попробуем получить текст напрямую из alternative
                JsonNode textNode = firstAlternative.path("text");
                if (!textNode.isMissingNode()) {
                    return textNode.asText();
                }
                return "Не удалось извлечь текст ответа из структуры Yandex GPT.";
            }

            JsonNode textNode = messageNode.path("text");
            if (textNode.isMissingNode()) {
                logger.warn("No 'text' field in message");
                return "Ответ от Yandex GPT не содержит текста.";
            }

            return textNode.asText();

        } catch (Exception e) {
            logger.error("Error parsing Yandex GPT response", e);
            return "Ошибка при обработке ответа от Yandex GPT: " + e.getMessage();
        }
    }
}
