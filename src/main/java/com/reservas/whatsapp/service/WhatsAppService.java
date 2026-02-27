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
}
