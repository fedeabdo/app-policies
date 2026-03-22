# Servicios y Duraciones

Este documento describe los servicios disponibles en el bot, su duración y reglas especiales.

## Servicios disponibles

| Servicio | Duración | ¿Se agenda por bot? |
|----------|----------|---------------------|
| Corte normal | 60 min | Sí |
| Corte y barba | 60 min | Sí |
| Barba | 30 min | Sí |
| Corte un solo número con máquina | 30 min | Sí |
| Color | - | No |

## Regla especial para Color

Cuando el cliente elige **color**, el bot no ofrece horarios ni confirma reserva.
En su lugar, informa este mensaje:

- Debe llamar al local al **22086214** para coordinar.

## Flujo conversacional actualizado

1. El usuario indica fecha.
2. El bot solicita el servicio.
3. Si el servicio es **color**, se deriva por teléfono (22086214).
4. Para los demás servicios, el bot solicita peluquero y horario.
5. La disponibilidad y la validación final usan la duración del servicio.

## Nota técnica

- Duración 30 min: `barba`, `corte un solo número con máquina`.
- Duración 60 min: `corte normal`, `corte y barba`.
- El evento en Google Calendar guarda también el servicio en la descripción (`Servicio: ...`).
