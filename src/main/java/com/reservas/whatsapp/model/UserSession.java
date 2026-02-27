package com.reservas.whatsapp.model;

import org.hibernate.annotations.UpdateTimestamp;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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
    
    private String userName;
    
    @Column(columnDefinition = "TEXT")
    private String availableSlots;
    
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
        ESPERANDO_HORARIO,
        ESPERANDO_NOMBRE,
        ESPERANDO_CONFIRMACION
    }
}
