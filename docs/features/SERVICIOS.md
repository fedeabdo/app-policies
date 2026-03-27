# Servicios y Duraciones

Este documento describe los servicios disponibles en el bot, su duración y reglas especiales.

## Servicios disponibles

| Servicio | Duración | ¿Se agenda por bot? |
|----------|----------|---------------------|
| Corte y barba | 60 min | Sí |
| Barba | 30 min | Sí |
| Corte un solo número con máquina | 30 min | Sí |
| Color | - | No |

## Servicio no disponible

El servicio **solo corte** ya no se ofrece.
Si el cliente lo pide, el bot le informa que no está disponible y ofrece los demás servicios.

## Regla especial para Color

Cuando el cliente elige **color**, el bot no ofrece horarios ni confirma reserva.
En su lugar, informa este mensaje:

- Debe llamar al local al **22086214** para coordinar.

## Flujo conversacional actualizado

1. El usuario indica fecha.
2. El bot solicita el servicio.
3. Si el cliente solicita **solo corte**, se informa que no está disponible.
4. Si el servicio es **color**, se deriva por teléfono (22086214).
5. Para **corte y barba**, **barba** o **corte un solo número con máquina**, el bot solicita peluquero y horario.
6. La disponibilidad y la validación final usan la duración del servicio.

## Nota técnica

- Duración 30 min: `barba`, `corte un solo número con máquina`.
- Duración 60 min: `corte y barba`.
- El evento en Google Calendar guarda también el servicio en la descripción (`Servicio: ...`).
