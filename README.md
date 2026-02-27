# Bot de WhatsApp para Reservas - Java Spring Boot

Bot automatizado para gestionar reservas mediante WhatsApp Business API, integrado con Google Calendar y PostgreSQL.

## 🚀 Características

- ✅ WhatsApp Business API oficial (no Twilio)
- 📅 Integración completa con Google Calendar API
- 🗄️ Persistencia con PostgreSQL
- ⏰ Verificación automática de horarios disponibles
- 🔄 Máquina de estados para conversaciones
- 📱 Soporte para mensajes interactivos
- 🏗️ Arquitectura Spring Boot con buenas prácticas

## 📋 Requisitos Previos

- **Java 17** o superior
- **Maven 3.6+**
- **PostgreSQL 12+** (o Docker)
- **Cuenta de Meta Business** con WhatsApp Business API
- **Cuenta de Google** con Calendar API

## 📖 Documentación

1. **[docs/SETUP.md](SETUP.md)** - Instalación y configuración inicial
2. **[docs/TROUBLESHOOTING.md](TROUBLESHOOTING.md)** - Solución de problemas
3. **[docs/DEPLOY.md](DEPLOY.md)** - Pasar a producción
4. **[docs/SYNC_RESERVAS.md](SYNC_RESERVAS.md)** - Sincronización con Google Calendar
5. **[docs/QUICKSTART_WSL.md](QUICKSTART_WSL.md)** - Guía rápida para desarrollo en WSL

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
mvn clean install
mvn spring-boot:run

# En otra terminal: exponer puerto con ngrok
ngrok http 8080
```

Luego ve a [SETUP.md](SETUP.md) para configurar el webhook en Meta.

## 🧪 Probar el Bot

```bash
# Script de prueba (simula mensaje de WhatsApp)
chmod +x test-webhook.sh
./test-webhook.sh
```

## 🏗️ Estructura del Proyecto

```
├── src/main/java/com/reservas/whatsapp/
│   ├── controller/       # Webhook de WhatsApp
│   ├── service/          # Lógica (conversación, calendario, WhatsApp)
│   ├── model/            # Entidades (Reservation, UserSession)
│   ├── repository/       # Acceso a datos
│   └── config/           # Configuración (Google Calendar)
├── src/main/resources/
│   ├── application.properties
│   └── credentials.json  (agregar después)
├── docker-compose.yml
├── pom.xml
└── .env                  (crear con tus datos)
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
