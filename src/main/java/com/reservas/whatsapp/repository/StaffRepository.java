package com.reservas.whatsapp.repository;

import com.reservas.whatsapp.model.Staff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StaffRepository extends JpaRepository<Staff, Long> {
    
    /**
     * Obtiene todos los profesionales activos ordenados por displayOrder
     */
    List<Staff> findByActiveTrueOrderByDisplayOrderAsc();
    
    /**
     * Busca un profesional por su ID de calendario de Google
     */
    Optional<Staff> findByGoogleCalendarId(String googleCalendarId);
    
    /**
     * Busca un profesional activo por su nombre (case-insensitive, contiene)
     */
    List<Staff> findByActiveTrueAndNameContainingIgnoreCase(String name);
    
    /**
     * Verifica si existe un profesional con el calendar ID dado
     */
    boolean existsByGoogleCalendarId(String googleCalendarId);
}
