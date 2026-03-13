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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
            session.setSelectedCalendarId(null);  // Limpiar calendario seleccionado
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
                        • "mis reservas" para ver tus citas
                        • "cancelar" para cancelar una reserva
                        • "salir" para reiniciar la conversación
                        """;
            } else {
                // Respuesta para otros comandos de reinicio
                response = """
                        🔄 Conversación reiniciada.

                        ¿Qué deseas hacer?
                        • Escribe "hoy", "mañana" o una fecha para hacer una reserva
                        • "mis reservas" para ver tus citas
                        • "cancelar" para cancelar una reserva
                        • "salir" para reiniciar
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

        // Comando global: ver mis reservas (funciona en cualquier estado)
        if (messageLower.equals("mis reservas") || messageLower.equals("ver reservas") || 
            messageLower.equals("reservas") || messageLower.equals("mis citas")) {
            String response = mostrarMisReservas(session);
            lastResponses.put(phoneNumber, response);
            whatsAppService.sendTextMessage(phoneNumber, response);
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
        log.info("Estado actual de sesión {}: {}", session.getPhoneNumber(), session.getState());
        return switch (session.getState()) {
            case INICIO -> handleInicio(session, message);
            case ESPERANDO_FECHA -> handleEsperandoFecha(session, message);
            case ESPERANDO_PELUQUERO -> handleEsperandoPeluquero(session, message);
            case ESPERANDO_HORARIO -> handleEsperandoHorario(session, message);
            case ESPERANDO_NOMBRE -> handleEsperandoNombre(session, message);
            case ESPERANDO_CONFIRMACION -> handleEsperandoConfirmacion(session, message);
            case MOSTRANDO_RESERVAS -> handleMostrandoReservas(session, message);
            case ESPERANDO_CONFIRMACION_CANCELACION -> handleConfirmacionCancelacion(session, message);
        };
    }
    private static final String ANY_CALENDAR = "ANY";

    private String handleInicio(UserSession session, String message) {
        String messageLower = message.toLowerCase().trim();
        
        // Si el usuario envía una fecha/palabra clave, procesarla directamente
        if (!messageLower.isEmpty() && 
            (messageLower.equals("hoy") || messageLower.equals("mañana") || 
             messageLower.contains("/") || messageLower.matches("\\d{1,2}.*"))) {
            // Cambiar estado y delegar a handleEsperandoFecha
            session.setState(UserSession.ConversationState.ESPERANDO_FECHA);
            return handleEsperandoFecha(session, message);
        }
        
        // Si pide cancelar
        if (messageLower.contains("cancelar")) {
            return manejarCancelacion(session);
        }
        
        // Por defecto, mostrar bienvenida e iniciar flujo de nueva reserva
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
                • "mis reservas" para ver tus citas
                • "cancelar" para cancelar una reserva
                • "salir" para reiniciar la conversación
                """;
    }

    private String handleEsperandoFecha(UserSession session, String message) {
        String messageLower = message.toLowerCase();

        // Si pide cancelar
        if (messageLower.contains("cancelar")) {
            return manejarCancelacion(session);
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

        // Guardar fecha y pasar a selección de peluquero
        session.setSelectedDate(fecha.atStartOfDay());
        session.setState(UserSession.ConversationState.ESPERANDO_PELUQUERO);
        sessionRepository.save(session);

        // Mostrar opciones de peluquero
        List<String> calendarNames = calendarService.getCalendarsNames();
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("📅 Fecha: %s\n\n", calendarService.formatDate(fecha)));
        sb.append("✂️ ¿Con quién te quieres cortar el pelo?\n\n");
        sb.append("0) 🔍 Cualquier peluquero disponible\n");
        for (int i = 0; i < calendarNames.size(); i++) {
            sb.append((i + 1)).append(") ").append(calendarNames.get(i)).append("\n");
        }
        sb.append("\nSelecciona un número o escribe el nombre.");
        return sb.toString();
    }

    /**
     * Maneja la selección de peluquero y muestra horarios disponibles
     */
    private String handleEsperandoPeluquero(UserSession session, String message) {
        List<String> calendarIds = calendarService.getCalendarIds();
        List<String> calendarNames = calendarService.getCalendarsNames();
        String trimmedMsg = message.trim().toLowerCase();
        LocalDate fecha = session.getSelectedDate().toLocalDate();
        
        String selectedCalendarId = null;
        boolean esAnyCalendar = false;

        // Opción "cualquiera" o "0"
        if (trimmedMsg.equals("0") || trimmedMsg.contains("cualquier") || trimmedMsg.contains("disponible")) {
            esAnyCalendar = true;
            selectedCalendarId = ANY_CALENDAR;
        } else {
            // Intentar seleccionar por número
            try {
                int idx = Integer.parseInt(trimmedMsg) - 1;
                if (idx >= 0 && idx < calendarIds.size()) {
                    selectedCalendarId = calendarIds.get(idx);
                }
            } catch (NumberFormatException ignored) {}

            // Intentar seleccionar por nombre
            if (selectedCalendarId == null) {
                for (int i = 0; i < calendarNames.size(); i++) {
                    if (trimmedMsg.equalsIgnoreCase(calendarNames.get(i)) || 
                        calendarNames.get(i).toLowerCase().contains(trimmedMsg)) {
                        selectedCalendarId = calendarIds.get(i);
                        break;
                    }
                }
            }
        }

        // Si no se encontró una opción válida
        if (selectedCalendarId == null) {
            StringBuilder sb = new StringBuilder();
            sb.append("No entendí tu selección. Por favor elige:\n\n");
            sb.append("0) 🔍 Cualquier peluquero disponible\n");
            for (int i = 0; i < calendarNames.size(); i++) {
                sb.append((i + 1)).append(") ").append(calendarNames.get(i)).append("\n");
            }
            return sb.toString();
        }

        // Guardar selección
        session.setSelectedCalendarId(selectedCalendarId);

        // Obtener y mostrar horarios disponibles
        List<String> slotsParaGuardar = new ArrayList<>();
        StringBuilder response = new StringBuilder();
        response.append(String.format("📅 Fecha: %s\n", calendarService.formatDate(fecha)));
        
        if (esAnyCalendar) {
            response.append("✂️ Peluquero: Cualquiera disponible\n\n");
            
            // Buscar en TODOS los calendarios
            Map<String, List<String>> slotsPorCalendario = new java.util.LinkedHashMap<>();
            Set<String> todosLosHorarios = new java.util.TreeSet<>();
            
            for (int i = 0; i < calendarIds.size(); i++) {
                String calId = calendarIds.get(i);
                List<String> horariosCalendario = calendarService.getAvailableSlots(calId, fecha);
                slotsPorCalendario.put(calId, horariosCalendario);
                todosLosHorarios.addAll(horariosCalendario);
            }
            
            if (todosLosHorarios.isEmpty()) {
                session.setState(UserSession.ConversationState.ESPERANDO_FECHA);
                return String.format("😕 Lo siento, no hay horarios disponibles con ningún peluquero para el %s.\n\n" +
                       "Por favor, elige otra fecha.", calendarService.formatDate(fecha));
            }
            
            response.append("⏰ Horarios disponibles:\n");
            int idx = 1;
            for (String horario : todosLosHorarios) {
                // Encontrar TODOS los peluqueros que tienen este horario
                List<String> peluquerosDisponibles = new ArrayList<>();
                List<String> calendarsDisponibles = new ArrayList<>();
                for (int i = 0; i < calendarIds.size(); i++) {
                    String calId = calendarIds.get(i);
                    if (slotsPorCalendario.get(calId).contains(horario)) {
                        peluquerosDisponibles.add(calendarNames.get(i));
                        calendarsDisponibles.add(calId);
                    }
                }
                // Guardar con formato: "HH:MM|calId1;calId2;calId3" (todos los disponibles)
                slotsParaGuardar.add(horario + "|" + String.join(";", calendarsDisponibles));
                response.append(String.format("%d. %s - %s\n", idx++, horario, String.join(", ", peluquerosDisponibles)));
            }
        } else {
            // Peluquero específico
            String nombrePeluquero = calendarService.getCalendarNameById(selectedCalendarId);
            response.append(String.format("✂️ Peluquero: %s\n\n", nombrePeluquero));
            
            List<String> horarios = calendarService.getAvailableSlots(selectedCalendarId, fecha);
            
            if (horarios.isEmpty()) {
                session.setState(UserSession.ConversationState.ESPERANDO_FECHA);
                return String.format("😕 Lo siento, %s no tiene horarios disponibles para el %s.\n\n" +
                       "Por favor, elige otra fecha o prueba con otro peluquero.", nombrePeluquero, calendarService.formatDate(fecha));
            }
            
            response.append("⏰ Horarios disponibles:\n");
            for (int i = 0; i < horarios.size(); i++) {
                slotsParaGuardar.add(horarios.get(i));
                response.append(String.format("%d. %s\n", i + 1, horarios.get(i)));
            }
        }

        // Guardar slots y cambiar estado
        session.setAvailableSlots(String.join(",", slotsParaGuardar));
        session.setState(UserSession.ConversationState.ESPERANDO_HORARIO);
        sessionRepository.save(session);

        response.append("\n¿Qué horario prefieres? (escribe el número)");
        return response.toString();
    }

    private String handleEsperandoHorario(UserSession session, String message) {
        try {
            int seleccion = Integer.parseInt(message.trim()) - 1;
            String[] slots = session.getAvailableSlots().split(",");

            if (seleccion < 0 || seleccion >= slots.length) {
                return "Por favor, selecciona un número válido de la lista.";
            }

            String slotSeleccionado = slots[seleccion];
            String horario;
            String calendarIds;
            
            // Formato: "HH:MM|calId1;calId2;calId3" (cuando es ANY) o solo "HH:MM" (peluquero específico)
            if (slotSeleccionado.contains("|")) {
                String[] partes = slotSeleccionado.split("\\|");
                horario = partes[0];
                calendarIds = partes[1]; // Puede ser "calId" o "calId1;calId2;..."
                session.setSelectedCalendarId(calendarIds);
            } else {
                horario = slotSeleccionado;
                calendarIds = session.getSelectedCalendarId();
            }
            
            session.setSelectedTime(horario);
            session.setState(UserSession.ConversationState.ESPERANDO_NOMBRE);
            sessionRepository.save(session);

            LocalDate fecha = session.getSelectedDate().toLocalDate();
            
            // Si hay múltiples calendarios, mostrar todos los peluqueros disponibles
            String textoCalendario;
            if (calendarIds.contains(";")) {
                String[] calIds = calendarIds.split(";");
                List<String> nombres = new ArrayList<>();
                for (String calId : calIds) {
                    nombres.add(calendarService.getCalendarNameById(calId));
                }
                textoCalendario = String.join(" / ", nombres);
            } else {
                textoCalendario = calendarService.getCalendarNameById(calendarIds);
            }

            return String.format("""
                    ✅ Perfecto!

                    📅 Fecha: %s
                    ⏰ Horario: %s
                    ✂️ Peluquero: %s

                    Por favor, indícame tu nombre completo para confirmar la reserva.
                    """, calendarService.formatDate(fecha), horario, textoCalendario);

        } catch (NumberFormatException e) {
            return "Por favor, escribe el número del horario que prefieres.";
        }
    }

    private String handleEsperandoNombre(UserSession session, String message) {
        session.setUserName(message.trim());
        session.setState(UserSession.ConversationState.ESPERANDO_CONFIRMACION);

        LocalDate fecha = session.getSelectedDate().toLocalDate();
        String horario = session.getSelectedTime();
        String calendarIds = session.getSelectedCalendarId();
        
        // Si hay múltiples calendarios, mostrar todos los peluqueros disponibles
        String textoCalendario;
        if (calendarIds.contains(";")) {
            String[] calIds = calendarIds.split(";");
            List<String> nombres = new ArrayList<>();
            for (String calId : calIds) {
                nombres.add(calendarService.getCalendarNameById(calId));
            }
            textoCalendario = String.join(" / ", nombres);
        } else {
            textoCalendario = calendarService.getCalendarNameById(calendarIds);
        }

        return String.format("""
                📋 *Resumen de tu reserva:*

                👤 Nombre: %s
                📅 Fecha: %s
                ⏰ Horario: %s
                ✂️ Peluquero: %s

                ¿Confirmas la reserva?
                Escribe "sí" para confirmar o "no" para cancelar.
                """, message.trim(), calendarService.formatDate(fecha), horario, textoCalendario);
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
                
                // Obtener calendarios candidatos
                // Formato puede ser: "calId" o "calId1;calId2;calId3" (cuando eligió "cualquier peluquero")
                String selectedCalendarId = session.getSelectedCalendarId();
                String[] calendarCandidates = selectedCalendarId.contains(";")
                        ? selectedCalendarId.split(";")
                        : new String[]{selectedCalendarId};
                
                // *** VALIDACIÓN EN TIEMPO REAL ***
                // Intentar con cada calendario candidato hasta encontrar uno disponible
                String calendarIdDisponible = null;
                for (String calId : calendarCandidates) {
                    if (calendarService.isSlotAvailable(calId, fechaHora)) {
                        calendarIdDisponible = calId;
                        log.info("Slot disponible encontrado con: {}", 
                                calendarService.getCalendarNameById(calId));
                        break;
                    } else {
                        log.warn("Slot no disponible con {}, intentando siguiente...", 
                                calendarService.getCalendarNameById(calId));
                    }
                }
                
                // Si ningún calendario tiene disponibilidad
                if (calendarIdDisponible == null) {
                    log.warn("Ningún peluquero disponible para {} a las {}", fecha, hora);
                    
                    // Volver a mostrar horarios disponibles
                    session.setState(UserSession.ConversationState.ESPERANDO_PELUQUERO);
                    sessionRepository.save(session);
                    
                    return String.format("""
                            😕 ¡Lo siento! El horario %s ya no está disponible con ningún peluquero.
                            
                            Parece que reservaron ese horario mientras conversábamos.
                            
                            Por favor, vuelve a seleccionar para ver los horarios actualizados:
                            
                            0) 🔍 Cualquier peluquero disponible
                            %s
                            """, session.getSelectedTime(), buildStaffOptions());
                }
                
                String nombrePeluquero = calendarService.getCalendarNameById(calendarIdDisponible);

                String eventId = calendarService.createReservation(
                    calendarIdDisponible,
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
                reservation.setCalendarId(calendarIdDisponible);
                reservation.setStatus(Reservation.ReservationStatus.CONFIRMED);
                reservationRepository.save(reservation);

                // Limpiar sesión
                sessionRepository.delete(session);

                return String.format("""
                        🎉 ¡Reserva confirmada!

                        👤 %s
                        📅 %s
                        ⏰ %s
                        ✂️ %s

                        Te esperamos! 😊

                        Escribe cualquier mensaje para hacer una nueva reserva.
                        """, session.getUserName(),
                        calendarService.formatDate(fecha),
                        session.getSelectedTime(),
                        nombrePeluquero);

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
     * Construye la lista de opciones de profesionales para mostrar
     */
    private String buildStaffOptions() {
        List<String> calendarNames = calendarService.getCalendarsNames();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < calendarNames.size(); i++) {
            sb.append((i + 1)).append(") ").append(calendarNames.get(i)).append("\n");
        }
        return sb.toString();
    }

    /**
     * Muestra horarios disponibles para los próximos días
     */
    private String mostrarHorariosDisponibles(UserSession session) {
        StringBuilder response = new StringBuilder("📅 Horarios disponibles:\n\n");
        
        boolean esAnyCalendar = ANY_CALENDAR.equals(session.getSelectedCalendarId());
        List<String> calendarIds = calendarService.getCalendarIds();
        
        for (int i = 0; i < 3; i++) {
            LocalDate fecha = LocalDate.now().plusDays(i);
            String diaTexto = switch (i) {
                case 0 -> "Hoy";
                case 1 -> "Mañana";
                default -> calendarService.formatDate(fecha);
            };
            response.append(String.format("*%s*\n", diaTexto));
            
            if (esAnyCalendar) {
                // Buscar en todos los calendarios
                Set<String> todosHorarios = new java.util.TreeSet<>();
                for (String calId : calendarIds) {
                    todosHorarios.addAll(calendarService.getAvailableSlots(calId, fecha));
                }
                if (todosHorarios.isEmpty()) {
                    response.append("  ❌ No hay horarios disponibles\n");
                } else {
                    List<String> primeros = todosHorarios.stream().limit(5).collect(Collectors.toList());
                    for (String horario : primeros) {
                        response.append(String.format("  ⏰ %s\n", horario));
                    }
                    if (todosHorarios.size() > 5) {
                        response.append(String.format("  ... y %d más\n", todosHorarios.size() - 5));
                    }
                }
            } else {
                // Peluquero específico
                List<String> horarios = calendarService.getAvailableSlots(session.getSelectedCalendarId(), fecha);
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
     * Muestra las reservas activas del usuario (solo visualización, sin cancelar)
     */
    private String mostrarMisReservas(UserSession session) {
        List<Reservation> reservas = reservationRepository.findByPhoneNumberAndStatus(
                session.getPhoneNumber(),
                Reservation.ReservationStatus.CONFIRMED
        );

        if (reservas.isEmpty()) {
            return """
                    📋 No tienes reservas activas.

                    ¿Deseas hacer una nueva reserva? Escribe "hoy", "mañana" o una fecha.
                    """;
        }

        StringBuilder response = new StringBuilder();
        response.append("📋 Tus reservas activas:\n\n");

        for (int i = 0; i < reservas.size(); i++) {
            Reservation r = reservas.get(i);
            String nombrePeluquero = r.getCalendarId() != null ? 
                calendarService.getCalendarNameById(r.getCalendarId()) : "Sin asignar";
            response.append(String.format(
                    "• %s a las %02d:00\n  ✂️ %s\n  👤 %s\n\n",
                    calendarService.formatDate(r.getReservationDateTime().toLocalDate()),
                    r.getReservationDateTime().getHour(),
                    nombrePeluquero,
                    r.getCustomerName()
            ));
        }

        response.append("Si deseas cancelar alguna, escribe \"cancelar\".");
        return response.toString();
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

        // Guardar reservas en la sesión (formato: id|fecha|hora|nombre|calendarId)
        StringBuilder reservasJson = new StringBuilder();
        for (int i = 0; i < reservas.size(); i++) {
            Reservation r = reservas.get(i);
            reservasJson.append(r.getId()).append("|")
                    .append(calendarService.formatDate(r.getReservationDateTime().toLocalDate())).append("|")
                    .append(r.getReservationDateTime().getHour()).append(":00|")
                    .append(r.getCustomerName()).append("|")
                    .append(r.getCalendarId() != null ? r.getCalendarId() : "");
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
            String nombrePeluquero = r.getCalendarId() != null ? 
                calendarService.getCalendarNameById(r.getCalendarId()) : "Sin asignar";
            response.append(String.format(
                    "%d. %s a las %02d:00 - ✂️ %s\n",
                    i + 1,
                    calendarService.formatDate(r.getReservationDateTime().toLocalDate()),
                    r.getReservationDateTime().getHour(),
                    nombrePeluquero
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
            String calendarId = reservaData.length > 4 ? reservaData[4] : null;
            String nombrePeluquero = (calendarId != null && !calendarId.isEmpty()) ? 
                calendarService.getCalendarNameById(calendarId) : "Sin asignar";

            session.setSelectedReservationId(reservaId);
            session.setSelectedCalendarId(calendarId);  // Guardar para usar al cancelar
            session.setState(UserSession.ConversationState.ESPERANDO_CONFIRMACION_CANCELACION);

            return String.format("""
                    ⚠️ Confirmar cancelación:

                    👤 %s
                    📅 %s
                    ⏰ %s
                    ✂️ %s

                    ¿Confirmas la cancelación? Escribe "sí" para confirmar o "no" para volver atrás.
                    """, nombre, fecha, hora, nombrePeluquero);

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
                
                // Usar el calendarId de la reserva o de la sesión
                String calendarIdParaCancelar = reserva.getCalendarId() != null ? 
                    reserva.getCalendarId() : session.getSelectedCalendarId();
                String nombrePeluquero = (calendarIdParaCancelar != null && !calendarIdParaCancelar.isEmpty()) ? 
                    calendarService.getCalendarNameById(calendarIdParaCancelar) : "Sin asignar";

                // Cancelar en Google Calendar
                if (calendarIdParaCancelar != null && !calendarIdParaCancelar.isEmpty()) {
                    calendarService.cancelReservation(calendarIdParaCancelar, reserva.getGoogleCalendarEventId());
                }

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
                        ✂️ %s

                        ¿Necesitas algo más? Escribe cualquier mensaje.
                        """, reserva.getCustomerName(),
                        calendarService.formatDate(reserva.getReservationDateTime().toLocalDate()),
                        String.format("%02d:00", reserva.getReservationDateTime().getHour()),
                        nombrePeluquero);

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
            // Sincroniza todos los calendarios configurados
            List<String> calendarIds = calendarService.getCalendarIds();
            
            for (String calendarId : calendarIds) {
                log.info("Sincronizando calendario: {}", calendarId);
                List<com.google.api.services.calendar.model.Event> events = calendarService.getAllEvents(calendarId);

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
                    reservation.setCalendarId(calendarId);
                    reservation.setStatus(Reservation.ReservationStatus.CONFIRMED);

                    reservationRepository.save(reservation);
                    added++;

                    log.info("Reserva sincronizada: {} - {} (Calendario: {})", customerName, phoneNumber, calendarId);

                } catch (Exception e) {
                    errors++;
                    errorLog.append("- Evento: ").append(event.getSummary()).append(" (Error: ").append(e.getMessage()).append(")\n");
                    log.error("Error sincronizando evento: {}", event.getSummary(), e);
                }
            }
            }
            log.info("Sincronización completada para todos los calendarios");

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
