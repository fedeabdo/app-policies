package com.reservas.whatsapp.controller;

import com.reservas.whatsapp.config.ReminderConfig;
import com.reservas.whatsapp.model.ReminderLog;
import com.reservas.whatsapp.model.Reservation;
import com.reservas.whatsapp.repository.ReservationRepository;
import com.reservas.whatsapp.service.ReminderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller para probar el sistema de recordatorios durante desarrollo.
 * Solo disponible en perfil "dev" o "test".
 */
@RestController
@RequestMapping("/api/test/reminders")
@RequiredArgsConstructor
@Slf4j
@Profile({"dev", "default"})  // Solo en desarrollo
public class ReminderTestController {
    
    private final ReminderService reminderService;
    private final ReservationRepository reservationRepository;
    
    /**
     * Ver configuración actual de recordatorios
     * GET /api/test/reminders/config
     */
    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> getConfig() {
        ReminderConfig config = reminderService.getConfig();
        
        Map<String, Object> response = new HashMap<>();
        response.put("enabled", config.isEnabled());
        response.put("templateName", config.getTemplateName());
        response.put("templateLanguage", config.getTemplateLanguage());
        response.put("checkIntervalMinutes", config.getCheckIntervalMinutes());
        
        // Convertir tiempos a lista de mapas
        List<Map<String, Object>> times = config.getTimes().stream()
                .map(t -> {
                    Map<String, Object> time = new HashMap<>();
                    time.put("minutesBefore", t.getMinutesBefore());
                    time.put("enabled", t.isEnabled());
                    time.put("description", t.getDescription());
                    time.put("templateName", t.getTemplateName());
                    return time;
                })
                .toList();
        response.put("times", times);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Enviar recordatorio manual para una reserva específica
     * POST /api/test/reminders/send/{reservationId}
     */
    @PostMapping("/send/{reservationId}")
    public ResponseEntity<Map<String, Object>> sendTestReminder(@PathVariable Long reservationId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            ReminderLog result = reminderService.sendManualReminder(reservationId);
            
            if (result != null) {
                response.put("success", true);
                response.put("status", result.getStatus().name());
                response.put("messageId", result.getWhatsappMessageId());
                response.put("error", result.getErrorMessage());
            } else {
                response.put("success", false);
                response.put("error", "No se pudo crear el log del recordatorio");
            }
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Forzar ejecución del proceso de recordatorios
     * POST /api/test/reminders/process
     */
    @PostMapping("/process")
    public ResponseEntity<Map<String, String>> triggerProcess() {
        Map<String, String> response = new HashMap<>();
        
        try {
            reminderService.processReminders();
            response.put("status", "ok");
            response.put("message", "Proceso de recordatorios ejecutado");
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Ver historial de recordatorios de una reserva
     * GET /api/test/reminders/history/{reservationId}
     */
    @GetMapping("/history/{reservationId}")
    public ResponseEntity<List<ReminderLog>> getHistory(@PathVariable Long reservationId) {
        return ResponseEntity.ok(reminderService.getReminderHistory(reservationId));
    }
    
    /**
     * Listar reservas confirmadas (para saber qué IDs usar)
     * GET /api/test/reminders/reservations
     */
    @GetMapping("/reservations")
    public ResponseEntity<List<Reservation>> getConfirmedReservations() {
        return ResponseEntity.ok(
            reservationRepository.findByStatus(Reservation.ReservationStatus.CONFIRMED)
        );
    }
}
