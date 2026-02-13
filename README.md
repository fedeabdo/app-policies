# Bot de WhatsApp para Reservas con Google Calendar

Bot automatizado para gestionar reservas mediante WhatsApp, integrado con Google Calendar.

## 🚀 Características

- ✅ Conversación interactiva en WhatsApp
- 📅 Integración completa con Google Calendar
- ⏰ Verificación automática de horarios disponibles
- 🔄 Confirmación de reservas
- 📱 Recordatorios automáticos
- 🌐 Soporte para múltiples usuarios simultáneos

## 📋 Requisitos Previos

- Python 3.8 o superior
- Cuenta de Google con Calendar habilitado
- Cuenta de Twilio (incluye crédito gratuito de prueba)
- ngrok (para pruebas locales) o servidor web con dominio

## 🔧 Instalación

### 1. Clonar o descargar los archivos

Asegúrate de tener estos archivos:
- `whatsapp_bot.py`
- `google_calendar_manager.py`
- `requirements.txt`
- `.env.example`

### 2. Instalar dependencias

```bash
pip install -r requirements.txt
```

### 3. Configurar Google Calendar API

#### a) Crear proyecto en Google Cloud Console

1. Ve a [Google Cloud Console](https://console.cloud.google.com/)
2. Crea un nuevo proyecto o selecciona uno existente
3. En el menú de navegación, ve a "APIs y Servicios" > "Biblioteca"
4. Busca "Google Calendar API" y habilítala

#### b) Crear credenciales OAuth 2.0

1. Ve a "APIs y Servicios" > "Credenciales"
2. Haz clic en "Crear credenciales" > "ID de cliente de OAuth"
3. Selecciona "Aplicación de escritorio"
4. Dale un nombre (ej: "Bot WhatsApp Reservas")
5. Descarga el archivo JSON
6. Renombra el archivo descargado a `credentials.json`
7. Coloca `credentials.json` en la carpeta del proyecto

#### c) Autorizar la aplicación

La primera vez que ejecutes el bot, se abrirá un navegador para autorizar el acceso:

```bash
python google_calendar_manager.py
```

Esto creará un archivo `token.json` que almacena tus credenciales.

### 4. Configurar Twilio para WhatsApp

#### a) Crear cuenta en Twilio

1. Ve a [Twilio](https://www.twilio.com/try-twilio)
2. Regístrate (obtendrás crédito gratuito para pruebas)
3. Verifica tu número de teléfono

#### b) Activar WhatsApp Sandbox

1. En el dashboard de Twilio, ve a "Messaging" > "Try it out" > "Send a WhatsApp message"
2. Sigue las instrucciones para unirte al sandbox:
   - Envía un mensaje de WhatsApp al número que te indican
   - El mensaje debe ser algo como: `join <código-único>`

#### c) Obtener credenciales

1. En el dashboard, ve a "Account" > "API keys & tokens"
2. Copia tu **Account SID** y **Auth Token**

#### d) Configurar variables de entorno

Copia `.env.example` a `.env` y completa con tus datos:

```bash
cp .env.example .env
```

Edita el archivo `.env`:

```
TWILIO_ACCOUNT_SID=ACxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
TWILIO_AUTH_TOKEN=tu_auth_token_aqui
TWILIO_WHATSAPP_NUMBER=whatsapp:+14155238886
PORT=5000
```

### 5. Configurar Webhook de Twilio

Para que Twilio pueda enviar mensajes a tu bot, necesitas un URL público.

#### Opción A: Usando ngrok (para pruebas locales)

1. Descarga [ngrok](https://ngrok.com/download)
2. Ejecuta tu bot:
   ```bash
   python whatsapp_bot.py
   ```
3. En otra terminal, ejecuta ngrok:
   ```bash
   ngrok http 5000
   ```
4. Copia la URL HTTPS que te da ngrok (ej: `https://abc123.ngrok.io`)

#### Opción B: Servidor en producción

Si tienes un servidor web, despliega el bot y usa tu dominio.

#### Configurar el webhook en Twilio

1. Ve a "Messaging" > "Settings" > "WhatsApp sandbox settings"
2. En "When a message comes in", pega tu URL + `/webhook`:
   - Ejemplo: `https://abc123.ngrok.io/webhook`
3. Asegúrate de que el método sea **POST**
4. Guarda los cambios

## 🎮 Uso

### Iniciar el bot

```bash
python whatsapp_bot.py
```

El servidor estará corriendo en `http://localhost:5000`

### Probar el bot

1. Envía un mensaje de WhatsApp al número de Twilio
2. El bot te guiará a través del proceso de reserva:
   - Te pedirá la fecha deseada
   - Mostrará horarios disponibles
   - Pedirá tu nombre
   - Confirmará la reserva

### Comandos disponibles

- **Cualquier mensaje inicial**: Inicia el proceso de reserva
- **"horarios"** o **"disponibilidad"**: Muestra horarios disponibles
- **"hoy"** o **"mañana"**: Para seleccionar fecha
- **DD/MM/YYYY**: Formato de fecha específica

## ⚙️ Configuración Avanzada

### Personalizar horarios de trabajo

Edita en `google_calendar_manager.py`:

```python
self.hora_inicio = 9      # Hora de inicio (9 AM)
self.hora_fin = 18        # Hora de fin (6 PM)
self.duracion_reserva = 60  # Duración en minutos
```

### Cambiar zona horaria

En `google_calendar_manager.py`, busca:

```python
'timeZone': 'America/Montevideo',
```

Reemplaza con tu zona horaria. Lista completa: [Zonas horarias](https://en.wikipedia.org/wiki/List_of_tz_database_time_zones)

### Usar un calendario específico

Si quieres usar un calendario diferente al principal:

1. Ve a Google Calendar
2. Haz clic en los tres puntos del calendario deseado
3. Ve a "Configuración y uso compartido"
4. Copia el "ID del calendario"
5. Al inicializar el manager:

```python
calendar_manager = GoogleCalendarManager(calendar_id='tu_calendar_id_aqui')
```

## 📱 Pasar a Producción

### 1. Actualizar a Twilio completo (no sandbox)

Para usar con cualquier número de WhatsApp (no solo los del sandbox):

1. En Twilio, solicita aprobación para "WhatsApp Business"
2. Sigue el proceso de verificación de Meta
3. Una vez aprobado, obtendrás un número de WhatsApp dedicado

### 2. Desplegar en un servidor

Opciones recomendadas:
- **Heroku**: Fácil y con capa gratuita
- **Railway**: Moderna y sencilla
- **DigitalOcean**: Más control, VPS desde $5/mes
- **Google Cloud Run**: Serverless, paga por uso

### 3. Usar una base de datos

Para producción, reemplaza `user_sessions` (dict en memoria) con:
- **Redis**: Para sesiones rápidas
- **PostgreSQL/MySQL**: Para almacenamiento permanente
- **Firebase Firestore**: Base de datos NoSQL en tiempo real

### 4. Agregar logs y monitoreo

```python
import logging

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)
```

## 🔒 Seguridad

- ⚠️ **NUNCA** subas `credentials.json`, `token.json` o `.env` a repositorios públicos
- Agrega a `.gitignore`:
  ```
  credentials.json
  token.json
  .env
  __pycache__/
  *.pyc
  ```
- Usa variables de entorno para secretos
- En producción, usa HTTPS siempre

## 🐛 Solución de Problemas

### Error: "No module named 'google'"

```bash
pip install --upgrade google-api-python-client
```

### Error: "Invalid grant" en Google Calendar

Elimina `token.json` y vuelve a autenticar:

```bash
rm token.json
python google_calendar_manager.py
```

### Bot no responde

1. Verifica que el webhook esté correctamente configurado en Twilio
2. Revisa que ngrok esté corriendo (si es local)
3. Chequea los logs del servidor para ver errores
4. Verifica que estés en el sandbox de WhatsApp de Twilio

### Horarios no aparecen

1. Verifica que el calendario tenga eventos
2. Revisa la zona horaria en el código
3. Asegúrate de que `hora_inicio` y `hora_fin` sean correctos

## 📚 Recursos Adicionales

- [Documentación de Twilio WhatsApp](https://www.twilio.com/docs/whatsapp)
- [Google Calendar API](https://developers.google.com/calendar/api/guides/overview)
- [Flask Documentation](https://flask.palletsprojects.com/)

## 🤝 Contribuciones

Este es un proyecto base que puedes expandir:

- ✨ Agregar cancelación de reservas
- 📧 Enviar confirmaciones por email
- 💳 Integrar pagos
- 📊 Panel de administración web
- 🔔 Recordatorios automáticos
- 🌍 Soporte multi-idioma

## 📄 Licencia

Libre para uso personal y comercial.

---

¿Preguntas o problemas? Revisa la sección de troubleshooting o abre un issue.
