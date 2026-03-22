package com.reservas.whatsapp.model;

import java.time.LocalDateTime;

import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_sessions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ConversationState state;

    private LocalDateTime selectedDate;

    private String selectedTime;

    private String selectedService;

    private String userName;

    @Column(columnDefinition = "TEXT")
    private String availableSlots;

    // Datos para cancelación de reservas
    @Column(columnDefinition = "TEXT")
    private String reservationsToCancel;

    private String selectedCalendarId;

    private Long selectedReservationId;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum ConversationState {
        INICIO,
        ESPERANDO_FECHA,
        ESPERANDO_SERVICIO,
        ESPERANDO_PELUQUERO,
        ESPERANDO_HORARIO,
        ESPERANDO_NOMBRE,
        ESPERANDO_CONFIRMACION,
        MOSTRANDO_RESERVAS,
        ESPERANDO_CONFIRMACION_CANCELACION
    }
}
