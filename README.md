# Bot de WhatsApp para Reservas - Java Spring Boot

Bot automatizado para gestionar reservas mediante WhatsApp Business API, integrado con Google Calendar y PostgreSQL.

## 🚀 Características

- ✅ WhatsApp Business API oficial (no Twilio)
- 📅 Integración completa con Google Calendar API
- 🗄️ Persistencia con PostgreSQL
- 👥 **Multi-profesional**: soporte para múltiples calendarios/peluqueros
- ⏰ Verificación automática de horarios disponibles
- 🔄 Máquina de estados para conversaciones
- 💈 **Servicios con duración dinámica**: corte/corte+barba (60 min), barba/un solo número (30 min)
- 📱 Soporte para mensajes interactivos y templates
- 🔔 **Recordatorios automáticos** configurables (24h, 2h, 30min antes)
- 🏗️ Arquitectura Spring Boot con buenas prácticas

## 📋 Requisitos Previos

- **Java 17** o superior
- **Maven 3.6+**
- **PostgreSQL 12+** (o Docker)
- **Cuenta de Meta Business** con WhatsApp Business API
- **Cuenta de Google** con Calendar API

## 📖 Documentación

Toda la documentación está organizada en [`docs/`](docs/README.md):

### Instalación y Configuración
- [SETUP.md](docs/setup/SETUP.md) - Instalación y configuración inicial
- [QUICKSTART_WSL.md](docs/setup/QUICKSTART_WSL.md) - Guía rápida para WSL
- [DEPLOY.md](docs/setup/DEPLOY.md) - Deploy en producción

### Funcionalidades
- [REMINDERS.md](docs/features/REMINDERS.md) - Sistema de recordatorios automáticos
- [SERVICIOS.md](docs/features/SERVICIOS.md) - Catálogo de servicios, duraciones y reglas
- [WHATSAPP_TEMPLATES.md](docs/features/WHATSAPP_TEMPLATES.md) - Crear/modificar templates de Meta
- [SYNC_RESERVAS.md](docs/features/SYNC_RESERVAS.md) - Sincronización con Google Calendar

### Otros
- [TROUBLESHOOTING.md](docs/TROUBLESHOOTING.md) - Solución de problemas
- [Pricing](docs/pricing/) - Estimación de costos WhatsApp API

## ⚡ Quick Start

```bash
# Clonar
git clone <tu-repo> && cd whatsapp-bot

# PostgreSQL (Docker)
docker-compose up -d

# Configurar credenciales
cp .env.example .env
# Editar .env con tus datos

# Compilar e iniciar
./scripts/run.sh
# o con logs en archivo:
./scripts/run-with-logs.sh

# En otra terminal: exponer puerto con ngrok
ngrok http 8080
```

Luego ve a [SETUP.md](docs/setup/SETUP.md) para configurar el webhook en Meta.

## 🧪 Probar el Bot

```bash
# Script de chat interactivo (simula WhatsApp)
./scripts/webhook-chat.sh <tu-numero>

# Probar recordatorios
./scripts/_test/test-reminders.sh list
./scripts/_test/test-reminders.sh send <reservation-id>
```

## 🏗️ Estructura del Proyecto

```
├── src/main/java/com/reservas/whatsapp/
│   ├── controller/       # Webhook de WhatsApp, test endpoints
│   ├── service/          # Lógica (conversación, calendario, WhatsApp, reminders)
│   ├── model/            # Entidades (Reservation, UserSession, Staff, ReminderLog)
│   ├── repository/       # Acceso a datos
│   └── config/           # Configuración (Calendar, Reminders, Staff)
├── src/main/resources/
│   ├── application.properties
│   ├── schema.sql
│   └── credentials.json  # (agregar - Google Calendar)
├── docs/                 # Documentación organizada
├── scripts/              # Scripts de ejecución y pruebas
├── docker-compose.yml
├── pom.xml
└── .env                  # (crear con tus datos)
```

## 🔒 Seguridad

- ✅ Nunca subas `.env`, `credentials.json`, `tokens/` a Git
- ✅ Usa variables de entorno en producción
- ✅ HTTPS obligatorio (ngrok en dev, certificado en prod)
- ✅ Valida todos los inputs del usuario
- ✅ Rota el access token periódicamente

## 📚 Enlaces Útiles

- [WhatsApp Business API](https://developers.facebook.com/docs/whatsapp)
- [Google Calendar API Java](https://developers.google.com/calendar/api/quickstart/java)
- [Spring Boot](https://spring.io/projects/spring-boot)
- [PostgreSQL](https://www.postgresql.org/docs/)

## 🎯 Mejoras Futuras

- Cancelación de reservas
- Confirmaciones por email
- Recordatorios automáticos
- Integración de pagos
- Dashboard web
- Multi-idioma
- Reportes y analytics

## 📄 Licencia

Libre para uso personal y comercial.

---

**¿Problemas?** Revisa [TROUBLESHOOTING.md](TROUBLESHOOTING.md)
