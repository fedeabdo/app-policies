package com.reservas.whatsapp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class WhatsAppService {
    
    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;
    
    @Value("${whatsapp.api.url}")
    private String whatsappApiUrl;
    
    @Value("${whatsapp.api.phone-number-id}")
    private String phoneNumberId;
    
    @Value("${whatsapp.api.access-token}")
    private String accessToken;
    
    /**
     * Envía un mensaje de texto por WhatsApp
     */
    public void sendTextMessage(String to, String message) {
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("messaging_product", "whatsapp");
            requestBody.put("recipient_type", "individual");
            requestBody.put("to", to);
            requestBody.put("type", "text");
            
            Map<String, String> text = new HashMap<>();
            text.put("preview_url", "false");
            text.put("body", message);
            requestBody.put("text", text);
            
            String url = String.format("%s/%s/messages", whatsappApiUrl, phoneNumberId);
            
            log.info("Enviando mensaje a WhatsApp API");
            log.info("URL: {}", url);
            log.info("To (número): {}", to);
            log.info("Request Body: {}", objectMapper.writeValueAsString(requestBody));
            
            WebClient webClient = webClientBuilder.build();
            
            String response = webClient.post()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            
            log.info("Mensaje enviado a {}: {}", to, response);
            
        } catch (Exception e) {
            log.error("Error al enviar mensaje de WhatsApp", e);
            throw new RuntimeException("Error al enviar mensaje", e);
        }
    }
    
    /**
     * Envía un mensaje con botones interactivos
     */
    public void sendInteractiveMessage(String to, String bodyText, Map<String, String> buttons) {
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("messaging_product", "whatsapp");
            requestBody.put("recipient_type", "individual");
            requestBody.put("to", to);
            requestBody.put("type", "interactive");
            
            Map<String, Object> interactive = new HashMap<>();
            interactive.put("type", "button");
            
            Map<String, String> body = new HashMap<>();
            body.put("text", bodyText);
            interactive.put("body", body);
            
            // Crear botones (máximo 3)
            java.util.List<Map<String, Object>> buttonList = new java.util.ArrayList<>();
            int count = 0;
            for (Map.Entry<String, String> entry : buttons.entrySet()) {
                if (count >= 3) break;
                
                Map<String, Object> button = new HashMap<>();
                button.put("type", "reply");
                
                Map<String, String> reply = new HashMap<>();
                reply.put("id", entry.getKey());
                reply.put("title", entry.getValue());
                button.put("reply", reply);
                
                buttonList.add(button);
                count++;
            }
            
            Map<String, Object> action = new HashMap<>();
            action.put("buttons", buttonList);
            interactive.put("action", action);
            
            requestBody.put("interactive", interactive);
            
            String url = String.format("%s/%s/messages", whatsappApiUrl, phoneNumberId);
            
            WebClient webClient = webClientBuilder.build();
            
            String response = webClient.post()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            
            log.info("Mensaje interactivo enviado a {}: {}", to, response);
            
        } catch (Exception e) {
            log.error("Error al enviar mensaje interactivo", e);
            throw new RuntimeException("Error al enviar mensaje interactivo", e);
        }
    }
    
    /**
     * Marca un mensaje como leído
     */
    public void markAsRead(String messageId) {
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("messaging_product", "whatsapp");
            requestBody.put("status", "read");
            requestBody.put("message_id", messageId);
            
            String url = String.format("%s/%s/messages", whatsappApiUrl, phoneNumberId);
            
            WebClient webClient = webClientBuilder.build();
            
            webClient.post()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .block();
            
            log.debug("Mensaje marcado como leído: {}", messageId);
            
        } catch (Exception e) {
            log.warn("Error al marcar mensaje como leído", e);
        }
    }
    
    /**
     * Envía un mensaje usando un template de WhatsApp Business API.
     * Los templates deben estar previamente aprobados por Meta.
     * 
     * @param to Número de teléfono del destinatario (con código de país)
     * @param templateName Nombre del template aprobado
     * @param languageCode Código de idioma (ej: "es", "en", "pt")
     * @param parameters Lista de parámetros para el template (en orden)
     * @return ID del mensaje enviado
     */
    public String sendTemplateMessage(String to, String templateName, String languageCode, 
                                       java.util.List<String> parameters) {
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("messaging_product", "whatsapp");
            requestBody.put("recipient_type", "individual");
            requestBody.put("to", to);
            requestBody.put("type", "template");
            
            // Configurar template
            Map<String, Object> template = new HashMap<>();
            template.put("name", templateName);
            
            Map<String, String> language = new HashMap<>();
            language.put("code", languageCode);
            template.put("language", language);
            
            // Agregar componentes con parámetros si hay
            if (parameters != null && !parameters.isEmpty()) {
                java.util.List<Map<String, Object>> components = new java.util.ArrayList<>();
                
                Map<String, Object> bodyComponent = new HashMap<>();
                bodyComponent.put("type", "body");
                
                java.util.List<Map<String, Object>> bodyParameters = new java.util.ArrayList<>();
                for (String param : parameters) {
                    Map<String, Object> paramMap = new HashMap<>();
                    paramMap.put("type", "text");
                    paramMap.put("text", param);
                    bodyParameters.add(paramMap);
                }
                bodyComponent.put("parameters", bodyParameters);
                
                components.add(bodyComponent);
                template.put("components", components);
            }
            
            requestBody.put("template", template);
            
            String url = String.format("%s/%s/messages", whatsappApiUrl, phoneNumberId);
            
            log.info("Enviando template '{}' a {}", templateName, to);
            log.debug("Request Body: {}", objectMapper.writeValueAsString(requestBody));
            
            WebClient webClient = webClientBuilder.build();
            
            String response = webClient.post()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            
            log.info("Template enviado exitosamente a {}: {}", to, response);
            
            // Extraer el message ID de la respuesta
            JsonNode responseJson = objectMapper.readTree(response);
            if (responseJson.has("messages") && responseJson.get("messages").isArray()) {
                return responseJson.get("messages").get(0).get("id").asText();
            }
            
            return null;
            
        } catch (Exception e) {
            log.error("Error al enviar template de WhatsApp", e);
            throw new RuntimeException("Error al enviar template: " + e.getMessage(), e);
        }
    }
    
    /**
     * Envía un mensaje de template con botones de acción rápida.
     * Útil para recordatorios que permiten confirmar o cancelar.
     * 
     * @param to Número de teléfono del destinatario
     * @param templateName Nombre del template
     * @param languageCode Código de idioma
     * @param bodyParameters Parámetros para el cuerpo del template
     * @param buttonParameters Parámetros para los botones (payload de quick reply)
     * @return ID del mensaje enviado
     */
    public String sendTemplateWithButtons(String to, String templateName, String languageCode,
                                          java.util.List<String> bodyParameters,
                                          java.util.List<String> buttonParameters) {
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("messaging_product", "whatsapp");
            requestBody.put("recipient_type", "individual");
            requestBody.put("to", to);
            requestBody.put("type", "template");
            
            Map<String, Object> template = new HashMap<>();
            template.put("name", templateName);
            
            Map<String, String> language = new HashMap<>();
            language.put("code", languageCode);
            template.put("language", language);
            
            java.util.List<Map<String, Object>> components = new java.util.ArrayList<>();
            
            // Agregar parámetros del body si hay
            if (bodyParameters != null && !bodyParameters.isEmpty()) {
                Map<String, Object> bodyComponent = new HashMap<>();
                bodyComponent.put("type", "body");
                
                java.util.List<Map<String, Object>> params = new java.util.ArrayList<>();
                for (String param : bodyParameters) {
                    Map<String, Object> paramMap = new HashMap<>();
                    paramMap.put("type", "text");
                    paramMap.put("text", param);
                    params.add(paramMap);
                }
                bodyComponent.put("parameters", params);
                components.add(bodyComponent);
            }
            
            // Agregar parámetros de los botones
            if (buttonParameters != null) {
                for (int i = 0; i < buttonParameters.size(); i++) {
                    Map<String, Object> buttonComponent = new HashMap<>();
                    buttonComponent.put("type", "button");
                    buttonComponent.put("sub_type", "quick_reply");
                    buttonComponent.put("index", String.valueOf(i));
                    
                    java.util.List<Map<String, Object>> buttonParams = new java.util.ArrayList<>();
                    Map<String, Object> payloadParam = new HashMap<>();
                    payloadParam.put("type", "payload");
                    payloadParam.put("payload", buttonParameters.get(i));
                    buttonParams.add(payloadParam);
                    
                    buttonComponent.put("parameters", buttonParams);
                    components.add(buttonComponent);
                }
            }
            
            if (!components.isEmpty()) {
                template.put("components", components);
            }
            
            requestBody.put("template", template);
            
            String url = String.format("%s/%s/messages", whatsappApiUrl, phoneNumberId);
            
            log.info("Enviando template con botones '{}' a {}", templateName, to);
            
            WebClient webClient = webClientBuilder.build();
            
            String response = webClient.post()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            
            log.info("Template con botones enviado a {}: {}", to, response);
            
            JsonNode responseJson = objectMapper.readTree(response);
            if (responseJson.has("messages") && responseJson.get("messages").isArray()) {
                return responseJson.get("messages").get(0).get("id").asText();
            }
            
            return null;
            
        } catch (Exception e) {
            log.error("Error al enviar template con botones", e);
            throw new RuntimeException("Error al enviar template con botones: " + e.getMessage(), e);
        }
    }
}
