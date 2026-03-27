---
name: testing-e2e-scripts
description: "Use when: validar flujos end-to-end del bot con scripts, reproducir regresiones, probar webhook/reminders rapidamente y documentar resultados de prueba."
---

# Skill: Testing E2E con Scripts

## Objetivo
Ejecutar pruebas funcionales rapidas y repetibles usando los scripts del proyecto para detectar regresiones temprano.

## Inputs Esperados
- Flujo a validar (reserva, recordatorio, webhook).
- Entorno levantado con DB y app corriendo.
- Casos positivos y negativos a cubrir.

## Referencias del Proyecto
- scripts/_test/test-webhook.sh
- scripts/_test/test-reminders.sh
- scripts/webhook-chat.sh
- README.md

## Flujo Recomendado
1. Verificar prerequisitos: app activa, DB activa, variables de entorno.
2. Ejecutar script correspondiente al flujo objetivo.
3. Capturar salida y comparar con comportamiento esperado.
4. Si falla, aislar si es datos, config o codigo.
5. Documentar comando usado y resultado para reproducibilidad.

## Checklist de Calidad
- Al menos un caso happy path y uno de error por flujo.
- Pruebas repetibles sin pasos manuales ocultos.
- Resultados claros para compartir en PR/incidente.

## Pitfalls Frecuentes
- Ejecutar tests con entorno incompleto.
- No limpiar estado previo y obtener falsos positivos.
- Concluir sobre una sola corrida sin reintento controlado.

## Salida Esperada
- Evidencia de pruebas ejecutadas.
- Diagnostico rapido de regresiones.
- Lista de comandos validos para QA/desarrollo.
