package com.reservas.whatsapp.service;

import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.EventReminder;
import com.google.api.services.calendar.model.Events;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class GoogleCalendarService {
    
    private final Calendar calendar;
    
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
            List<String> allSlots = generateAllSlots();
            
            // Filtrar slots ocupados
            List<String> availableSlots = new ArrayList<>(allSlots);
            
            for (Event event : events.getItems()) {
                LocalDateTime eventStart = convertToLocalDateTime(event.getStart().getDateTime());
                LocalDateTime eventEnd = convertToLocalDateTime(event.getEnd().getDateTime());
                
                availableSlots.removeIf(slot -> {
                    LocalTime slotTime = LocalTime.parse(slot);
                    LocalDateTime slotStart = date.atTime(slotTime);
                    LocalDateTime slotEnd = slotStart.plusMinutes(reservationDuration);
                    
                    // Verificar solapamiento
                    return !(slotEnd.isBefore(eventStart) || slotStart.isAfter(eventEnd) || slotStart.isEqual(eventEnd));
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
    public String createReservation(String calendarId, LocalDateTime dateTime, String customerName, String phoneNumber) {
        try {
            Event event = new Event()
                    .setSummary("Reserva - " + customerName)
                    .setDescription("Cliente: " + customerName + "\nTeléfono: " + phoneNumber);
            
            LocalDateTime endDateTime = dateTime.plusMinutes(reservationDuration);
            
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
                            new EventReminder().setMethod("popup").setMinutes(24 * 60)
                    ));
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
    private List<String> generateAllSlots() {
        List<String> slots = new ArrayList<>();
        for (int hour = businessHoursStart; hour < businessHoursEnd; hour++) {
            slots.add(String.format("%02d:00", hour));
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
                ZoneId.of(timezone)
        );
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


    public List<String> getCalendarIds(){
        return calendarIds;
    }

    public String getCalendarNameById(String calendarId) {
        try {
            String name = calendar.calendars().get(calendarId).execute().getSummary();
            log.debug("Nombre de calendario obtenido: {} -> {}", calendarId, name);
            return name != null ? name : "Calendario sin nombre";
        } catch (IOException e) {
            log.error("Error al obtener nombre del calendario {}: {}", calendarId, e.getMessage());
            return "Calendario no disponible";
        }
    }


    public List<String> getCalendarsNames() {
        List<String> calendarNames = new ArrayList<>();
        for (String calendarId : calendarIds) {
            calendarNames.add(getCalendarNameById(calendarId));
        }
        return calendarNames;
    }
}
