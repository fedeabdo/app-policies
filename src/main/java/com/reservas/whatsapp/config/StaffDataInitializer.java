package com.reservas.whatsapp.config;

import com.google.api.services.calendar.Calendar;
import com.reservas.whatsapp.model.Staff;
import com.reservas.whatsapp.repository.StaffRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Componente de inicialización que migra automáticamente los calendarios
 * desde la configuración (application.properties) a la tabla staff en BD.
 * 
 * Obtiene los nombres directamente del calendario de Google.
 * Solo se ejecuta si la tabla staff está vacía.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StaffDataInitializer implements CommandLineRunner {

    private final StaffRepository staffRepository;
    private final Calendar googleCalendar;
    
    @Value("#{'${google.calendar.calendar-ids}'.split(',\\s*')}")
    private List<String> calendarIds;
    
    @Value("${staff.auto-import:true}")
    private boolean autoImport;

    @Override
    public void run(String... args) {
        if (!autoImport) {
            log.info("Auto-import de staff deshabilitado");
            return;
        }
        
        if (staffRepository.count() > 0) {
            log.info("Ya existen {} profesionales en la BD, omitiendo importación", 
                    staffRepository.count());
            return;
        }

        if (calendarIds == null || calendarIds.isEmpty()) {
            log.warn("No hay calendar IDs configurados para importar");
            return;
        }

        log.info("Importando {} calendarios desde configuración a tabla staff...", 
                calendarIds.size());

        for (int i = 0; i < calendarIds.size(); i++) {
            String calendarId = calendarIds.get(i).trim();
            if (calendarId.isEmpty()) continue;

            try {
                // Obtener nombre del calendario directamente desde Google
                String calendarName = getCalendarName(calendarId);
                
                Staff staff = new Staff();
                staff.setName(calendarName);
                staff.setGoogleCalendarId(calendarId);
                staff.setActive(true);
                staff.setDisplayOrder(i);
                
                staffRepository.save(staff);
                log.info("✓ Profesional '{}' importado desde calendario de Google", calendarName);
                
            } catch (Exception e) {
                log.error("Error al importar calendario {}: {}", calendarId, e.getMessage());
            }
        }

        log.info("Importación completada. Total profesionales: {}", staffRepository.count());
    }
    
    /**
     * Obtiene el nombre (summary) del calendario desde Google Calendar API
     */
    private String getCalendarName(String calendarId) {
        try {
            String name = googleCalendar.calendars().get(calendarId).execute().getSummary();
            return name != null && !name.isBlank() ? name : "Profesional";
        } catch (Exception e) {
            log.warn("No se pudo obtener nombre del calendario {}: {}", calendarId, e.getMessage());
            return "Profesional";
        }
    }
}
