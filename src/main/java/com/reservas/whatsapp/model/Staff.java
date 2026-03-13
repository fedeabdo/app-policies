package com.reservas.whatsapp.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entidad que representa a un profesional/peluquero del negocio.
 * Cada profesional tiene asociado un calendario de Google donde se gestionan sus citas.
 */
@Entity
@Table(name = "staff")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Staff {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String name;
    
    @Column
    private String email;
    
    /**
     * ID del calendario de Google asociado a este profesional.
     * Ejemplo: "abc123@group.calendar.google.com"
     */
    @Column(nullable = false, unique = true)
    private String googleCalendarId;
    
    /**
     * Indica si el profesional está activo y puede recibir reservas.
     */
    @Column(nullable = false)
    private boolean active = true;
    
    /**
     * Orden de aparición en la lista (menor = primero)
     */
    @Column(nullable = false)
    private int displayOrder = 0;
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
