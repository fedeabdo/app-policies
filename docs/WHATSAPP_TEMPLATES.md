# Guía de Templates de WhatsApp Business API

Esta guía explica cómo crear, modificar y gestionar templates de mensajes para WhatsApp Business API de Meta.

## Índice

1. [¿Qué son los Templates?](#qué-son-los-templates)
2. [Acceder al Business Manager](#acceder-al-business-manager)
3. [Crear un Template](#crear-un-template)
4. [Tipos de Templates](#tipos-de-templates)
5. [Variables y Parámetros](#variables-y-parámetros)
6. [Template de Recordatorio (Ejemplo)](#template-de-recordatorio-ejemplo)
7. [Proceso de Aprobación](#proceso-de-aprobación)
8. [Modificar un Template](#modificar-un-template)
9. [Errores Comunes y Soluciones](#errores-comunes-y-soluciones)
10. [Mejores Prácticas](#mejores-prácticas)

---

## ¿Qué son los Templates?

Los **Message Templates** son mensajes pre-aprobados que puedes enviar a usuarios a través de WhatsApp Business API. Son obligatorios para:

- **Iniciar conversaciones** con usuarios (mensajes proactivos)
- Enviar **notificaciones** fuera de la ventana de 24 horas
- Enviar **recordatorios de citas**
- **Confirmaciones de pedidos**
- **Actualizaciones de envío**

> ⚠️ **Importante**: Solo puedes responder con mensajes regulares (sin template) dentro de las 24 horas después de que el usuario te escribió.

---

## Acceder al Business Manager

### Paso 1: Ingresar a Meta Business Suite

1. Ve a [business.facebook.com](https://business.facebook.com)
2. Inicia sesión con tu cuenta de Facebook asociada al negocio
3. Selecciona tu cuenta de Business Manager

### Paso 2: Ir a WhatsApp Manager

1. En el menú lateral izquierdo, busca **"WhatsApp"** o **"WhatsApp Manager"**
2. También puedes acceder directamente a: `https://business.facebook.com/wa/manage/`
3. Selecciona tu cuenta de WhatsApp Business

### Paso 3: Acceder a Plantillas de Mensajes

1. En WhatsApp Manager, ve a **"Herramientas de la cuenta"** → **"Plantillas de mensajes"**
2. O directamente: **"Message Templates"** en el menú

---

## Crear un Template

### Paso 1: Iniciar Creación

1. Haz clic en **"Crear plantilla"** o **"Create Template"**
2. Selecciona la **categoría** del template:

| Categoría | Uso | Ejemplos |
|-----------|-----|----------|
| **Marketing** | Promociones, ofertas | Descuentos, nuevos productos |
| **Utilidad** | Transaccional, actualizaciones | Recordatorios, confirmaciones |
| **Autenticación** | Códigos de verificación | OTPs, 2FA |

> 💡 **Para recordatorios**: Usa la categoría **"Utilidad"**

### Paso 2: Configurar Detalles Básicos

1. **Nombre del template**: 
   - Solo letras minúsculas, números y guiones bajos
   - Ejemplo: `reserva_recordatorio`, `cita_confirmacion`
   
2. **Idioma**: 
   - Selecciona `Español` o el idioma que necesites
   - Puedes crear el mismo template en múltiples idiomas

### Paso 3: Diseñar el Contenido

#### Header (Encabezado) - Opcional
Tipos disponibles:
- **Texto**: Título corto (máx 60 caracteres)
- **Imagen**: JPG o PNG (recomendado 800x418px)
- **Video**: MP4 (máx 16MB)
- **Documento**: PDF

#### Body (Cuerpo) - Obligatorio
El mensaje principal. Aquí defines el texto con variables.

**Formato de variables:**
```
{{1}}, {{2}}, {{3}}, etc.
```

**Ejemplo:**
```
Hola {{1}}, te recordamos tu cita para el {{2}} a las {{3}}.

Tu cita es en {{4}}.

¡Te esperamos!
```

#### Footer (Pie) - Opcional
Texto pequeño al final (máx 60 caracteres).
```
Responde CANCELAR para cancelar tu cita
```

#### Buttons (Botones) - Opcional
Tipos de botones:
1. **Call to Action**:
   - Llamar: `PHONE_NUMBER`
   - Visitar sitio web: `URL`
   
2. **Quick Reply** (Respuesta rápida):
   - Hasta 3 botones
   - Texto máximo: 25 caracteres

---

## Tipos de Templates

### Template Simple (Solo Texto)
```
Hola {{1}}, te recordamos tu reserva:
📅 Fecha: {{2}}
⏰ Hora: {{3}}

¡Te esperamos!
```

### Template con Botones Quick Reply
```
Hola {{1}}, tu cita es el {{2}} a las {{3}}.

¿Confirmas tu asistencia?

[Confirmar ✅] [Cancelar ❌]
```

### Template con Call to Action
```
Hola {{1}}, recuerda tu cita el {{2}}.

Si necesitas reprogramar, contáctanos:

[📞 Llamar] [🌐 Ver ubicación]
```

### Template con Header de Imagen
```
[IMAGEN: Logo del negocio]

Hola {{1}}, ¡no olvides tu cita!
📅 {{2}} a las {{3}}

Te esperamos en nuestra sucursal.
```

---

## Variables y Parámetros

### Reglas para Variables

1. **Numeración secuencial**: `{{1}}`, `{{2}}`, `{{3}}`...
2. **No saltar números**: Si usas `{{3}}`, debes tener `{{1}}` y `{{2}}`
3. **Contenido de ejemplo**: Meta requiere ejemplos de cada variable

### Ejemplos de Variables

| Variable | Descripción | Ejemplo |
|----------|-------------|---------|
| `{{1}}` | Nombre del cliente | María |
| `{{2}}` | Fecha de la cita | lunes 15 de marzo |
| `{{3}}` | Hora de la cita | 14:30 |
| `{{4}}` | Nombre del profesional | Dr. García |
| `{{5}}` | Tiempo restante | 24 horas |

### Cómo se Envían desde el Bot

En el código Java, los parámetros se pasan en orden:

```java
whatsAppService.sendTemplateMessage(
    phoneNumber,
    "reserva_recordatorio",
    "es",
    List.of(
        "María",           // {{1}} - nombre del cliente
        "lunes 15 de marzo", // {{2}} - fecha
        "14:30",           // {{3}} - hora
        "Juan",            // {{4}} - nombre del profesional
        "24 horas"         // {{5}} - tiempo restante
    )
);
```

---

## Template de Recordatorio (Ejemplo)

### Configuración Recomendada

**Nombre**: `reserva_recordatorio`  
**Categoría**: Utilidad  
**Idioma**: Español

### Contenido del Template

```
HEADER: (ninguno o tu logo)

BODY:
Hola {{1}} 👋

Te recordamos tu reserva:

📅 Fecha: {{2}}
⏰ Hora: {{3}}
💇 Con: {{4}}
⏳ Falta: {{5}}

Si necesitas cancelar o modificar tu cita, responde a este mensaje.

¡Te esperamos! 

FOOTER:
Responde CANCELAR para cancelar
```

### Valores de Ejemplo (requeridos por Meta)

| Variable | Ejemplo |
|----------|---------|
| `{{1}}` | María García |
| `{{2}}` | viernes 20 de marzo |
| `{{3}}` | 15:00 |
| `{{4}}` | Juan |
| `{{5}}` | 24 horas |

### Alternativa con Botones

```
BODY:
Hola {{1}}, te recordamos tu cita con {{4}} el {{2}} a las {{3}}.

¿Confirmas tu asistencia?

BUTTONS (Quick Reply):
- Confirmar ✅
- Cancelar ❌
- Reprogramar 📅
```

---

## Proceso de Aprobación

### Estados del Template

| Estado | Descripción | Tiempo |
|--------|-------------|--------|
| **Pending** | En revisión | - |
| **Approved** | Aprobado, listo para usar | 24-48 hrs |
| **Rejected** | Rechazado | Inmediato |

### Tiempos de Aprobación

- **Utilidad/Transaccional**: 24-48 horas (más rápido)
- **Marketing**: 24-72 horas
- **Autenticación**: Usualmente mismo día

### Razones Comunes de Rechazo

1. ❌ Contenido promocional en categoría Utilidad
2. ❌ Variables mal formateadas
3. ❌ Contenido que viola políticas de WhatsApp
4. ❌ Gramática o formato incorrecto
5. ❌ Falta de contexto claro
6. ❌ Información engañosa

### Cómo Apelar un Rechazo

1. Ve al template rechazado
2. Revisa el motivo de rechazo
3. Corrige el problema
4. Haz clic en **"Solicitar revisión"** o crea uno nuevo

---

## Modificar un Template

### ⚠️ Limitaciones Importantes

- **NO puedes editar** un template aprobado
- Debes **crear un nuevo template** con los cambios
- El template anterior sigue funcionando

### Pasos para "Modificar"

1. Ve a la lista de templates
2. Copia el contenido del template existente
3. Crea un **nuevo template** con nombre diferente
   - Ej: `reserva_recordatorio_v2`
4. Espera la aprobación del nuevo
5. Actualiza la configuración en `application.properties`:
   ```properties
   reminder.template-name=reserva_recordatorio_v2
   ```
6. (Opcional) Elimina el template antiguo

### Eliminar un Template

1. Ve al template que deseas eliminar
2. Haz clic en **"Eliminar"** o **"Delete"**
3. Confirma la acción

> ⚠️ **Cuidado**: Si el bot está usando ese template, los envíos fallarán.

---

## Errores Comunes y Soluciones

### Error: Template Not Found

```
Error: Template 'xxx' not found
```

**Causas:**
- Nombre incorrecto del template
- Template no aprobado
- Idioma incorrecto

**Solución:**
```properties
# Verificar nombre exacto (minúsculas, guiones bajos)
reminder.template-name=reserva_recordatorio

# Verificar idioma
reminder.template-language=es
```

### Error: Invalid Parameters

```
Error: Parameter count mismatch
```

**Causa:** Número de parámetros no coincide con las variables del template.

**Solución:** Verificar que el número de parámetros en el código coincida con las variables `{{1}}`, `{{2}}`, etc.

### Error: Rate Limit

```
Error: Rate limit exceeded
```

**Causa:** Demasiados mensajes enviados.

**Solución:**
- Implementar cola de mensajes
- Espaciar envíos
- Verificar límites de tu cuenta

### Error: User Not Opted In

```
Error: User has not opted in
```

**Causa:** El usuario no ha dado consentimiento para recibir mensajes.

**Solución:** Solo enviar a usuarios que hayan iniciado conversación o dado consentimiento.

---

## Mejores Prácticas

### Contenido

✅ **Hacer:**
- Ser claro y conciso
- Incluir información relevante
- Usar emojis moderadamente
- Dar opción de cancelar/responder

❌ **No hacer:**
- Usar mayúsculas excesivas
- Incluir enlaces sospechosos
- Enviar contenido no solicitado
- Usar lenguaje agresivo

### Variables

✅ **Hacer:**
- Validar que no estén vacías
- Usar formatos consistentes
- Sanitizar datos de entrada

```java
// ✅ Bueno
String name = customerName != null ? customerName : "Cliente";

// ❌ Malo
String name = customerName; // Puede ser null
```

### Timing de Recordatorios

| Tipo de Servicio | Recordatorio 1 | Recordatorio 2 |
|------------------|----------------|----------------|
| Cita médica | 24 horas | 2 horas |
| Restaurante | 4 horas | 30 min |
| Peluquería | 24 horas | 1 hora |
| Evento | 1 semana | 1 día |

### Configuración Recomendada para el Bot

```properties
# Producción
reminder.enabled=true
reminder.template-name=reserva_recordatorio
reminder.template-language=es
reminder.check-interval-minutes=5

reminder.times[0].minutes-before=1440
reminder.times[0].enabled=true

reminder.times[1].minutes-before=120
reminder.times[1].enabled=true
```

---

## Referencias

- [Documentación oficial de WhatsApp Business API](https://developers.facebook.com/docs/whatsapp/cloud-api/guides/send-message-templates)
- [Guía de Message Templates](https://developers.facebook.com/docs/whatsapp/message-templates)
- [WhatsApp Business Policy](https://www.whatsapp.com/legal/business-policy/)
- [Meta Business Help Center](https://www.facebook.com/business/help)

---

## Soporte

Si tienes problemas:

1. Verifica el estado del template en WhatsApp Manager
2. Revisa los logs de la aplicación
3. Consulta la documentación de Meta
4. Contacta al soporte de Meta Business

---

*Última actualización: Marzo 2026*
