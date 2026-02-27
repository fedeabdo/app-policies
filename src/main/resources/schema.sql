-- Crear base de datos
CREATE DATABASE whatsapp_reservas;

-- Conectar a la base de datos
\c whatsapp_reservas;

-- Las tablas se crearán automáticamente con JPA/Hibernate
-- Este script es opcional, solo para referencia

-- Crear extensión para UUID si la necesitas
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Índices adicionales para mejorar performance (opcional)
-- Se crean después de que JPA genere las tablas

-- CREATE INDEX idx_user_sessions_phone ON user_sessions(phone_number);
-- CREATE INDEX idx_reservations_phone ON reservations(phone_number);
-- CREATE INDEX idx_reservations_datetime ON reservations(reservation_date_time);
-- CREATE INDEX idx_reservations_status ON reservations(status);
