# 🐛 Solución de Problemas

## Error: "Could not find credentials.json"

```bash
# Asegúrate de que el archivo esté en:
src/main/resources/credentials.json
```

## Error: "Connection refused" PostgreSQL

```bash
# Verifica que PostgreSQL esté corriendo
docker-compose ps

# O si es local:
sudo service postgresql status
```

## Webhook no recibe mensajes

1. Verifica que la URL sea HTTPS
2. Confirma que el verify token coincida en Meta y en `.env`
3. Revisa logs: `tail -f logs/spring.log`
4. Prueba el endpoint:
   ```bash
   curl https://tu-url-ngrok.ngrok.io/webhook/health
   ```

## Webhook no valida (Error de verificación)

Si Meta dice "No se pudo validar la URL de devolución de llamada":

1. Verifica que ngrok esté corriendo: `ngrok http 8080`
2. Verifica que Spring Boot esté corriendo: `mvn spring-boot:run`
3. Asegúrate de que el Verify Token en Meta sea **EXACTAMENTE** igual al de `application.properties`
4. Prueba el endpoint directamente:
   ```bash
   curl "https://tu-url-ngrok.ngrok.io/webhook?hub.mode=subscribe&hub.challenge=test&hub.verify_token=tu_token"
   ```

## El número no tiene WhatsApp

Si Meta dice que el número no tiene WhatsApp:

1. Ve a **WhatsApp → Getting Started → Phone Numbers**
2. Click en "Add phone number"
3. Ingresa tu número real (con código de país: +598xxxxxxxxx)
4. Completa la verificación (recibirás código por SMS o WhatsApp)
5. Una vez verificado, ese número será tu "Phone Number ID"
6. Agrega ese mismo número como "Test Recipient"

## Error 401 Unauthorized

El access token es inválido o expirado:

1. Regenera el token en Meta:
   - Meta for Developers → Tu app
   - Business Settings → System Users
   - Genera un nuevo token con permisos `whatsapp_business_messaging` y `whatsapp_business_management`
2. Actualiza tu `.env`:
   ```bash
   WHATSAPP_ACCESS_TOKEN=nuevo_token_aqui
   ```
3. Reinicia Spring Boot

## Error 400 Bad Request

Problema con el formato de la solicitud. Causas comunes:

1. **Formato de número incorrecto**:
   - Debe ser: `codigopais + numero` (sin espacios ni +)
   - Ejemplo Uruguay: `5598123456`

2. **PHONE_NUMBER_ID inválido**:
   - Debe ser un número largo (15+ dígitos)
   - NO es un número de WhatsApp real

3. **Revisa los logs** para ver exactamente qué está enviando

## Bot no responde

1. Revisa los logs de la aplicación
2. Verifica que el Access Token sea válido
3. Confirma que tu número esté en la lista de testers
4. Asegúrate de que el webhook esté suscrito a "messages"
5. Las apps sin publicar **solo reciben webhooks de prueba** desde el panel de Meta

## Error de Google Calendar

```bash
# Elimina el directorio de tokens y re-autoriza
rm -rf tokens/
mvn spring-boot:run
```

## Problema: App sin publicar no recibe mensajes

**Limitación de Meta**: Las apps de Meta solo pueden recibir webhooks de prueba mientras estén sin publicar.

**Soluciones**:

- **Desarrollo**: Usa el panel de Meta para enviar webhooks de prueba
- **Producción**: Publica la app en Meta (ve a App Review y solicita permisos)

## Necesito más ayuda

Revisa:
- [WhatsApp Business API Docs](https://developers.facebook.com/docs/whatsapp)
- [Google Calendar API Java](https://developers.google.com/calendar/api/quickstart/java)
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
