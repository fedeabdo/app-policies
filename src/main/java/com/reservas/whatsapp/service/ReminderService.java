package com.reservas.whatsapp.service;

import com.reservas.whatsapp.config.ReminderConfig;
import com.reservas.whatsapp.model.ReminderLog;
import com.reservas.whatsapp.model.Reservation;
import com.reservas.whatsapp.model.Staff;
import com.reservas.whatsapp.repository.ReminderLogRepository;
import com.reservas.whatsapp.repository.ReservationRepository;
import com.reservas.whatsapp.repository.StaffRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Servicio para gestionar y enviar recordatorios de reservas.
 * 
 * Funcionalidades:
 * - Envío automático de recordatorios según configuración
 * - Soporte para múltiples recordatorios por reserva
 * - Uso de templates de WhatsApp Business API
 * - Registro de todos los recordatorios enviados
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReminderService {
    
    private final ReservationRepository reservationRepository;
    private final ReminderLogRepository reminderLogRepository;
    private final StaffRepository staffRepository;
    private final WhatsAppService whatsAppService;
    private final ReminderConfig reminderConfig;
    
    private static final DateTimeFormatter DATE_FORMATTER = 
        DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", new Locale("es", "ES"));
    private static final DateTimeFormatter TIME_FORMATTER = 
        DateTimeFormatter.ofPattern("HH:mm");
    
    /**
     * Tarea programada que revisa y envía recordatorios pendientes.
     * Se ejecuta según el intervalo configurado (por defecto cada 5 minutos).
     */
    @Scheduled(fixedRateString = "#{${reminder.check-interval-minutes:5} * 60000}")
    @Transactional
    public void processReminders() {
        if (!reminderConfig.isEnabled()) {
            return;
        }
        
        log.debug("Iniciando proceso de recordatorios...");
        
        List<ReminderConfig.ReminderTime> enabledReminders = reminderConfig.getEnabledReminders();
        
        if (enabledReminders.isEmpty()) {
            log.debug("No hay recordatorios configurados");
            return;
        }
        
        // Obtener reservas confirmadas que podrían necesitar recordatorios
        LocalDateTime now = LocalDateTime.now();
        
        // Buscar el recordatorio con más anticipación para definir el rango de búsqueda
        int maxMinutesBefore = enabledReminders.stream()
                .mapToInt(ReminderConfig.ReminderTime::getMinutesBefore)
                .max()
                .orElse(1440); // 24 horas por defecto
        
        LocalDateTime searchEnd = now.plusMinutes(maxMinutesBefore + 10);
        
        List<Reservation> upcomingReservations = reservationRepository
                .findByReservationDateTimeBetween(now, searchEnd);
        
        // Filtrar solo las confirmadas
        List<Reservation> confirmedReservations = upcomingReservations.stream()
                .filter(r -> r.getStatus() == Reservation.ReservationStatus.CONFIRMED)
                .toList();
        
        log.debug("Encontradas {} reservas confirmadas para revisar recordatorios", 
                  confirmedReservations.size());
        
        for (Reservation reservation : confirmedReservations) {
            processReservationReminders(reservation, enabledReminders, now);
        }
    }
    
    /**
     * Procesa los recordatorios para una reserva específica
     */
    private void processReservationReminders(
            Reservation reservation, 
            List<ReminderConfig.ReminderTime> reminders,
            LocalDateTime now) {
        
        LocalDateTime reservationTime = reservation.getReservationDateTime();
        
        for (int i = 0; i < reminders.size(); i++) {
            ReminderConfig.ReminderTime reminderTime = reminders.get(i);
            int reminderNumber = i + 1;
            
            // Calcular cuándo se debe enviar este recordatorio
            LocalDateTime sendAt = reservationTime.minusMinutes(reminderTime.getMinutesBefore());
            
            // Verificar si es momento de enviar (con una ventana de tolerancia)
            LocalDateTime windowStart = sendAt.minusMinutes(reminderConfig.getCheckIntervalMinutes());
            LocalDateTime windowEnd = sendAt.plusMinutes(reminderConfig.getCheckIntervalMinutes());
            
            if (now.isAfter(windowStart) && now.isBefore(windowEnd)) {
                // Verificar si ya se envió este recordatorio
                if (!reminderLogRepository.existsByReservationAndMinutesBefore(
                        reservation, reminderTime.getMinutesBefore())) {
                    
                    sendReminder(reservation, reminderTime, reminderNumber);
                }
            }
        }
    }
    
    /**
     * Envía un recordatorio específico para una reserva
     * @return El log del recordatorio guardado
     */
    @Transactional
    public ReminderLog sendReminder(
            Reservation reservation, 
            ReminderConfig.ReminderTime reminderTime,
            int reminderNumber) {
        
        String templateName = reminderConfig.getTemplateForReminder(reminderTime);
        
        log.info("Enviando recordatorio #{} para reserva {} ({}min antes)", 
                 reminderNumber, reservation.getId(), reminderTime.getMinutesBefore());
        
        ReminderLog reminderLog = new ReminderLog();
        reminderLog.setReservation(reservation);
        reminderLog.setReminderNumber(reminderNumber);
        reminderLog.setMinutesBefore(reminderTime.getMinutesBefore());
        reminderLog.setTemplateName(templateName);
        reminderLog.setStatus(ReminderLog.ReminderStatus.PENDING);
        
        try {
            // Construir parámetros según configuración del template
            List<String> params = buildTemplateParams(reservation, templateName, reminderTime);
            
            // Obtener idioma específico del template o el global
            String language = reminderConfig.getLanguageForTemplate(templateName);
            
            // Enviar mensaje con template
            String messageId = whatsAppService.sendTemplateMessage(
                    reservation.getPhoneNumber(),
                    templateName,
                    language,
                    params
            );
            
            reminderLog.setStatus(ReminderLog.ReminderStatus.SENT);
            reminderLog.setWhatsappMessageId(messageId);
            
            log.info("Recordatorio enviado exitosamente. ID: {}", messageId);
            
        } catch (Exception e) {
            log.error("Error al enviar recordatorio para reserva {}: {}", 
                      reservation.getId(), e.getMessage());
            reminderLog.setStatus(ReminderLog.ReminderStatus.FAILED);
            reminderLog.setErrorMessage(e.getMessage());
        }
        
        return reminderLogRepository.save(reminderLog);
    }
    
    /**
     * Envía un recordatorio manual para una reserva
     */
    @Transactional
    public ReminderLog sendManualReminder(Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("Reserva no encontrada: " + reservationId));
        
        if (reservation.getStatus() != Reservation.ReservationStatus.CONFIRMED) {
            throw new IllegalStateException("Solo se pueden enviar recordatorios para reservas confirmadas");
        }
        
        // Calcular minutos reales restantes hasta la reserva
        long minutesUntilReservation = java.time.Duration.between(
                LocalDateTime.now(), 
                reservation.getReservationDateTime()
        ).toMinutes();
        
        // No enviar recordatorio si la reserva ya pasó
        if (minutesUntilReservation < 0) {
            throw new IllegalStateException("No se puede enviar recordatorio: la reserva ya pasó (hace " + 
                    Math.abs(minutesUntilReservation) + " minutos)");
        }
        
        // Contar recordatorios previos
        Long sentCount = reminderLogRepository.countSentReminders(reservation);
        int reminderNumber = sentCount.intValue() + 1;
        
        // Usar el tiempo real restante para el recordatorio manual
        ReminderConfig.ReminderTime manualReminder = new ReminderConfig.ReminderTime();
        manualReminder.setMinutesBefore((int) minutesUntilReservation);
        manualReminder.setEnabled(true);
        manualReminder.setDescription("Recordatorio manual");
        
        return sendReminder(reservation, manualReminder, reminderNumber);
    }
    
    /**
     * Obtiene el historial de recordatorios de una reserva
     */
    public List<ReminderLog> getReminderHistory(Long reservationId) {
        return reminderLogRepository.findByReservationId(reservationId);
    }
    
    /**
     * Formatea el tiempo restante de forma amigable
     */
    private String formatTimeRemaining(int minutes) {
        if (minutes >= 1440) {
            int days = minutes / 1440;
            return days == 1 ? "1 día" : days + " días";
        } else if (minutes >= 60) {
            int hours = minutes / 60;
            return hours == 1 ? "1 hora" : hours + " horas";
        } else {
            return minutes == 1 ? "1 minuto" : minutes + " minutos";
        }
    }
    
    /**
     * Construye la lista de parámetros para el template según la configuración.
     * 
     * Campos disponibles:
     * - customer_name: Nombre del cliente
     * - staff_name: Nombre del profesional
     * - date: Fecha formateada
     * - time: Hora
     * - time_remaining: Tiempo restante (ej: "24 horas")
     */
    private List<String> buildTemplateParams(
            Reservation reservation, 
            String templateName,
            ReminderConfig.ReminderTime reminderTime) {
        
        // Preparar todos los valores disponibles
        String customerName = reservation.getCustomerName();
        String dateFormatted = reservation.getReservationDateTime().format(DATE_FORMATTER);
        String timeFormatted = reservation.getReservationDateTime().format(TIME_FORMATTER);
        String staffName = staffRepository.findByGoogleCalendarId(reservation.getCalendarId())
                .map(Staff::getName)
                .orElse("tu profesional");
        String timeRemaining = formatTimeRemaining(reminderTime.getMinutesBefore());
        
        // Buscar configuración del template
        ReminderConfig.TemplateParams templateParams = reminderConfig.getTemplates().get(templateName);
        
        if (templateParams == null || templateParams.getParams().isEmpty()) {
            // Configuración por defecto si no hay config específica
            log.debug("No hay configuración para template '{}', usando orden por defecto", templateName);
            return List.of(customerName, staffName, dateFormatted, timeFormatted);
        }
        
        // Construir parámetros según configuración
        List<String> params = new java.util.ArrayList<>();
        
        for (String paramName : templateParams.getParams()) {
            String value = switch (paramName.toLowerCase().trim()) {
                case "customer_name", "nombre", "name" -> customerName;
                case "staff_name", "profesional", "staff" -> staffName;
                case "date", "fecha" -> dateFormatted;
                case "time", "hora" -> timeFormatted;
                case "time_remaining", "tiempo_restante", "falta" -> timeRemaining;
                default -> {
                    log.warn("Parámetro desconocido en template '{}': {}", templateName, paramName);
                    yield paramName; // Devuelve el nombre literal si no se reconoce
                }
            };
            params.add(value);
        }
        
        log.debug("Template '{}' con {} parámetros: {}", templateName, params.size(), params);
        return params;
    }
    
    /**
     * Verifica la configuración actual de recordatorios
     */
    public ReminderConfig getConfig() {
        return reminderConfig;
    }
}
