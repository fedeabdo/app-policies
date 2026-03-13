package com.reservas.whatsapp.repository;

import com.reservas.whatsapp.model.ReminderLog;
import com.reservas.whatsapp.model.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReminderLogRepository extends JpaRepository<ReminderLog, Long> {

    /**
     * Busca todos los recordatorios enviados para una reserva
     */
    List<ReminderLog> findByReservation(Reservation reservation);
    
    /**
     * Busca todos los recordatorios enviados para una reserva por ID
     */
    List<ReminderLog> findByReservationId(Long reservationId);
    
    /**
     * Verifica si ya se envió un recordatorio específico para una reserva
     */
    Optional<ReminderLog> findByReservationAndReminderNumber(Reservation reservation, Integer reminderNumber);
    
    /**
     * Cuenta cuántos recordatorios exitosos se han enviado para una reserva
     */
    @Query("SELECT COUNT(r) FROM ReminderLog r WHERE r.reservation = :reservation AND r.status = 'SENT'")
    Long countSentReminders(@Param("reservation") Reservation reservation);
    
    /**
     * Verifica si ya existe un recordatorio para una reserva con ciertos minutos de anticipación
     */
    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END FROM ReminderLog r WHERE r.reservation = :reservation AND r.minutesBefore = :minutesBefore")
    boolean existsByReservationAndMinutesBefore(@Param("reservation") Reservation reservation, @Param("minutesBefore") Integer minutesBefore);
}
