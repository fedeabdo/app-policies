---
name: reminders-whatsapp
description: "Use when: implementar o ajustar recordatorios automaticos por WhatsApp, scheduler, ventanas de envio, templates, retries y trazabilidad en reminder_logs."
---

# Skill: Recordatorios Automaticos por WhatsApp

## Objetivo
Ajustar o extender el sistema de recordatorios con envios confiables, sin duplicados y con historial claro.

## Inputs Esperados
- Nuevo timing de recordatorios (ej: 24h, 2h, 30m).
- Cambios de template o parametros por idioma.
- Reportes de recordatorios no enviados o duplicados.

## Referencias del Proyecto
- src/main/java/com/reservas/whatsapp/service/ReminderService.java
- src/main/java/com/reservas/whatsapp/model/ReminderLog.java
- src/main/java/com/reservas/whatsapp/repository/ReminderLogRepository.java
- src/main/java/com/reservas/whatsapp/config/ReminderConfig.java
- docs/features/REMINDERS.md
- docs/features/WHATSAPP_TEMPLATES.md

## Flujo Recomendado
1. Revisar configuracion en application.properties y ventanas de envio.
2. Confirmar filtros: solo reservas confirmadas y dentro del rango.
3. Evaluar idempotencia antes de cada envio (si ya existe log, no reenviar).
4. Enviar template con parametros en orden correcto.
5. Persistir resultado en logs con estado y error si aplica.
6. Validar con endpoints/scripts de prueba.

## Checklist de Calidad
- No enviar duplicados para el mismo reminder.
- Manejar fallas de WhatsApp sin perder trazabilidad.
- Mantener soporte multi-template y multi-idioma.
- Cobertura de pruebas para scheduler y envio manual.

## Pitfalls Frecuentes
- Ventanas demasiado amplias que generan duplicados.
- Orden incorrecto de parametros del template.
- Ignorar diferencias entre idioma por default y por template.

## Salida Esperada
- Recordatorios consistentes y auditables.
- Configuracion clara para nuevos timings/templates.
- Pruebas y scripts de verificacion actualizados.
