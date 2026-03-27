---
name: webhook-operability
description: "Use when: depurar o mejorar el webhook de WhatsApp, validacion de payload, idempotencia, manejo de errores, logging y guias de troubleshooting en produccion."
---

# Skill: Webhook y Operabilidad

## Objetivo
Fortalecer el endpoint webhook para que procese mensajes de forma confiable y observable en entornos reales.

## Inputs Esperados
- Errores reportados en webhook.
- Casos de mensajes no procesados o procesados dos veces.
- Necesidad de mejorar logs para soporte.

## Referencias del Proyecto
- src/main/java/com/reservas/whatsapp/controller/WhatsAppWebhookController.java
- src/main/java/com/reservas/whatsapp/service/WhatsAppService.java
- docs/TROUBLESHOOTING.md
- scripts/webhook-chat.sh

## Flujo Recomendado
1. Validar estructura de payload antes de usar campos internos.
2. Implementar guardas contra null y ramas inesperadas.
3. Aplicar idempotencia para eventos repetidos.
4. Registrar logs con contexto minimo: id evento, telefono, accion.
5. Exponer respuestas HTTP utiles para diagnostico.

## Checklist de Calidad
- Webhook no cae ante payload parcial o invalido.
- Eventos repetidos no duplican efectos.
- Logs accionables para rastrear punta a punta.
- Casos de prueba para payload realista y edge cases.

## Pitfalls Frecuentes
- Asumir campos siempre presentes en webhook.
- Mezclar errores de negocio con errores de integracion.
- Logging ruidoso sin identificadores clave.

## Salida Esperada
- Webhook mas robusto y facil de depurar.
- Mejores mensajes de error y telemetria.
- Documentacion breve de troubleshooting actualizada.
