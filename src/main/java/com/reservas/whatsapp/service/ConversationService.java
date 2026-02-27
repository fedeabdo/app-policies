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
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConversationService {
    
    private final UserSessionRepository sessionRepository;
    private final ReservationRepository reservationRepository;
    private final GoogleCalendarService calendarService;
    private final WhatsAppService whatsAppService;
    
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
        
        String response = handleConversationState(session, message.trim());
        
        // Enviar respuesta
        whatsAppService.sendTextMessage(phoneNumber, response);
        
        // Guardar sesión actualizada
        sessionRepository.save(session);
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
        };
    }
    
    private String handleInicio(UserSession session, String message) {
        session.setState(UserSession.ConversationState.ESPERANDO_FECHA);
        
        return """
                ¡Hola! 👋 Bienvenido al sistema de reservas.
                
                Para hacer una reserva, por favor indícame:
                📅 ¿Qué día deseas reservar?
                
                Puedes escribir:
                • "hoy"
                • "mañana"
                • Una fecha específica (ejemplo: 15/02/2026)
                
                También puedes escribir "horarios" para ver disponibilidad.
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
}
