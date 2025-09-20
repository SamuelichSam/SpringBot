package com.telegrambot.SpringBot.yandexgpt.model.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.telegrambot.SpringBot.BotConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.rmi.RemoteException;
import java.util.Random;

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

    public String generateImage(String prompt) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            String url = botConfig.getYandexApiUrl() + "/foundationModels/v1/imageGenerationAsync";
            logger.info("Sending image generation request to Yandex API: {}", url);

            HttpPost httpPost = new HttpPost(url);
            httpPost.setHeader("Authorization", "Api-Key " + botConfig.getYandexApiKey());
            httpPost.setHeader("Content-Type", "application/json");

            ObjectNode requestJson = objectMapper.createObjectNode();

            // Модель генерации
            String modelUri = "art://" + botConfig.getYandexFolderId() + "/" + botConfig.getYandexArtModel();
            requestJson.put("model_uri", modelUri);

            // Опции генерации
            ObjectNode generationOptions = objectMapper.createObjectNode();
            generationOptions.put("seed", new Random().nextInt());
            generationOptions.put("mime_type","image/jpeg");

            ObjectNode aspectRatio = objectMapper.createObjectNode();
            aspectRatio.put("width_ratio", 2);
            aspectRatio.put("height_ratio", 1);
            generationOptions.set("aspect_ratio", aspectRatio);
            requestJson.set("generation_options", generationOptions);

            // Промт
            ArrayNode messages = objectMapper.createArrayNode();
            ObjectNode message = objectMapper.createObjectNode();
            message.put("text", prompt);
            message.put("weight", 1.0);
            messages.add(message);
            requestJson.set("messages", messages);

            String jsonRequest = objectMapper.writeValueAsString(requestJson);
            logger.debug("Image generation request JSON: {}", jsonRequest);

            httpPost.setEntity(new StringEntity(jsonRequest, "UTF-8"));

            String responseJson = httpClient.execute(httpPost, httpResponse -> {
                int statusCode = httpResponse.getStatusLine().getStatusCode();
                logger.info("Image generation response status: {}", statusCode);

                if (statusCode != 200) {
                    String errorResponse = EntityUtils.toString(httpResponse.getEntity());
                    logger.error("Image generation error: {}", errorResponse);
                    throw new RemoteException("Image API returned status: " + statusCode + ". Response: " + errorResponse);
                }
                return EntityUtils.toString(httpResponse.getEntity());
            });
            logger.info("Image generation response JSON: {}", responseJson);

            JsonNode rootNode = objectMapper.readTree(responseJson);
            JsonNode errorNode = rootNode.path("error");
            if (!errorNode.isMissingNode()) {
                logger.warn("Yandex Art API returned error: {}", errorNode.toString());
                return null;
            }

            String operationId = rootNode.path("id").asText();
            if (operationId.isEmpty()) {
                logger.warn("No operation ID in response");
                return null;
            }
            return waitForImageResult(operationId);
        } catch (Exception e) {
            logger.error("Error during image generation", e);
            return null;
        }
    }

    private String waitForImageResult(String operationId) throws Exception {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            String url = botConfig.getYandexApiUrl() + "/operations/" + operationId;
            HttpGet httpGet = new HttpGet(url);
            httpGet.setHeader("Authorization", "Api-Key " + botConfig.getYandexApiKey());

            for (int attempt = 1; attempt <= 30; attempt++) {
                Thread.sleep(2000); // ждём 2 секунды

                String resultJson = httpClient.execute(httpGet, httpResponse -> {
                    int statusCode = httpResponse.getStatusLine().getStatusCode();
                    if (statusCode != 200) {
                        String error = EntityUtils.toString(httpResponse.getEntity());
                        throw new RuntimeException("HTTP " + statusCode + ": " + error);
                    }
                    return EntityUtils.toString(httpResponse.getEntity());
                });

                logger.debug("CloseOperation check (attempt {}): {}", attempt, resultJson);

                JsonNode resultNode = objectMapper.readTree(resultJson);

                if (!resultNode.path("done").asBoolean()) {
                    continue; // не готово — ждём следующей итерации
                }

                // Теперь проверяем: успешен ли результат
                JsonNode errorNode = resultNode.path("error");
                if (!errorNode.isMissingNode()) {
                    logger.warn("Operation failed: {}", errorNode.toString());
                    return null;
                }

                JsonNode responseNode = resultNode.path("response");
                if (responseNode.isMissingNode()) {
                    logger.warn("No 'response' in operation result");
                    return null;
                }

                JsonNode imageNode = responseNode.path("image");
                if (imageNode.isMissingNode() || !imageNode.isTextual()) {
                    logger.warn("No 'image' in response or not textual");
                    return null;
                }

                String base64 = imageNode.asText().trim();
                if (base64.isEmpty()) {
                    logger.warn("Base64 image is empty");
                    return null;
                }

                return "image/jpeg;base64," + base64;
            }

            logger.warn("Timeout waiting for image generation (operationId: {})", operationId);
            return null;
        }
    }

    private String createRequestJson(String message) {
        try {
            ObjectNode requestJson = objectMapper.createObjectNode();

            requestJson.put("modelUri", "gpt://" + botConfig.getYandexFolderId() + "/" + botConfig.getYandexModel());

            ObjectNode completionOptions = objectMapper.createObjectNode();
            completionOptions.put("stream", false);
            completionOptions.put("temperature", botConfig.getTemperature());
            completionOptions.put("maxTokens", botConfig.getMaxTokens());
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
