package com.reservas.whatsapp.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reservas.whatsapp.service.ConversationService;
import com.reservas.whatsapp.service.WhatsAppService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/webhook")
@RequiredArgsConstructor
@Slf4j
public class WhatsAppWebhookController {
    
    private final ConversationService conversationService;
    private final WhatsAppService whatsAppService;
    private final ObjectMapper objectMapper;
    
    @Value("${whatsapp.api.verify-token}")
    private String verifyToken;
    
    /**
     * Verificación del webhook (requerido por WhatsApp)
     * GET /webhook?hub.mode=subscribe&hub.challenge=123456&hub.verify_token=tu_token
     */
    @GetMapping
    public ResponseEntity<?> verifyWebhook(
            @RequestParam("hub.mode") String mode,
            @RequestParam("hub.challenge") String challenge,
            @RequestParam("hub.verify_token") String token) {
        
        log.info("Verificando webhook - mode: {}, token: {}", mode, token);
        
        if ("subscribe".equals(mode) && verifyToken.equals(token)) {
            log.info("Webhook verificado exitosamente");
            return ResponseEntity.ok(challenge);
        } else {
            log.warn("Verificación de webhook fallida");
            return ResponseEntity.status(403).body("Forbidden");
        }
    }
    
    /**
     * Recibe mensajes de WhatsApp
     * POST /webhook
     */
    @PostMapping
    public ResponseEntity<?> receiveMessage(@RequestBody String payload) {
        try {
            log.debug("Webhook recibido: {}", payload);
            
            JsonNode root = objectMapper.readTree(payload);
            JsonNode entry = root.path("entry");
            
            if (!entry.isArray() || entry.size() == 0) {
                return ResponseEntity.ok().build();
            }
            
            JsonNode changes = entry.get(0).path("changes");
            if (!changes.isArray() || changes.size() == 0) {
                return ResponseEntity.ok().build();
            }
            
            JsonNode value = changes.get(0).path("value");
            JsonNode messages = value.path("messages");
            
            if (!messages.isArray() || messages.size() == 0) {
                // Puede ser una actualización de estado, no un mensaje
                return ResponseEntity.ok().build();
            }
            
            JsonNode message = messages.get(0);
            
            String messageId = message.path("id").asText();
            String from = message.path("from").asText();
            String messageType = message.path("type").asText();
            
            log.info("Mensaje recibido - ID: {}, From: {}, Type: {}", messageId, from, messageType);
            
            // Marcar como leído
            boolean isTestMessage = messageId == null 
                    || messageId.startsWith("wamid.test");

            if (!isTestMessage) {
                whatsAppService.markAsRead(messageId);
            } else {
                log.info("Mensaje de prueba detectado, no se marcará como leído");
            }
            
            // Procesar solo mensajes de texto
            if ("text".equals(messageType)) {
                String text = message.path("text").path("body").asText();
                log.info("Texto del mensaje: {}", text);
                
                // Procesar mensaje de forma asíncrona
                processMessageAsync(from, text);
            } else if ("interactive".equals(messageType)) {
                // Manejar respuestas a botones interactivos
                JsonNode buttonReply = message.path("interactive").path("button_reply");
                String buttonId = buttonReply.path("id").asText();
                String buttonTitle = buttonReply.path("title").asText();
                
                log.info("Botón presionado - ID: {}, Title: {}", buttonId, buttonTitle);
                processMessageAsync(from, buttonTitle);
            }
            
            return ResponseEntity.ok().build();
            
        } catch (Exception e) {
            log.error("Error procesando webhook", e);
            return ResponseEntity.status(500).body("Error procesando mensaje");
        }
    }
    
    /**
     * Procesa el mensaje de forma asíncrona para responder rápido al webhook
     */
    private void processMessageAsync(String phoneNumber, String message) {
        new Thread(() -> {
            try {
                conversationService.processMessage(phoneNumber, message);
            } catch (Exception e) {
                log.error("Error procesando mensaje de {}", phoneNumber, e);
                try {
                    whatsAppService.sendTextMessage(
                            phoneNumber,
                            "Lo siento, ocurrió un error procesando tu mensaje. Por favor intenta nuevamente."
                    );
                } catch (Exception ex) {
                    log.error("Error enviando mensaje de error", ex);
                }
            }
        }).start();
    }
    
    /**
     * Endpoint de salud
     */
    @GetMapping("/health")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok().body("{\"status\":\"ok\"}");
    }

    /**
     * Obtiene la última respuesta del bot para un número de teléfono
     * Usado para testing desde cliente CLI
     */
    @GetMapping("/response/{phoneNumber}")
    public ResponseEntity<?> getLastResponse(@PathVariable String phoneNumber) {
        String response = conversationService.getLastResponse(phoneNumber);
        return ResponseEntity.ok().body(response);
    }

    /**
     * Sincroniza todos los eventos de Google Calendar hacia la tabla de reservas
     * POST /webhook/sync-calendar
     *
     * Respuesta:
     * {
     *   "added": 5,
     *   "skipped": 3,
     *   "errors": 0,
     *   "message": "✅ Sincronización completada:\n- 5 nuevas reservas importadas\n- 3 reservas ya existentes\n- 0 errores"
     * }
     */
    @PostMapping("/sync-calendar")
    public ResponseEntity<?> syncCalendarReservations() {
        log.info("Sincronización de calendario solicitada");
        ConversationService.SyncResult result = conversationService.syncReservationsFromCalendar();

        return ResponseEntity.ok().body(Map.of(
                "added", result.added,
                "skipped", result.skipped,
                "errors", result.errors,
                "message", result.message
        ));
    }
}
