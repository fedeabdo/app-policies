package com.reservas.whatsapp.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "reservations")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Reservation {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String phoneNumber;
    
    @Column(nullable = false)
    private String customerName;
    
    @Column(nullable = false)
    private LocalDateTime reservationDateTime;
    
    @Column(nullable = false)
    private String googleCalendarEventId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReservationStatus status;
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    private LocalDateTime cancelledAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = ReservationStatus.CONFIRMED;
        }
    }
    
    public enum ReservationStatus {
        CONFIRMED,
        CANCELLED,
        COMPLETED
    }
}
