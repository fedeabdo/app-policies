---
name: deploy-local-wsl
description: "Use when: preparar entorno local o WSL, levantar servicios, ejecutar app con logs, revisar configuracion de deploy y resolver problemas de setup."
---

# Skill: Setup, Deploy y Operacion Local/WSL

## Objetivo
Estandarizar el arranque del proyecto y la resolucion de problemas de entorno para desarrollo y despliegue.

## Inputs Esperados
- Objetivo operativo: levantar local, validar predeploy o depurar entorno.
- Sistema objetivo (Linux/WSL).
- Errores de inicializacion o conectividad.

## Referencias del Proyecto
- docs/setup/SETUP.md
- docs/setup/QUICKSTART_WSL.md
- docs/setup/DEPLOY.md
- scripts/run.sh
- scripts/run-with-logs.sh
- docker-compose.yml

## Flujo Recomendado
1. Validar prerequisitos: Java, Maven, Docker/Postgres, variables .env.
2. Levantar dependencias (DB) y luego aplicacion.
3. Ejecutar con logs persistentes cuando se investiga un problema.
4. Verificar salud minima: app responde, webhook accesible, DB conectada.
5. Seguir checklist de deploy para evitar omisiones.

## Checklist de Calidad
- Entorno reproducible con comandos documentados.
- Logs disponibles para diagnostico rapido.
- Secretos y credenciales fuera de git.
- Procedimiento claro para reinicio limpio.

## Pitfalls Frecuentes
- Mezclar config de dev y prod sin control.
- Credenciales incompletas o rutas invalidas.
- Diagnosticar sin revisar logs de arranque.

## Salida Esperada
- Entorno funcional y estable.
- Pasos de setup/deploy claros para el equipo.
- Troubleshooting operativo reducido.
