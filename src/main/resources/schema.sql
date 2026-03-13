-- Las tablas se crearán automáticamente con JPA/Hibernate
-- Este script es opcional, solo para referencia

-- Crear extensión para UUID si la necesitas
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- =====================================================
-- DATOS INICIALES: PROFESIONALES (STAFF)
-- =====================================================
-- Ejecutar este INSERT después de iniciar la aplicación por primera vez
-- (para que JPA cree la tabla staff primero)
--
-- Reemplaza los valores con los nombres reales de tus profesionales
-- y sus IDs de calendario de Google.

-- INSERT INTO staff (name, email, google_calendar_id, active, display_order, created_at, updated_at)
-- VALUES 
--     ('Peluquero 1', 'peluquero1@email.com', '04fdc684c5c7da4b721f2c78bc856e4b28237df4504243b26aca933fcc259eae@group.calendar.google.com', true, 1, NOW(), NOW()),
--     ('Peluquero 2', 'peluquero2@email.com', '5ad59402d7c1576939adc86e0980ede4207a0d3f7fef428ff67f9d5391dd3345@group.calendar.google.com', true, 2, NOW(), NOW()),
--     ('Peluquero 3', 'peluquero3@email.com', '3eba9bca4aac2824fd705465e1d8beb1c2452f6b0f160495ee66ed9eb14680a9@group.calendar.google.com', true, 3, NOW(), NOW());

-- =====================================================
-- ÍNDICES ADICIONALES PARA PERFORMANCE (opcional)
-- =====================================================
-- Se crean después de que JPA genere las tablas

-- CREATE INDEX idx_user_sessions_phone ON user_sessions(phone_number);
-- CREATE INDEX idx_reservations_phone ON reservations(phone_number);
-- CREATE INDEX idx_reservations_datetime ON reservations(reservation_date_time);
-- CREATE INDEX idx_reservations_status ON reservations(status);
-- CREATE INDEX idx_staff_active ON staff(active);
-- CREATE INDEX idx_staff_calendar_id ON staff(google_calendar_id);
