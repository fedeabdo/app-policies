#!/bin/bash

# Script para probar el webhook de forma local
# Simula un mensaje de WhatsApp llegando al bot

# Cambiar estos valores según sea necesario
WEBHOOK_URL="http://localhost:8080/webhook"
PHONE_NUMBER="598"$1  # Tu número de prueba (formato: codigopais + numero, sin espacios ni +)
MESSAGE=$2

# IMPORTANTE: El formato de phone_number debe ser:
# - Código de país + número (sin + ni espacios)
# - Ejemplo para Uruguay: 598 + 9XXXXXXXX = 59892XXXXXXX

# Payload JSON que simula un mensaje de WhatsApp
PAYLOAD=$(cat <<EOF
{
  "object": "whatsapp_business_account",
  "entry": [
    {
      "id": "XXXXXXXXXXXXXXX",
      "changes": [
        {
          "value": {
            "messaging_product": "whatsapp",
            "metadata": {
              "display_phone_number": "12345678",
              "phone_number_id": "123456789012345"
            },
            "contacts": [
              {
                "profile": {
                  "name": "Test User"
                },
                "wa_id": "$PHONE_NUMBER"
              }
            ],
            "messages": [
              {
                "from": "$PHONE_NUMBER",
                "id": "wamid.test123456",
                "timestamp": "$(date +%s)",
                "type": "text",
                "text": {
                  "body": "$MESSAGE"
                }
              }
            ]
          },
          "field": "messages"
        }
      ],
      "timestamp": "$(date +%s)"
    }
  ]
}
EOF
)

echo "📤 Enviando mensaje de prueba al webhook..."
echo "URL: $WEBHOOK_URL"
echo "Mensaje: $MESSAGE"
echo "Desde: $PHONE_NUMBER"
echo ""

# Enviar la petición
curl -X POST \
  -H "Content-Type: application/json" \
  -d "$PAYLOAD" \
  "$WEBHOOK_URL"

echo ""
echo "✅ Petición enviada. Revisa los logs de Spring Boot."
