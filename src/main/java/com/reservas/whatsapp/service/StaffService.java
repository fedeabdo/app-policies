package com.reservas.whatsapp.service;

import com.reservas.whatsapp.model.Staff;
import com.reservas.whatsapp.repository.StaffRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class StaffService {

    private final StaffRepository staffRepository;

    /**
     * Obtiene todos los profesionales activos
     */
    public List<Staff> getActiveStaff() {
        return staffRepository.findByActiveTrueOrderByDisplayOrderAsc();
    }

    /**
     * Obtiene un profesional por su ID
     */
    public Optional<Staff> getStaffById(Long id) {
        return staffRepository.findById(id);
    }

    /**
     * Obtiene un profesional por su calendar ID de Google
     */
    public Optional<Staff> getStaffByCalendarId(String calendarId) {
        return staffRepository.findByGoogleCalendarId(calendarId);
    }

    /**
     * Busca profesionales por nombre
     */
    public List<Staff> searchStaffByName(String name) {
        return staffRepository.findByActiveTrueAndNameContainingIgnoreCase(name);
    }

    /**
     * Obtiene solo los IDs de calendario de los profesionales activos
     */
    public List<String> getActiveCalendarIds() {
        return getActiveStaff().stream()
                .map(Staff::getGoogleCalendarId)
                .toList();
    }

    /**
     * Obtiene solo los nombres de los profesionales activos
     */
    public List<String> getActiveStaffNames() {
        return getActiveStaff().stream()
                .map(Staff::getName)
                .toList();
    }

    /**
     * Obtiene el nombre de un profesional por su calendar ID
     */
    public String getStaffNameByCalendarId(String calendarId) {
        return staffRepository.findByGoogleCalendarId(calendarId)
                .map(Staff::getName)
                .orElse("Profesional no encontrado");
    }

    /**
     * Crea o actualiza un profesional
     */
    @Transactional
    public Staff saveStaff(Staff staff) {
        log.info("Guardando profesional: {}", staff.getName());
        return staffRepository.save(staff);
    }

    /**
     * Crea un nuevo profesional con los datos básicos
     */
    @Transactional
    public Staff createStaff(String name, String googleCalendarId) {
        return createStaff(name, googleCalendarId, null);
    }

    /**
     * Crea un nuevo profesional
     */
    @Transactional
    public Staff createStaff(String name, String googleCalendarId, String email) {
        if (staffRepository.existsByGoogleCalendarId(googleCalendarId)) {
            throw new IllegalArgumentException("Ya existe un profesional con ese calendario");
        }

        Staff staff = new Staff();
        staff.setName(name);
        staff.setGoogleCalendarId(googleCalendarId);
        staff.setEmail(email);
        staff.setActive(true);
        staff.setDisplayOrder(getActiveStaff().size());

        return staffRepository.save(staff);
    }

    /**
     * Desactiva un profesional (no lo elimina)
     */
    @Transactional
    public void deactivateStaff(Long id) {
        staffRepository.findById(id).ifPresent(staff -> {
            staff.setActive(false);
            staffRepository.save(staff);
            log.info("Profesional desactivado: {}", staff.getName());
        });
    }

    /**
     * Activa un profesional
     */
    @Transactional
    public void activateStaff(Long id) {
        staffRepository.findById(id).ifPresent(staff -> {
            staff.setActive(true);
            staffRepository.save(staff);
            log.info("Profesional activado: {}", staff.getName());
        });
    }

    /**
     * Verifica si hay profesionales configurados
     */
    public boolean hasActiveStaff() {
        return !getActiveStaff().isEmpty();
    }
}
