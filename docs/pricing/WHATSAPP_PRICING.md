# Estimación de Costos WhatsApp Business API

> Documento actualizado: Marzo 2026

## Fuentes Oficiales

- **Documentación de Precios Meta**: https://developers.facebook.com/docs/whatsapp/pricing
- **Calculadora de Precios WhatsApp Business**: https://business.whatsapp.com/products/platform-pricing

---

## Modelo de Precios (Vigente desde Julio 2025)

Meta cobra **por mensaje de template entregado**. Los cambios principales del modelo actual:

| Tipo de Mensaje | Costo |
|-----------------|-------|
| **Template Messages** (fuera de ventana) | Se cobra por mensaje entregado |
| **Non-template Messages** (texto, imagen, etc.) | **GRATIS** (dentro de Customer Service Window) |
| **Utility templates** (dentro de CSW) | **GRATIS** |
| **Service Messages** | **GRATIS** |

### Customer Service Window (CSW)
- Se abre cuando el cliente te envía un mensaje
- Dura **24 horas**
- Durante este período, puedes enviar mensajes de texto normales sin costo

### Free Entry Point Window
- Si el cliente te contacta desde un "Click to WhatsApp Ad" o botón de Facebook
- Tienes **72 horas** de mensajes gratis (incluidos templates)

---

## Precios por Región

### Uruguay (+598) - Región: "Rest of Latin America"

| Categoría | Precio por mensaje (USD) |
|-----------|-------------------------|
| **Utility** | ~$0.0420 |
| **Marketing** | ~$0.0625 |
| **Authentication** | ~$0.0420 |
| **Service** | GRATIS |


> **Nota**: Los precios pueden variar cada trimestre (1 enero, 1 abril, 1 julio, 1 octubre). Consultar siempre la [documentación oficial](https://developers.facebook.com/docs/whatsapp/pricing).

---

## Análisis del Flujo de la Aplicación

### Flujo Típico de Reserva

```
1. Cliente escribe "hola" o fecha → Abre CSW (24h)
2. Bot responde con opciones     → GRATIS (dentro de CSW)
3. Cliente elige fecha           → No cobra (mensaje del cliente)
4. Bot muestra horarios          → GRATIS (dentro de CSW)
5. Cliente elige horario         → No cobra
6. Bot pide nombre               → GRATIS (dentro de CSW)
7. Cliente da nombre             → No cobra
8. Bot confirma reserva          → GRATIS (dentro de CSW)
```

**Resultado: Toda la conversación de reserva es GRATIS** si el cliente inicia.

### Recordatorios de Cita (Template Messages)

Los recordatorios enviados **fuera** de la ventana de 24h **SÍ se cobran**:

| Tipo de Recordatorio | Categoría | Costo Uruguay |
|---------------------|-----------|---------------|
| Recordatorio 24h antes | Utility | ~$0.0420 USD |
| Recordatorio 1h antes | Utility | ~$0.0420 USD |
| Promociones | Marketing | ~$0.0625 USD |

---

## Capacidad de la Peluquería

### Configuración Base
- **Peluqueros**: 3
- **Duración por corte**: 1 hora
- **Horario**: 9:00 a 18:00 (9 horas/día)
- **Días laborables**: 6 días/semana (Lunes a Sábado)
- **Semanas por mes**: 4

### Cálculo de Capacidad Máxima

```
Cortes por peluquero/día:     9 cortes
Cortes por peluquero/semana:  9 × 6 = 54 cortes
Cortes por peluquero/mes:     54 × 4 = 216 cortes

CAPACIDAD TOTAL (3 peluqueros): 216 × 3 = 648 cortes/mes
```

---

## Escenarios de Estimación Mensual

### Variables de cada escenario:
- **Ocupación**: % de la capacidad total que se usa
- **% Bot**: % de las reservas que se hacen por WhatsApp (vs teléfono, walk-in, etc.)
- **Recordatorios**: 1 o 2 templates por reserva

---

### Escenario 1: Peluquería Nueva / Baja Demanda

| Parámetro | Valor |
|-----------|-------|
| Ocupación | 30% |
| Cortes totales/mes | 648 × 30% = **195 cortes** |
| % Reservas por Bot | 30% |
| Reservas por Bot | 195 × 30% = **~60 reservas** |

| Concepto | Cantidad | Costo unitario | Total |
|----------|----------|----------------|-------|
| Conversaciones de reserva | 60 | $0 (CSW) | $0 |
| Recordatorio 24h antes | 60 | $0.042 | $2.52 |
| **TOTAL con 1 recordatorio** | | | **$2.52 USD** |
| **TOTAL con 2 recordatorios** | | | **$5.04 USD** |

---

### Escenario 2: Peluquería Establecida / Demanda Media

| Parámetro | Valor |
|-----------|-------|
| Ocupación | 50% |
| Cortes totales/mes | 648 × 50% = **324 cortes** |
| % Reservas por Bot | 50% |
| Reservas por Bot | 324 × 50% = **~160 reservas** |

| Concepto | Cantidad | Costo unitario | Total |
|----------|----------|----------------|-------|
| Conversaciones de reserva | 160 | $0 (CSW) | $0 |
| Recordatorio 24h antes | 160 | $0.042 | $6.72 |
| **TOTAL con 1 recordatorio** | | | **$6.72 USD** |
| **TOTAL con 2 recordatorios** | | | **$13.44 USD** |

---

### Escenario 3: Peluquería Popular / Alta Demanda

| Parámetro | Valor |
|-----------|-------|
| Ocupación | 70% |
| Cortes totales/mes | 648 × 70% = **454 cortes** |
| % Reservas por Bot | 60% |
| Reservas por Bot | 454 × 60% = **~270 reservas** |

| Concepto | Cantidad | Costo unitario | Total |
|----------|----------|----------------|-------|
| Conversaciones de reserva | 270 | $0 (CSW) | $0 |
| Recordatorio 24h antes | 270 | $0.042 | $11.34 |
| **TOTAL con 1 recordatorio** | | | **$11.34 USD** |
| **TOTAL con 2 recordatorios** | | | **$22.68 USD** |

---

### Escenario 4: Peluquería Muy Popular / Bot como Canal Principal

| Parámetro | Valor |
|-----------|-------|
| Ocupación | 85% |
| Cortes totales/mes | 648 × 85% = **551 cortes** |
| % Reservas por Bot | 80% |
| Reservas por Bot | 551 × 80% = **~440 reservas** |

| Concepto | Cantidad | Costo unitario | Total |
|----------|----------|----------------|-------|
| Conversaciones de reserva | 440 | $0 (CSW) | $0 |
| Recordatorio 24h antes | 440 | $0.042 | $18.48 |
| **TOTAL con 1 recordatorio** | | | **$18.48 USD** |
| **TOTAL con 2 recordatorios** | | | **$36.96 USD** |

---

## Resumen de Costos por Escenario

| Escenario | Ocupación | % Bot | Reservas Bot | 1 recordatorio | 2 recordatorios |
|-----------|-----------|-------|--------------|----------------|-----------------|
| Nueva/Baja | 30% | 30% | ~60 | **~$2.50** | **~$5.00** |
| Establecida | 50% | 50% | ~160 | **~$6.70** | **~$13.50** |
| Popular | 70% | 60% | ~270 | **~$11.30** | **~$22.70** |
| Muy Popular | 85% | 80% | ~440 | **~$18.50** | **~$37.00** |

---

## Simulador Rápido

Fórmula para calcular tu costo estimado:

```
Cortes_mes = (horas_dia × dias_semana × 4 semanas × num_peluqueros) × ocupacion%
Reservas_bot = Cortes_mes × porcentaje_bot%
Costo_mensual = Reservas_bot × $0.042 × num_recordatorios
```

**Ejemplo personalizado:**
- 3 peluqueros, 8 horas/día, 5 días/semana, 60% ocupación, 40% por bot, 2 recordatorios
- Cortes/mes: (8 × 5 × 4 × 3) × 0.60 = 288 cortes
- Reservas bot: 288 × 0.40 = 115 reservas
- Costo: 115 × $0.042 × 2 = **$9.66 USD/mes**

---

## Optimizaciones para Reducir Costos

### 1. Utility Templates dentro del CSW son GRATIS
Si envías confirmación inmediatamente después de que el cliente reserve (mientras la ventana sigue abierta), no pagas.

### 2. Agrupar recordatorios
En lugar de enviar 2 recordatorios separados, considera enviar solo 1 recordatorio bien redactado.

### 3. Usar categoría correcta
- **Utility** para recordatorios de citas (más barato)
- **Marketing** solo para promociones (más caro)

### 4. Volume Tiers
A partir de 25,000 mensajes/mes en una categoría, se desbloquean descuentos automáticos.

---

## Consideraciones Adicionales

### Templates Requieren Aprobación
- Debes crear y aprobar templates en [Meta Business Suite](https://business.facebook.com/)
- El proceso de aprobación puede tomar 24-48 horas
- Templates rechazados deben modificarse y reenviarse

### Facturación
- La facturación se maneja a través de Meta Business Suite
- Se cobra por mensajes **entregados** (no enviados)
- Los pagos se realizan mediante tarjeta de crédito o prepago

### Actualizaciones de Precios
- Meta puede actualizar precios cada trimestre
- Se notifica con 1 mes de anticipación para cambios de tarifas
- Revisar periódicamente la [documentación oficial](https://developers.facebook.com/docs/whatsapp/pricing)

---

## Enlaces Útiles

| Recurso | URL |
|---------|-----|
| Documentación de Precios | https://developers.facebook.com/docs/whatsapp/pricing |
| Calculadora de Precios | https://business.whatsapp.com/products/platform-pricing |
| Rate Cards (CSV) | https://developers.facebook.com/docs/whatsapp/pricing#rate-cards-and-volume-tiers |
| Categorías de Templates | https://developers.facebook.com/docs/whatsapp/templates/template-categorization |
| Facturación WhatsApp | https://www.facebook.com/business/help/2225184664363779 |

---

*Última actualización: Marzo 2026*
*Precios sujetos a cambios por Meta*
