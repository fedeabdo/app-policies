package com.reservas.whatsapp.repository;

import com.reservas.whatsapp.model.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    List<Reservation> findByPhoneNumber(String phoneNumber);

    List<Reservation> findByPhoneNumberAndStatus(String phoneNumber, Reservation.ReservationStatus status);

    List<Reservation> findByReservationDateTimeBetween(LocalDateTime start, LocalDateTime end);

    Optional<Reservation> findByGoogleCalendarEventId(String eventId);

    List<Reservation> findByStatus(Reservation.ReservationStatus status);
}
