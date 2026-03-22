package com.reservas.whatsapp.service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.EventReminder;
import com.google.api.services.calendar.model.Events;
import com.reservas.whatsapp.model.Staff;
import com.reservas.whatsapp.repository.StaffRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class GoogleCalendarService {

    private final Calendar calendar;
    private final StaffRepository staffRepository;

    @Value("#{'${google.calendar.calendar-ids}'.split(',\\s*')}")
    private List<String> calendarIds;

    @Value("${business.hours.start}")
    private int businessHoursStart;

    @Value("${business.hours.end}")
    private int businessHoursEnd;

    @Value("${business.hours.reservation-duration-minutes}")
    private int reservationDuration;

    @Value("${business.timezone}")
    private String timezone;

    /**
     * Obtiene los horarios disponibles para una fecha específica
     */
    public List<String> getAvailableSlots(String calendarId, LocalDate date) {
        return getAvailableSlots(calendarId, date, reservationDuration);
    }

    /**
     * Obtiene los horarios disponibles para una fecha específica según duración.
     */
    public List<String> getAvailableSlots(String calendarId, LocalDate date, int durationMinutes) {
        try {
            // Obtener eventos del día
            LocalDateTime startOfDay = date.atStartOfDay();
            LocalDateTime endOfDay = date.atTime(23, 59, 59);

            DateTime startDateTime = new DateTime(startOfDay.atZone(ZoneId.of(timezone)).toInstant().toEpochMilli());
            DateTime endDateTime = new DateTime(endOfDay.atZone(ZoneId.of(timezone)).toInstant().toEpochMilli());

            Events events = calendar.events().list(calendarId)
                    .setTimeMin(startDateTime)
                    .setTimeMax(endDateTime)
                    .setOrderBy("startTime")
                    .setSingleEvents(true)
                    .execute();

            // Generar todos los slots posibles
            List<String> allSlots = generateAllSlots(durationMinutes);

            // Filtrar slots ocupados
            List<String> availableSlots = new ArrayList<>(allSlots);

            // Si la fecha es hoy, remover horarios pasados
            LocalDateTime now = LocalDateTime.now(ZoneId.of(timezone));
            if (date.isEqual(now.toLocalDate())) {
                availableSlots.removeIf(slot -> {
                    LocalDateTime slotStart = date.atTime(LocalTime.parse(slot));
                    return !slotStart.isAfter(now);
                });
            }

            for (Event event : events.getItems()) {
                LocalDateTime eventStart = convertToLocalDateTime(event.getStart().getDateTime());
                LocalDateTime eventEnd = convertToLocalDateTime(event.getEnd().getDateTime());

                availableSlots.removeIf(slot -> {
                    LocalTime slotTime = LocalTime.parse(slot);
                    LocalDateTime slotStart = date.atTime(slotTime);
                    LocalDateTime slotEnd = slotStart.plusMinutes(durationMinutes);

                    // Verificar solapamiento
                    return !(slotEnd.isBefore(eventStart) || slotStart.isAfter(eventEnd)
                            || slotStart.isEqual(eventEnd));
                });
            }

            return availableSlots;

        } catch (IOException e) {
            log.error("Error al obtener horarios disponibles", e);
            return new ArrayList<>();
        }
    }

    /**
     * Crea una reserva en Google Calendar
     */
    public String createReservation(String calendarId, LocalDateTime dateTime, String customerName,
            String phoneNumber) {
        return createReservation(calendarId, dateTime, customerName, phoneNumber, null, reservationDuration);
    }

    /**
     * Crea una reserva en Google Calendar con duración y servicio personalizados.
     */
    public String createReservation(String calendarId,
            LocalDateTime dateTime,
            String customerName,
            String phoneNumber,
            String serviceName,
            int durationMinutes) {
        try {
            String description = "Cliente: " + customerName + "\nTeléfono: " + phoneNumber;
            if (serviceName != null && !serviceName.isBlank()) {
                description += "\nServicio: " + serviceName;
            }

            Event event = new Event()
                    .setSummary("Reserva - " + customerName)
                    .setDescription(description);

            LocalDateTime endDateTime = dateTime.plusMinutes(durationMinutes);

            EventDateTime start = new EventDateTime()
                    .setDateTime(new DateTime(dateTime.atZone(ZoneId.of(timezone)).toInstant().toEpochMilli()))
                    .setTimeZone(timezone);

            EventDateTime end = new EventDateTime()
                    .setDateTime(new DateTime(endDateTime.atZone(ZoneId.of(timezone)).toInstant().toEpochMilli()))
                    .setTimeZone(timezone);

            event.setStart(start);
            event.setEnd(end);

            // Configurar recordatorios
            Event.Reminders reminders = new Event.Reminders()
                    .setUseDefault(false)
                    .setOverrides(Arrays.asList(
                            new EventReminder().setMethod("popup").setMinutes(60),
                            new EventReminder().setMethod("popup").setMinutes(24 * 60)));
            event.setReminders(reminders);

            Event createdEvent = calendar.events().insert(calendarId, event).execute();

            log.info("Evento creado: {}", createdEvent.getId());
            return createdEvent.getId();

        } catch (IOException e) {
            log.error("Error al crear reserva en Calendar", e);
            throw new RuntimeException("Error al crear reserva", e);
        }
    }

    /**
     * Cancela una reserva en Google Calendar
     */
    public void cancelReservation(String calendarId, String eventId) {
        try {
            calendar.events().delete(calendarId, eventId).execute();
            log.info("Evento cancelado: {}", eventId);
        } catch (IOException e) {
            log.error("Error al cancelar reserva", e);
            throw new RuntimeException("Error al cancelar reserva", e);
        }
    }

    /**
     * Genera todos los slots horarios posibles del día
     */
    private List<String> generateAllSlots(int durationMinutes) {
        List<String> slots = new ArrayList<>();
        LocalTime start = LocalTime.of(businessHoursStart, 0);
        LocalTime end = LocalTime.of(businessHoursEnd, 0);
        LocalTime current = start;

        while (!current.plusMinutes(durationMinutes).isAfter(end)) {
            slots.add(current.format(DateTimeFormatter.ofPattern("HH:mm")));
            current = current.plusMinutes(30);
        }
        return slots;
    }

    /**
     * Convierte DateTime de Google a LocalDateTime
     */
    private LocalDateTime convertToLocalDateTime(DateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(dateTime.getValue()),
                ZoneId.of(timezone));
    }

    /**
     * Obtiene todos los eventos del calendario (para sincronización)
     */
    public List<Event> getAllEvents(String calendarId) {
        try {
            Events events = calendar.events().list(calendarId)
                    .setOrderBy("startTime")
                    .setSingleEvents(true)
                    .execute();

            return events.getItems() != null ? events.getItems() : new ArrayList<>();
        } catch (IOException e) {
            log.error("Error al obtener todos los eventos", e);
            return new ArrayList<>();
        }
    }

    /**
     * Formatea una fecha para mostrar al usuario
     */
    public String formatDate(LocalDate date) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        return date.format(formatter);
    }

    /**
     * Obtiene los IDs de calendario de los profesionales activos.
     * Usa la BD como fuente principal, con fallback a la configuración.
     */
    public List<String> getCalendarIds() {
        List<Staff> activeStaff = staffRepository.findByActiveTrueOrderByDisplayOrderAsc();
        if (!activeStaff.isEmpty()) {
            return activeStaff.stream()
                    .map(Staff::getGoogleCalendarId)
                    .toList();
        }
        // Fallback a configuración (para compatibilidad)
        log.warn("No hay profesionales en BD, usando configuración de properties");
        return calendarIds;
    }

    /**
     * Obtiene el nombre de un profesional por su calendar ID.
     * Primero busca en la BD, luego consulta Google Calendar como fallback.
     */
    public String getCalendarNameById(String calendarId) {
        // Primero buscar en BD
        Optional<Staff> staff = staffRepository.findByGoogleCalendarId(calendarId);
        if (staff.isPresent()) {
            return staff.get().getName();
        }

        // Fallback: consultar Google Calendar
        try {
            String name = calendar.calendars().get(calendarId).execute().getSummary();
            log.debug("Nombre de calendario obtenido de Google: {} -> {}", calendarId, name);
            return name != null ? name : "Calendario sin nombre";
        } catch (IOException e) {
            log.error("Error al obtener nombre del calendario {}: {}", calendarId, e.getMessage());
            return "Calendario no disponible";
        }
    }

    /**
     * Obtiene los nombres de todos los profesionales activos.
     * Usa la BD como fuente principal.
     */
    public List<String> getCalendarsNames() {
        List<Staff> activeStaff = staffRepository.findByActiveTrueOrderByDisplayOrderAsc();
        if (!activeStaff.isEmpty()) {
            return activeStaff.stream()
                    .map(Staff::getName)
                    .toList();
        }
        // Fallback a consultar nombres desde Google Calendar
        log.warn("No hay profesionales en BD, consultando nombres desde Google Calendar");
        List<String> calendarNames = new ArrayList<>();
        for (String calendarId : calendarIds) {
            calendarNames.add(getCalendarNameById(calendarId));
        }
        return calendarNames;
    }

    /**
     * Verifica disponibilidad en tiempo real justo antes de confirmar.
     * Evita race conditions cuando el calendario se modificó externamente.
     */
    public boolean isSlotAvailable(String calendarId, LocalDateTime dateTime) {
        return isSlotAvailable(calendarId, dateTime, reservationDuration);
    }

    /**
     * Verifica disponibilidad en tiempo real justo antes de confirmar para una
     * duración específica.
     */
    public boolean isSlotAvailable(String calendarId, LocalDateTime dateTime, int durationMinutes) {
        try {
            if (isPastDateTime(dateTime)) {
                return false;
            }

            LocalDateTime slotEnd = dateTime.plusMinutes(durationMinutes);

            DateTime startDateTime = new DateTime(dateTime.atZone(ZoneId.of(timezone)).toInstant().toEpochMilli());
            DateTime endDateTime = new DateTime(slotEnd.atZone(ZoneId.of(timezone)).toInstant().toEpochMilli());

            Events events = calendar.events().list(calendarId)
                    .setTimeMin(startDateTime)
                    .setTimeMax(endDateTime)
                    .setOrderBy("startTime")
                    .setSingleEvents(true)
                    .execute();

            // Si no hay eventos en ese rango, está disponible
            boolean available = events.getItems() == null || events.getItems().isEmpty();
            log.debug("Verificación de disponibilidad para {} a las {}: {}",
                    calendarId, dateTime, available ? "DISPONIBLE" : "OCUPADO");
            return available;

        } catch (IOException e) {
            log.error("Error al verificar disponibilidad", e);
            return false; // Por seguridad, asumir no disponible
        }
    }

    /**
     * Indica si una fecha/hora está en el pasado según la zona horaria del negocio.
     */
    public boolean isPastDateTime(LocalDateTime dateTime) {
        LocalDateTime now = LocalDateTime.now(ZoneId.of(timezone));
        return !dateTime.isAfter(now);
    }
}
