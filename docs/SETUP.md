# 🔧 Instalación y Configuración

## 1. Clonar el repositorio

```bash
git clone <tu-repo>
cd whatsapp-bot
```

## 2. Configurar PostgreSQL

### Opción A: Usar Docker (Recomendado)

```bash
docker-compose up -d
```

### Opción B: PostgreSQL local

```bash
psql -U postgres
CREATE DATABASE whatsapp_reservas;
\q
```

## 3. Configurar Google Calendar API

### a) Crear proyecto en Google Cloud Console

1. Ve a [Google Cloud Console](https://console.cloud.google.com/)
2. Crea un nuevo proyecto
3. Habilita **Google Calendar API**:
   - APIs y Servicios → Biblioteca
   - Busca "Google Calendar API" → Habilitar

### b) Crear credenciales OAuth 2.0

1. APIs y Servicios → Credenciales
2. "Crear credenciales" → "ID de cliente de OAuth"
3. Configurar pantalla de consentimiento:
   - Tipo: Externo (o Interno si es G Suite)
   - Agrega tu email como usuario de prueba
4. Tipo de aplicación: "Aplicación de escritorio"
5. Descarga el JSON y renombra a `credentials.json`
6. Colócalo en `src/main/resources/credentials.json`

## 4. Configurar WhatsApp Business API

### a) Crear cuenta en Meta for Developers

1. Ve a [developers.facebook.com](https://developers.facebook.com/)
2. Crea una app → Tipo: "Business"
3. Agrega producto "WhatsApp"

### b) Configurar credenciales WhatsApp

1. En el Dashboard de WhatsApp, ve a "API Setup"
2. Copia el **Phone Number ID** (número largo de 15+ dígitos)
3. Genera un **Access Token** permanente:
   - Ve a "System Users" en Business Settings
   - Crea un System User
   - Genera un token con permisos `whatsapp_business_messaging` y `whatsapp_business_management`

### c) Agregar número de teléfono de prueba

1. En WhatsApp → Getting Started
2. Agrega tu número personal como destinatario de prueba
3. Confirma el código que te llega por WhatsApp

## 5. Configurar Variables de Entorno

Crea un archivo `.env` en la raíz del proyecto:

```env
WHATSAPP_PHONE_NUMBER_ID=123456789012345
WHATSAPP_ACCESS_TOKEN=EAAxxxxxxxxxxxxxxxxxxxxxxxx
WHATSAPP_VERIFY_TOKEN=mi-token-secreto-123

SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/whatsapp_reservas
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=postgres
```

## 6. Compilar e Iniciar

```bash
mvn clean install
mvn spring-boot:run
```

La aplicación estará en `http://localhost:8080`

## 7. Configurar Webhook de WhatsApp

### Instalar ngrok (Linux)

```bash
# Opción 1: Usando snap
sudo snap install ngrok

# Opción 2: Descargar desde ngrok.com
# https://ngrok.com/download
tar xvzf ~/Downloads/ngrok-v3-stable-linux-amd64.tgz
sudo mv ngrok /usr/local/bin/

# Autenticar
ngrok authtoken tu_token_aqui

# Exponer puerto 8080
ngrok http 8080
```

Verás una salida como:
```
Forwarding                    https://abc123.ngrok.io -> http://localhost:8080
```

### Registrar Webhook en Meta

1. Ve a [Meta for Developers](https://developers.facebook.com/)
2. Selecciona tu app de WhatsApp
3. Ve a WhatsApp → Configuration
4. En "Webhook", haz clic en "Edit"
5. **Callback URL**: `https://abc123.ngrok.io/webhook`
6. **Verify Token**: El valor que pusiste en `.env`
7. Haz clic en "Verify and Save"
8. Suscríbete a `messages` en "Webhook fields"

## 📝 Configuración Personalizada

### Horarios de trabajo

En `application.properties`:

```properties
business.hours.start=9
business.hours.end=18
business.hours.reservation-duration-minutes=60
```

### Zona horaria

```properties
business.timezone=America/Montevideo
```

Lista completa: [Wikipedia - TZ Database](https://en.wikipedia.org/wiki/List_of_tz_database_time_zones)

### Calendario específico

En `application.properties`:

```properties
google.calendar.calendar-id=tu_calendar_id@group.calendar.google.com
```
