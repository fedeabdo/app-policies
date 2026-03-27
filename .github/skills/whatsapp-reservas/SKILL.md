---
name: whatsapp-reservas
description: "Use when: implementar o ajustar el flujo conversacional de reservas por WhatsApp, validacion de fecha/hora, deteccion de datos faltantes, confirmacion de turno y manejo de estados de sesion."
---

# Skill: Flujo de Reservas por WhatsApp

## Objetivo
Implementar o modificar el flujo principal de reserva desde mensajes de WhatsApp, manteniendo coherencia de estado y validaciones del negocio.

## Inputs Esperados
- Cambio funcional solicitado por negocio en el flujo de reserva.
- Mensajes o ejemplos de conversacion esperados.
- Restricciones de servicio, horario o staff.

## Referencias del Proyecto
- src/main/java/com/reservas/whatsapp/service/ConversationService.java
- src/main/java/com/reservas/whatsapp/model/UserSession.java
- src/main/java/com/reservas/whatsapp/repository/UserSessionRepository.java
- src/main/java/com/reservas/whatsapp/controller/WhatsAppWebhookController.java
- docs/features/SERVICIOS.md

## Flujo Recomendado
1. Revisar estado actual de sesion y transiciones existentes.
2. Identificar el punto exacto del flujo a modificar.
3. Mantener prompts y respuestas consistentes en tono y formato.
4. Validar datos obligatorios antes de confirmar reserva.
5. Persistir estado de sesion en cada transicion critica.
6. Agregar o actualizar pruebas del flujo impactado.

## Checklist de Calidad
- No romper transiciones existentes de la maquina de estados.
- Evitar estados huerfanos o loops conversacionales.
- Mensajes de error claros cuando falte informacion.
- Confirmacion final incluye fecha, hora, servicio y staff.
- Cobertura de pruebas para happy path y casos invalidos.

## Pitfalls Frecuentes
- Confirmar reserva sin todos los datos obligatorios.
- Reutilizar estado previo de otra conversacion por no resetear sesion.
- Cambiar textos sin mantener consistencia con respuestas existentes.

## Salida Esperada
- Cambios en servicios/controladores/modelos segun corresponda.
- Pruebas actualizadas o nuevas.
- Resumen de impacto en flujo y estados.
