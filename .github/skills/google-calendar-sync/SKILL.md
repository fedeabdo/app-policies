---
name: google-calendar-sync
description: "Use when: crear o corregir sincronizacion de reservas con Google Calendar, importacion de eventos, parseo de metadata, deduplicacion y consistencia entre calendario y base de datos."
---

# Skill: Sincronizacion con Google Calendar

## Objetivo
Mantener sincronizadas las reservas entre Google Calendar y la base local sin duplicados ni perdida de datos.

## Inputs Esperados
- Cambios en reglas de sincronizacion.
- Cambios en formato de eventos o descripcion.
- Errores de importacion/actualizacion desde calendario.

## Referencias del Proyecto
- src/main/java/com/reservas/whatsapp/service/GoogleCalendarService.java
- src/main/java/com/reservas/whatsapp/config/GoogleCalendarConfig.java
- src/main/java/com/reservas/whatsapp/model/Reservation.java
- src/main/java/com/reservas/whatsapp/repository/ReservationRepository.java
- docs/features/SYNC_RESERVAS.md

## Flujo Recomendado
1. Revisar contrato actual de evento (titulo, descripcion, telefono, servicio).
2. Confirmar estrategia de deduplicacion por llave de negocio.
3. Aplicar parseo robusto con manejo de errores por evento.
4. Registrar contadores de importados, existentes y errores.
5. Validar persistencia y timezone antes de cerrar el cambio.

## Checklist de Calidad
- No duplicar reservas ya existentes.
- Respuesta de sync con metricas utiles.
- Errores parciales no deben frenar toda la corrida.
- Timezone consistente entre calendario y DB.

## Pitfalls Frecuentes
- Parseo fragil por depender de texto exacto.
- Ignorar zonas horarias y desplazar horarios.
- Tratar todos los errores como fatales.

## Salida Esperada
- Sincronizacion estable y observable.
- Logs/metricas para troubleshooting.
- Pruebas para eventos validos e invalidos.
