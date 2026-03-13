# Sistema de Recordatorios

Sistema automático para enviar recordatorios de reservas por WhatsApp usando templates de Meta.

## Índice

- [Configuración](#configuración)
- [Cómo Funciona](#cómo-funciona)
- [Endpoints de Prueba](#endpoints-de-prueba)
- [Crear Template en Meta](#crear-template-en-meta)
- [Troubleshooting](#troubleshooting)

---

## Configuración

En `application.properties`:

```properties
# Habilitar/deshabilitar sistema
reminder.enabled=true

# Template de WhatsApp (debe estar aprobado por Meta)
reminder.template-name=reserva_recordatorio
reminder.template-language=es

# Cada cuántos minutos revisar reservas pendientes
reminder.check-interval-minutes=5

# Recordatorios configurables
# Recordatorio 1: 24 horas antes
reminder.times[0].minutes-before=1440
reminder.times[0].enabled=true
reminder.times[0].description=Recordatorio 24 horas antes

# Recordatorio 2: 2 horas antes
reminder.times[1].minutes-before=120
reminder.times[1].enabled=true
reminder.times[1].description=Recordatorio 2 horas antes

# Recordatorio 3: 30 minutos antes (deshabilitado)
reminder.times[2].minutes-before=30
reminder.times[2].enabled=false
reminder.times[2].description=Recordatorio 30 minutos antes
```

### Agregar más recordatorios

```properties
reminder.times[3].minutes-before=60
reminder.times[3].enabled=true
reminder.times[3].description=Recordatorio 1 hora antes

# También puedes usar un template diferente para este recordatorio
reminder.times[3].template-name=recordatorio_urgente
```

### Configurar parámetros e idioma por template

Cada template puede tener diferentes variables e idioma. Configura qué datos van en cada posición y el idioma:

```properties
# Template: recordatorio_reserva
# Mensaje: "Hola {{1}}, recordatorio de cita con {{2}} el {{3}} a las {{4}}"
reminder.templates.recordatorio_reserva.params=customer_name,staff_name,date,time
reminder.templates.recordatorio_reserva.language=es_UY

# Template: reserva_recordatorio  
# Mensaje: "Hola {{1}}, reserva: Fecha {{2}}, Hora {{3}}, Con {{4}}, Falta {{5}}"
reminder.templates.reserva_recordatorio.params=customer_name,date,time,staff_name,time_remaining
reminder.templates.reserva_recordatorio.language=es_UY
```

**Campos disponibles:**

| Campo | Descripción | Ejemplo |
|-------|-------------|---------|
| `customer_name` | Nombre del cliente | María García |
| `staff_name` | Nombre del profesional | Juan |
| `date` | Fecha formateada | viernes 20 de marzo |
| `time` | Hora | 15:00 |
| `time_remaining` | Tiempo restante | 24 horas |

**Idiomas comunes:**

| Código | Idioma |
|--------|--------|
| `es` | Español |
| `es_AR` | Español (Argentina) |
| `es_UY` | Español (Uruguay) |
| `es_MX` | Español (México) |
| `en` | Inglés |
| `en_US` | Inglés (EEUU) |
| `pt_BR` | Portugués (Brasil) |

> Si no configuras idioma para un template, se usa `reminder.template-language` por defecto.

---

## Cómo Funciona

### Flujo del Scheduler

```
Cada X minutos (check-interval-minutes)
         │
         ▼
┌────────────────────────────────┐
│ ¿reminder.enabled = true?      │──No──► Sale
└────────────────────────────────┘
         │ Sí
         ▼
┌────────────────────────────────┐
│ Busca reservas CONFIRMADAS     │
│ en las próximas 24+ horas      │
└────────────────────────────────┘
         │
         ▼
┌────────────────────────────────────────────┐
│ Por cada reserva, revisa cada recordatorio │
│ configurado (1440min, 120min, etc.)        │
└────────────────────────────────────────────┘
         │
         ▼
┌──────────────────────────────────────────┐
│ ¿Es hora de enviar este recordatorio?    │
│                                          │
│ sendAt = horaReserva - minutesBefore     │
│ ventana = sendAt ± checkInterval         │
│                                          │
│ Si "ahora" está en la ventana:           │
│   - Verifica si ya se envió (evita dup)  │
│   - Envía el template por WhatsApp       │
│   - Guarda log en reminder_logs          │
└──────────────────────────────────────────┘
```

### Ejemplo

**Reserva:** 15 de marzo a las 14:00  
**Check interval:** 5 minutos

| Recordatorio | Se envía | Ventana |
|--------------|----------|---------|
| 24h antes (1440 min) | 14 marzo 14:00 | 13:55 - 14:05 |
| 2h antes (120 min) | 15 marzo 12:00 | 11:55 - 12:05 |

---

## Endpoints de Prueba

> ⚠️ Solo disponibles en desarrollo (perfil `dev` o `default`)

### Ver configuración
```bash
curl http://localhost:8080/api/test/reminders/config | jq
```

### Listar reservas confirmadas
```bash
curl http://localhost:8080/api/test/reminders/reservations | jq
```

### Enviar recordatorio manual
```bash
curl -X POST http://localhost:8080/api/test/reminders/send/1 | jq
```

### Forzar ejecución del scheduler
```bash
curl -X POST http://localhost:8080/api/test/reminders/process | jq
```

### Ver historial de una reserva
```bash
curl http://localhost:8080/api/test/reminders/history/1 | jq
```

### Script de prueba

```bash
# Ver ayuda
bash scripts/_test/test-reminders.sh

# Comandos
bash scripts/_test/test-reminders.sh config
bash scripts/_test/test-reminders.sh reservations
bash scripts/_test/test-reminders.sh send 1
bash scripts/_test/test-reminders.sh history 1
bash scripts/_test/test-reminders.sh process
```

---

## Crear Template en Meta

### Pasos

1. Ve a [Meta Business Suite](https://business.facebook.com) → WhatsApp Manager
2. Herramientas de cuenta → Plantillas de mensajes
3. Crear plantilla:
   - **Nombre:** `reserva_recordatorio`
   - **Categoría:** Utilidad
   - **Idioma:** Español

### Contenido del Template

```
Hola {{1}} 👋

Te recordamos tu reserva:

📅 Fecha: {{2}}
⏰ Hora: {{3}}
💇 Con: {{4}}
⏳ Falta: {{5}}

¡Te esperamos!
```

### Variables

| Variable | Descripción | Ejemplo |
|----------|-------------|---------|
| `{{1}}` | Nombre del cliente | María García |
| `{{2}}` | Fecha | viernes 20 de marzo |
| `{{3}}` | Hora | 15:00 |
| `{{4}}` | Nombre del profesional | Juan |
| `{{5}}` | Tiempo restante | 24 horas |

> 📖 Ver [WHATSAPP_TEMPLATES.md](WHATSAPP_TEMPLATES.md) para guía completa de templates.

---

## Troubleshooting

### El recordatorio no se envía

1. **Verificar que está habilitado:**
   ```bash
   curl http://localhost:8080/api/test/reminders/config | jq '.enabled'
   ```

2. **Verificar que hay reservas confirmadas:**
   ```bash
   curl http://localhost:8080/api/test/reminders/reservations | jq
   ```

3. **Revisar logs:**
   ```bash
   grep -i "recordatorio" logs/app.log
   ```

### Error: Template not found

- Verificar que el template está **aprobado** en Meta
- Verificar nombre exacto en `reminder.template-name`
- Verificar idioma en `reminder.template-language`

### Error: Parameter count mismatch

El número de parámetros no coincide con las variables del template. El bot envía 5:
1. Nombre cliente
2. Fecha
3. Hora
4. Profesional
5. Tiempo restante

### Recordatorio duplicado

No debería pasar. El sistema verifica en `reminder_logs` antes de enviar. Si ocurre, revisar:
```sql
SELECT * FROM reminder_logs WHERE reservation_id = X;
```

### Ver tabla de logs

```sql
SELECT 
    rl.id,
    r.customer_name,
    rl.reminder_number,
    rl.minutes_before,
    rl.status,
    rl.sent_at
FROM reminder_logs rl
JOIN reservations r ON rl.reservation_id = r.id
ORDER BY rl.sent_at DESC;
```

---

## Archivos Relacionados

| Archivo | Descripción |
|---------|-------------|
| [ReminderService.java](../src/main/java/com/reservas/whatsapp/service/ReminderService.java) | Lógica principal |
| [ReminderConfig.java](../src/main/java/com/reservas/whatsapp/config/ReminderConfig.java) | Configuración |
| [ReminderLog.java](../src/main/java/com/reservas/whatsapp/model/ReminderLog.java) | Modelo de logs |
| [ReminderTestController.java](../src/main/java/com/reservas/whatsapp/controller/ReminderTestController.java) | Endpoints de test |
| [WHATSAPP_TEMPLATES.md](WHATSAPP_TEMPLATES.md) | Guía de templates |

---

*Última actualización: Marzo 2026*
