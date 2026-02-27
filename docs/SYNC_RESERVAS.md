# Sincronización de Reservas desde Google Calendar

## Cómo usar

Desde cualquier terminal o script, ejecuta:

``` bash
curl -X POST http://localhost:8080/webhook/sync-calendar
```

## Qué hace este endpoint

El endpoint sincroniza todos los eventos de Google Calendar que
coincidan con el patrón:

    Reserva - *

Y los guarda en la tabla `reservations`, incluyendo:

-   Teléfono
-   Fecha y hora
-   Datos asociados a la reserva

## Comportamiento de la sincronización

La sincronización ya se encuentra funcionando.\
Cada vez que llames el endpoint, la respuesta indicará:

-   ✅ Cuántas nuevas reservas se importaron
-   🔁 Cuántas reservas ya existían (no se duplican)
-   ⚠️ Si hubo errores en el parseo de eventos

## Ejemplo de respuesta esperada

``` json
{
  "imported": 3,
  "alreadyExisting": 5,
  "errors": 1
}
```
