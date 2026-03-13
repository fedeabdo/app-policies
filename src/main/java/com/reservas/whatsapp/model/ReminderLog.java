package com.reservas.whatsapp.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Registra cada recordatorio enviado para una reserva.
 * Permite rastrear qué recordatorios ya se enviaron y cuándo.
 */
@Entity
@Table(name = "reminder_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReminderLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id", nullable = false)
    private Reservation reservation;
    
    /**
     * Número del recordatorio (1, 2, 3, etc.)
     * Basado en la configuración de cuántos recordatorios enviar
     */
    @Column(nullable = false)
    private Integer reminderNumber;
    
    /**
     * Minutos antes de la cita en que se envió este recordatorio
     */
    @Column(nullable = false)
    private Integer minutesBefore;
    
    /**
     * Nombre del template de WhatsApp usado
     */
    @Column(nullable = false)
    private String templateName;
    
    /**
     * Estado del envío
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReminderStatus status;
    
    /**
     * Mensaje de error si falló
     */
    @Column(length = 500)
    private String errorMessage;
    
    /**
     * ID del mensaje de WhatsApp (si fue exitoso)
     */
    private String whatsappMessageId;
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime sentAt;
    
    @PrePersist
    protected void onCreate() {
        sentAt = LocalDateTime.now();
    }
    
    public enum ReminderStatus {
        SENT,
        FAILED,
        PENDING
    }
}
