# 🚀 Pasar a Producción

## 1. Obtener número de WhatsApp Business oficial

Para usar con cualquier usuario (no solo testers):

1. En Meta Business Suite, solicita verificación de negocio
2. Una vez aprobado, compra un número de WhatsApp dedicado
3. Completa el proceso de aprobación de Meta

## 2. Usar base de datos en la nube

Recomendaciones:
- **AWS RDS PostgreSQL**
- **Google Cloud SQL**
- **Azure Database for PostgreSQL**
- **Heroku Postgres**

Actualiza `SPRING_DATASOURCE_URL` con la URL de tu base de datos.

## 3. Desplegar aplicación

### Heroku

```bash
heroku create mi-bot-whatsapp
heroku addons:create heroku-postgresql:mini
git push heroku main
```

### Railway

- Conecta tu repositorio de GitHub
- Railway detectará automáticamente Spring Boot
- Agrega PostgreSQL desde el dashboard

### Docker

```bash
mvn clean package
docker build -t whatsapp-bot .
docker run -p 8080:8080 whatsapp-bot
```

## 4. Configurar dominio y HTTPS

WhatsApp requiere HTTPS. Usa:
- Certificado Let's Encrypt (gratuito)
- Certificado de tu proveedor cloud
- Cloudflare (proxy gratuito con SSL)

Actualiza tu webhook URL en Meta a tu dominio real.

## 5. Variables de entorno en producción

**Nunca** pongas credenciales en `application.properties`. Usa variables de entorno:

```bash
export WHATSAPP_PHONE_NUMBER_ID=tu_phone_id
export WHATSAPP_ACCESS_TOKEN=tu_token
export WHATSAPP_VERIFY_TOKEN=tu_verify_token
export SPRING_DATASOURCE_URL=tu_db_url
export SPRING_DATASOURCE_USERNAME=tu_usuario
export SPRING_DATASOURCE_PASSWORD=tu_password
export GOOGLE_CALENDAR_CREDENTIALS_FILE=/path/to/credentials.json
```

## 6. Publicar app en Meta

1. Ve a tu app → App Review
2. Agrega los siguientes permisos:
   - `whatsapp_business_messaging`
   - `whatsapp_business_management`
3. Completa el formulario de revisión
4. Una vez aprobada, podrás recibir mensajes de usuarios reales

## 🔒 Checklist de Seguridad

- ✅ Nunca subas `.env`, `credentials.json`, `tokens/` a Git
- ✅ Usa variables de entorno para todas las credenciales
- ✅ HTTPS obligatorio en producción
- ✅ Rota el access token periódicamente
- ✅ Implementa rate limiting
- ✅ Valida y sanitiza todos los inputs del usuario
- ✅ Usa firewall para restringir acceso a la BD
- ✅ Configura backups automáticos de la BD
- ✅ Monitorea logs y errores
- ✅ Implementa alertas para fallos críticos

## 📊 Monitoreo

Monitor tu aplicación con:

```bash
# Ver logs en tiempo real
tail -f logs/spring.log

# Ver estado de la app
curl https://tu-dominio.com/webhook/health

# Métricas de la BD
SELECT COUNT(*) FROM reservations;
SELECT COUNT(*) FROM user_sessions;
```

## 💡 Tips de Producción

1. **Escala automática**: Configura auto-scaling en tu proveedor cloud
2. **CDN**: Usa CDN para servir assets estáticos
3. **Caché**: Implementa Redis para caché de sesiones
4. **Email**: Agrega confirmaciones por email a las reservas
5. **SMS**: Considerá SMS para recordatorios
