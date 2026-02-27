package com.reservas.whatsapp.service;

import com.reservas.whatsapp.model.Reservation;
import com.reservas.whatsapp.model.UserSession;
import com.reservas.whatsapp.repository.ReservationRepository;
import com.reservas.whatsapp.repository.UserSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConversationService {

    private final UserSessionRepository sessionRepository;
    private final ReservationRepository reservationRepository;
    private final GoogleCalendarService calendarService;
    private final WhatsAppService whatsAppService;

    // Cache de últimas respuestas para acceso desde cliente de prueba
    private final Map<String, String> lastResponses = new HashMap<>();

    /**
     * Obtiene la última respuesta enviada a un número de teléfono
     */
    public String getLastResponse(String phoneNumber) {
        return lastResponses.getOrDefault(phoneNumber, "");
    }

    /**
     * Procesa un mensaje entrante y maneja la conversación
     */
    @Transactional
    public void processMessage(String phoneNumber, String message) {
        log.info("Procesando mensaje de {}: {}", phoneNumber, message);

        // Obtener o crear sesión
        UserSession session = sessionRepository.findByPhoneNumber(phoneNumber)
                .orElseGet(() -> {
                    UserSession newSession = new UserSession();
                    newSession.setPhoneNumber(phoneNumber);
                    newSession.setState(UserSession.ConversationState.INICIO);
                    return sessionRepository.save(newSession);
                });

        String messageLower = message.trim().toLowerCase();

        // Comandos globales que funcionan en cualquier estado
        if (messageLower.equals("salir") || messageLower.equals("reiniciar") ||
            messageLower.equals("nueva reserva") || messageLower.equals("menu") ||
            messageLower.equals("hola") ) {
            // Reiniciar la conversación
            session.setState(UserSession.ConversationState.INICIO);
            session.setSelectedDate(null);
            session.setSelectedTime(null);
            session.setUserName(null);
            session.setAvailableSlots(null);
            session.setReservationsToCancel(null);
            session.setSelectedReservationId(null);
            sessionRepository.save(session);

            String response;
            if (messageLower.equals("hola")) {
                // Respuesta más natural para saludos
                response = """
                        ¡Hola! 👋 Bienvenido al sistema de reservas.

                        Para hacer una reserva, por favor indícame:
                        📅 ¿Qué día deseas reservar?

                        Puedes escribir:
                        • "hoy"
                        • "mañana"
                        • Una fecha específica (ejemplo: 15/02/2026)

                        En cualquier momento puedes:
                        • Escribe "cancelar" para cancelar una reserva existente
                        • Escribe "salir" para reiniciar la conversación
                        """;
            } else {
                // Respuesta para otros comandos de reinicio
                response = """
                        🔄 Conversación reiniciada.

                        ¿Qué deseas hacer?
                        • Escribe "hoy", "mañana" o una fecha para hacer una reserva
                        • Escribe "cancelar" para cancelar una reserva existente
                        • Escribe "salir" en cualquier momento para reiniciar
                        """;
            }

            lastResponses.put(phoneNumber, response);
            whatsAppService.sendTextMessage(phoneNumber, response);
            return;
        }

        // Comando global: cancelar una reserva (funciona en cualquier estado)
        if (messageLower.equals("cancelar") || messageLower.contains("cancel")) {
            String response = manejarCancelacion(session);
            lastResponses.put(phoneNumber, response);
            whatsAppService.sendTextMessage(phoneNumber, response);

            // Solo guardar si la sesión no fue eliminada
            if (session.getId() != null && sessionRepository.existsById(session.getId())) {
                sessionRepository.save(session);
            }
            return;
        }

        String response = handleConversationState(session, message.trim());

        // Guardar respuesta en cache para acceso desde cliente de prueba
        lastResponses.put(phoneNumber, response);

        // Enviar respuesta
        whatsAppService.sendTextMessage(phoneNumber, response);

        // Guardar sesión actualizada solo si aún existe en la base de datos
        // (puede haber sido eliminada al confirmar/cancelar una reserva)
        if (session.getId() != null && sessionRepository.existsById(session.getId())) {
            sessionRepository.save(session);
        }
    }

    /**
     * Maneja el flujo de conversación según el estado actual
     */
    private String handleConversationState(UserSession session, String message) {
        return switch (session.getState()) {
            case INICIO -> handleInicio(session, message);
            case ESPERANDO_FECHA -> handleEsperandoFecha(session, message);
            case ESPERANDO_HORARIO -> handleEsperandoHorario(session, message);
            case ESPERANDO_NOMBRE -> handleEsperandoNombre(session, message);
            case ESPERANDO_CONFIRMACION -> handleEsperandoConfirmacion(session, message);
            case MOSTRANDO_RESERVAS -> handleMostrandoReservas(session, message);
            case ESPERANDO_CONFIRMACION_CANCELACION -> handleConfirmacionCancelacion(session, message);
        };
    }

    private String handleInicio(UserSession session, String message) {
        // Por defecto, inicia el flujo de nueva reserva
        session.setState(UserSession.ConversationState.ESPERANDO_FECHA);

        return """
                ¡Hola! 👋 Bienvenido al sistema de reservas.

                Para hacer una reserva, por favor indícame:
                📅 ¿Qué día deseas reservar?

                Puedes escribir:
                • "hoy"
                • "mañana"
                • Una fecha específica (ejemplo: 15/02/2026)

                En cualquier momento puedes:
                • Escribe "cancelar" para cancelar una reserva existente
                • Escribe "salir" para reiniciar la conversación
                """;
    }

    private String handleEsperandoFecha(UserSession session, String message) {
        String messageLower = message.toLowerCase();

        // Si pide ver horarios
        if (messageLower.contains("horarios") || messageLower.contains("disponibilidad")) {
            return mostrarHorariosDisponibles();
        }

        // Intentar parsear la fecha
        LocalDate fecha = parseFecha(message);

        if (fecha == null) {
            return "No pude entender la fecha. Por favor usa:\n" +
                   "• \"hoy\" o \"mañana\"\n" +
                   "• Formato DD/MM/YYYY (ejemplo: 15/02/2026)";
        }

        // Validar que la fecha no sea pasada
        if (fecha.isBefore(LocalDate.now())) {
            return "Lo siento, no puedo crear reservas para fechas pasadas.\n" +
                   "Por favor elige otra fecha.";
        }

        // Obtener horarios disponibles
        List<String> horarios = calendarService.getAvailableSlots(fecha);

        if (horarios.isEmpty()) {
            return String.format("😕 Lo siento, no hay horarios disponibles para el %s.\n\n" +
                   "Por favor, elige otra fecha.", calendarService.formatDate(fecha));
        }

        // Guardar información en sesión
        session.setSelectedDate(fecha.atStartOfDay());
        session.setAvailableSlots(String.join(",", horarios));
        session.setState(UserSession.ConversationState.ESPERANDO_HORARIO);

        // Construir respuesta con horarios
        StringBuilder response = new StringBuilder();
        response.append(String.format("Para el día %s:\n\n", calendarService.formatDate(fecha)));
        response.append("⏰ Horarios disponibles:\n");

        for (int i = 0; i < horarios.size(); i++) {
            response.append(String.format("%d. %s\n", i + 1, horarios.get(i)));
        }

        response.append("\n¿Qué horario prefieres? (escribe el número)");

        return response.toString();
    }

    private String handleEsperandoHorario(UserSession session, String message) {
        try {
            int seleccion = Integer.parseInt(message.trim()) - 1;
            String[] horarios = session.getAvailableSlots().split(",");

            if (seleccion < 0 || seleccion >= horarios.length) {
                return "Por favor, selecciona un número válido de la lista.";
            }

            String horarioSeleccionado = horarios[seleccion];
            session.setSelectedTime(horarioSeleccionado);
            session.setState(UserSession.ConversationState.ESPERANDO_NOMBRE);

            LocalDate fecha = session.getSelectedDate().toLocalDate();

            return String.format("""
                    ✅ Perfecto!

                    📅 Fecha: %s
                    ⏰ Horario: %s

                    Por favor, indícame tu nombre completo para confirmar la reserva.
                    """, calendarService.formatDate(fecha), horarioSeleccionado);

        } catch (NumberFormatException e) {
            return "Por favor, escribe el número del horario que prefieres.";
        }
    }

    private String handleEsperandoNombre(UserSession session, String message) {
        session.setUserName(message.trim());
        session.setState(UserSession.ConversationState.ESPERANDO_CONFIRMACION);

        LocalDate fecha = session.getSelectedDate().toLocalDate();
        String horario = session.getSelectedTime();

        return String.format("""
                📋 *Resumen de tu reserva:*

                👤 Nombre: %s
                📅 Fecha: %s
                ⏰ Horario: %s

                ¿Confirmas la reserva?
                Escribe "sí" para confirmar o "no" para cancelar.
                """, message.trim(), calendarService.formatDate(fecha), horario);
    }

    private String handleEsperandoConfirmacion(UserSession session, String message) {
        String messageLower = message.toLowerCase();

        if (messageLower.contains("si") || messageLower.contains("sí") ||
            messageLower.contains("confirmar") || messageLower.equals("s")) {

            try {
                // Crear reserva
                LocalDate fecha = session.getSelectedDate().toLocalDate();
                LocalTime hora = LocalTime.parse(session.getSelectedTime());
                LocalDateTime fechaHora = LocalDateTime.of(fecha, hora);

                String eventId = calendarService.createReservation(
                        fechaHora,
                        session.getUserName(),
                        session.getPhoneNumber()
                );

                // Guardar en base de datos
                Reservation reservation = new Reservation();
                reservation.setPhoneNumber(session.getPhoneNumber());
                reservation.setCustomerName(session.getUserName());
                reservation.setReservationDateTime(fechaHora);
                reservation.setGoogleCalendarEventId(eventId);
                reservation.setStatus(Reservation.ReservationStatus.CONFIRMED);
                reservationRepository.save(reservation);

                // Limpiar sesión
                sessionRepository.delete(session);

                return String.format("""
                        🎉 ¡Reserva confirmada!

                        👤 %s
                        📅 %s
                        ⏰ %s

                        Te esperamos! 😊

                        Escribe cualquier mensaje para hacer una nueva reserva.
                        """, session.getUserName(),
                        calendarService.formatDate(fecha),
                        session.getSelectedTime());

            } catch (Exception e) {
                log.error("Error al crear reserva", e);
                return "❌ Hubo un error al crear la reserva. Por favor, intenta nuevamente más tarde.";
            }

        } else if (messageLower.contains("no") || messageLower.contains("cancelar")) {
            sessionRepository.delete(session);
            return "❌ Reserva cancelada.\n\nEscribe cualquier mensaje para hacer una nueva reserva.";
        } else {
            return "Por favor, responde 'sí' para confirmar o 'no' para cancelar.";
        }
    }

    /**
     * Muestra horarios disponibles para los próximos días
     */
    private String mostrarHorariosDisponibles() {
        StringBuilder response = new StringBuilder("📅 Horarios disponibles:\n\n");

        for (int i = 0; i < 3; i++) {
            LocalDate fecha = LocalDate.now().plusDays(i);
            List<String> horarios = calendarService.getAvailableSlots(fecha);

            String diaTexto = switch (i) {
                case 0 -> "Hoy";
                case 1 -> "Mañana";
                default -> calendarService.formatDate(fecha);
            };

            response.append(String.format("*%s*\n", diaTexto));

            if (horarios.isEmpty()) {
                response.append("  ❌ No hay horarios disponibles\n");
            } else {
                List<String> primeros = horarios.stream().limit(5).collect(Collectors.toList());
                for (String horario : primeros) {
                    response.append(String.format("  ⏰ %s\n", horario));
                }
                if (horarios.size() > 5) {
                    response.append(String.format("  ... y %d más\n", horarios.size() - 5));
                }
            }
            response.append("\n");
        }

        response.append("Por favor, indícame la fecha que prefieres.");
        return response.toString();
    }

    /**
     * Parsea una fecha desde texto
     */
    private LocalDate parseFecha(String texto) {
        String textoLower = texto.toLowerCase().trim();

        if (textoLower.equals("hoy")) {
            return LocalDate.now();
        } else if (textoLower.equals("mañana") || textoLower.equals("manana")) {
            return LocalDate.now().plusDays(1);
        }

        // Intentar parsear formatos de fecha
        String[] formatos = {"dd/MM/yyyy", "dd-MM-yyyy", "dd/MM/yy"};

        for (String formato : formatos) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(formato);
                return LocalDate.parse(texto, formatter);
            } catch (DateTimeParseException e) {
                // Intentar siguiente formato
            }
        }

        return null;
    }

    /**
     * Maneja el flujo de cancelación de reservas
     */
    private String manejarCancelacion(UserSession session) {
        List<Reservation> reservas = reservationRepository.findByPhoneNumberAndStatus(
                session.getPhoneNumber(),
                Reservation.ReservationStatus.CONFIRMED
        );

        if (reservas.isEmpty()) {
            return """
                    ❌ No tienes reservas activas para cancelar.

                    ¿Deseas hacer una nueva reserva? Escribe cualquier mensaje para comenzar.
                    """;
        }

        // Guardar reservas en la sesión
        StringBuilder reservasJson = new StringBuilder();
        for (int i = 0; i < reservas.size(); i++) {
            Reservation r = reservas.get(i);
            reservasJson.append(r.getId()).append("|")
                    .append(calendarService.formatDate(r.getReservationDateTime().toLocalDate())).append("|")
                    .append(r.getReservationDateTime().getHour()).append(":00|")
                    .append(r.getCustomerName());
            if (i < reservas.size() - 1) {
                reservasJson.append("||");
            }
        }
        session.setReservationsToCancel(reservasJson.toString());
        session.setState(UserSession.ConversationState.MOSTRANDO_RESERVAS);

        // Mostrar reservas
        StringBuilder response = new StringBuilder();
        response.append("📋 Tus reservas activas:\n\n");

        for (int i = 0; i < reservas.size(); i++) {
            Reservation r = reservas.get(i);
            response.append(String.format(
                    "%d. %s - %s a las %02d:00\n",
                    i + 1,
                    calendarService.formatDate(r.getReservationDateTime().toLocalDate()),
                    r.getCustomerName(),
                    r.getReservationDateTime().getHour()
            ));
        }

        response.append("\n¿Cuál deseas cancelar? (escribe el número)");
        return response.toString();
    }

    /**
     * Maneja la selección de reserva a cancelar
     */
    private String handleMostrandoReservas(UserSession session, String message) {
        try {
            int seleccion = Integer.parseInt(message.trim()) - 1;
            String[] reservasArray = session.getReservationsToCancel().split("\\|\\|");

            if (seleccion < 0 || seleccion >= reservasArray.length) {
                return "Por favor, selecciona un número válido de la lista.";
            }

            String[] reservaData = reservasArray[seleccion].split("\\|");
            long reservaId = Long.parseLong(reservaData[0]);
            String fecha = reservaData[1];
            String hora = reservaData[2];
            String nombre = reservaData[3];

            session.setSelectedReservationId(reservaId);
            session.setState(UserSession.ConversationState.ESPERANDO_CONFIRMACION_CANCELACION);

            return String.format("""
                    ⚠️ Confirmar cancelación:

                    👤 %s
                    📅 %s
                    ⏰ %s

                    ¿Confirmas la cancelación? Escribe "sí" para confirmar o "no" para volver atrás.
                    """, nombre, fecha, hora);

        } catch (NumberFormatException e) {
            return "Por favor, escribe el número de la reserva que deseas cancelar.";
        }
    }

    /**
     * Maneja la confirmación de cancelación
     */
    private String handleConfirmacionCancelacion(UserSession session, String message) {
        String messageLower = message.toLowerCase();

        if (messageLower.contains("si") || messageLower.contains("sí") ||
            messageLower.contains("confirmar") || messageLower.equals("s")) {

            try {
                Long reservaId = session.getSelectedReservationId();
                Optional<Reservation> reservaOpt = reservationRepository.findById(reservaId);

                if (reservaOpt.isEmpty()) {
                    return "❌ No se encontró la reserva.";
                }

                Reservation reserva = reservaOpt.get();

                // Cancelar en Google Calendar
                calendarService.cancelReservation(reserva.getGoogleCalendarEventId());

                // Marcar como cancelada en BD
                reserva.setStatus(Reservation.ReservationStatus.CANCELLED);
                reserva.setCancelledAt(LocalDateTime.now());
                reservationRepository.save(reserva);

                // Limpiar sesión
                sessionRepository.delete(session);

                return String.format("""
                        ✅ Reserva cancelada exitosamente.

                        👤 %s
                        📅 %s
                        ⏰ %s

                        ¿Necesitas algo más? Escribe cualquier mensaje.
                        """, reserva.getCustomerName(),
                        calendarService.formatDate(reserva.getReservationDateTime().toLocalDate()),
                        String.format("%02d:00", reserva.getReservationDateTime().getHour()));

            } catch (Exception e) {
                log.error("Error al cancelar reserva", e);
                return "❌ Hubo un error al cancelar la reserva. Por favor, intenta nuevamente.";
            }

        } else if (messageLower.contains("no")) {
            // Volver al menú de cancelación
            return manejarCancelacion(session);
        } else {
            return "Por favor, responde 'sí' para confirmar o 'no' para volver atrás.";
        }
    }

    /**
     * Sincroniza todos los eventos de Google Calendar hacia la tabla de reservas
     * Solo agrega eventos que no existan aún en la BD
     */
    @Transactional
    public SyncResult syncReservationsFromCalendar() {
        log.info("Iniciando sincronización de reservas desde Google Calendar");

        int added = 0;
        int skipped = 0;
        int errors = 0;
        StringBuilder errorLog = new StringBuilder();

        try {
            List<com.google.api.services.calendar.model.Event> events = calendarService.getAllEvents();

            for (com.google.api.services.calendar.model.Event event : events) {
                try {
                    // Solo procesar eventos que tengan el patrón "Reserva - {nombre}"
                    String summary = event.getSummary();
                    if (summary == null || !summary.startsWith("Reserva - ")) {
                        continue;
                    }

                    // Verificar si ya existe
                    String eventId = event.getId();
                    if (reservationRepository.findByGoogleCalendarEventId(eventId).isPresent()) {
                        skipped++;
                        continue;
                    }

                    // Extraer nombre del cliente del summary: "Reserva - {nombre}"
                    String customerName = summary.substring("Reserva - ".length()).trim();

                    // Extraer teléfono de la descripción
                    String description = event.getDescription();
                    String phoneNumber = extractPhoneFromDescription(description);

                    if (phoneNumber == null || phoneNumber.isEmpty()) {
                        errors++;
                        errorLog.append("- Evento: ").append(summary).append(" (Sin teléfono)\n");
                        continue;
                    }

                    // Extraer fecha/hora
                    LocalDateTime reservationDateTime = extractDateTimeFromEvent(event);
                    if (reservationDateTime == null) {
                        errors++;
                        errorLog.append("- Evento: ").append(summary).append(" (Sin fecha)\n");
                        continue;
                    }

                    // Crear la reserva
                    Reservation reservation = new Reservation();
                    reservation.setPhoneNumber(phoneNumber);
                    reservation.setCustomerName(customerName);
                    reservation.setReservationDateTime(reservationDateTime);
                    reservation.setGoogleCalendarEventId(eventId);
                    reservation.setStatus(Reservation.ReservationStatus.CONFIRMED);

                    reservationRepository.save(reservation);
                    added++;

                    log.info("Reserva sincronizada: {} - {}", customerName, phoneNumber);

                } catch (Exception e) {
                    errors++;
                    errorLog.append("- Evento: ").append(event.getSummary()).append(" (Error: ").append(e.getMessage()).append(")\n");
                    log.error("Error sincronizando evento: {}", event.getSummary(), e);
                }
            }

        } catch (Exception e) {
            log.error("Error obtiendo eventos del calendario", e);
            return new SyncResult(added, skipped, errors + 1, "Error obtiendo eventos: " + e.getMessage());
        }

        String resultMessage = String.format("✅ Sincronización completada:\n- %d nuevas reservas importadas\n- %d reservas ya existentes\n- %d errores",
                added, skipped, errors);

        if (errors > 0) {
            resultMessage += "\n\nErrores encontrados:\n" + errorLog.toString();
        }

        return new SyncResult(added, skipped, errors, resultMessage);
    }

    /**
     * Extrae el teléfono de la descripción del evento
     * Formato esperado: "Cliente: {nombre}\nTeléfono: {teléfono}"
     */
    private String extractPhoneFromDescription(String description) {
        if (description == null || description.isEmpty()) {
            return null;
        }

        String[] lines = description.split("\n");
        for (String line : lines) {
            if (line.startsWith("Teléfono:") || line.startsWith("Telefono:")) {
                return line.substring(line.indexOf(":") + 1).trim();
            }
        }

        return null;
    }

    /**
     * Extrae la fecha y hora del evento de Google Calendar
     */
    private LocalDateTime extractDateTimeFromEvent(com.google.api.services.calendar.model.Event event) {
        try {
            com.google.api.services.calendar.model.EventDateTime startDateTime = event.getStart();
            if (startDateTime == null || startDateTime.getDateTime() == null) {
                return null;
            }

            com.google.api.client.util.DateTime googleDateTime = startDateTime.getDateTime();
            return LocalDateTime.ofInstant(
                    java.time.Instant.ofEpochMilli(googleDateTime.getValue()),
                    java.time.ZoneId.of("America/Montevideo")
            );
        } catch (Exception e) {
            log.error("Error extrayendo fecha del evento", e);
            return null;
        }
    }

    /**
     * Resultado de la sincronización
     */
    public static class SyncResult {
        public final int added;
        public final int skipped;
        public final int errors;
        public final String message;

        public SyncResult(int added, int skipped, int errors, String message) {
            this.added = added;
            this.skipped = skipped;
            this.errors = errors;
            this.message = message;
        }
    }
}
