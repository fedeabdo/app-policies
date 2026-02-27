# 🚀 Guía Rápida: Instalación en WSL

Esta guía te ayudará a tener el bot funcionando en Windows usando WSL en menos de 30 minutos.

## 📋 Requisitos

- Windows 10 versión 2004+ (Build 19041+) o Windows 11
- Permisos de administrador

## ⚡ Instalación en 5 pasos

### **Paso 1: Instalar WSL (5 min)**

Abre **PowerShell como Administrador** y ejecuta:

```powershell
wsl --install
```

**Reinicia tu PC** cuando te lo pida.

### **Paso 2: Configurar Ubuntu (2 min)**

Después del reinicio, Ubuntu se abrirá automáticamente:

```bash
# Crear usuario y contraseña
Enter new UNIX username: tuusuario
New password: ********
```

### **Paso 3: Instalar dependencias (10 min)**

```bash
# Actualizar sistema
sudo apt update && sudo apt upgrade -y

# Extraer el proyecto
cd ~
mkdir projects
cd projects

# Si tienes el .tar.gz en Downloads:
cp /mnt/c/Users/TuUsuario/Downloads/whatsapp-bot-java-springboot.tar.gz .
tar -xzf whatsapp-bot-java-springboot.tar.gz
cd java-whatsapp-bot

# Ejecutar script de instalación automática
chmod +x install-wsl.sh
./install-wsl.sh
```

El script instalará:
- ✅ Java 17
- ✅ Maven
- ✅ Git
- ✅ Docker y Docker Compose
- ✅ PostgreSQL Client
- ✅ Herramientas adicionales

### **Paso 4: Configurar el proyecto**

```bash
# Copiar archivo de configuración
cp .env.example .env

# Editar con tus credenciales
nano .env
```

Completa:
```env
WHATSAPP_PHONE_NUMBER_ID=tu_phone_number_id
WHATSAPP_ACCESS_TOKEN=tu_access_token
WHATSAPP_VERIFY_TOKEN=inventa-un-token-secreto
```

**Guardar:** `Ctrl + O`, luego `Enter`, luego `Ctrl + X`

### **Paso 5: Ejecutar el bot**

```bash
# Iniciar PostgreSQL
docker-compose up -d

# Compilar proyecto
mvn clean install

# Ejecutar aplicación
mvn spring-boot:run
```

En otra terminal:

```bash
# Instalar y ejecutar ngrok
cd ~
wget https://bin.equinox.io/c/bNyj1mQVY4c/ngrok-v3-stable-linux-amd64.tgz
tar -xzf ngrok-v3-stable-linux-amd64.tgz
sudo mv ngrok /usr/local/bin/

# Autenticarte en ngrok.com y obtener tu token
ngrok config add-authtoken TU_TOKEN

# Exponer el servidor
ngrok http 8080
```

**Copia la URL HTTPS** que aparece y configúrala en Meta for Developers.

## ✅ Verificación

Tu bot está funcionando si ves:

```
Started WhatsAppReservationBotApplication in X seconds
```

Y ngrok muestra:
```
Forwarding  https://abc123.ngrok.io -> http://localhost:8080
```

## 🎯 Próximos pasos

1. **Configurar Google Calendar:**
   - Descargar `credentials.json` de Google Cloud Console
   - Colocar en `src/main/resources/credentials.json`
   - La primera ejecución abrirá el navegador para autorizar

2. **Configurar webhook en Meta:**
   - URL: `https://tu-ngrok.ngrok.io/webhook`
   - Verify Token: El mismo de tu `.env`

3. **Probar el bot:**
   - Enviar "Hola" por WhatsApp
   - El bot debería responder

## 🐛 Problemas Comunes

### Docker no inicia

```bash
sudo service docker start
```

### Puerto 8080 ocupado

```bash
sudo kill -9 $(sudo lsof -t -i:8080)
```

### Maven no encuentra Java

```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
source ~/.bashrc
```

## 📚 Documentación Completa

Para más detalles, consulta el `README_JAVA.md` completo.

## 🆘 Ayuda

Si tienes problemas:

1. Revisa los logs: `docker-compose logs -f`
2. Verifica versiones: `java -version`, `mvn -version`
3. Consulta el README completo

---

¡Listo! Ahora tienes un entorno completo de desarrollo en WSL. 🎉
