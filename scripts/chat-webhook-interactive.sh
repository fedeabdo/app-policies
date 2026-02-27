#!/bin/bash

# Script interactivo para probar el webhook de WhatsApp
# Se itera en un loop esperando mensajes del usuario

WEBHOOK_URL="http://localhost:8080/webhook"
PHONE_NUMBER="598"$1  # Tu número de telefono

if [ -z "$1" ]; then
    echo "❌ Uso: ./test-webhook-interactive.sh <numero>"
    echo "   Ejemplo: ./test-webhook-interactive.sh 95783047"
    exit 1
fi

PHONE_NUMBER="598$1"
echo "✅ Conectado. Números de teléfono: $PHONE_NUMBER"
echo "📝 Escribe 'salir' para terminar"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

while true; do
    read -p "📱 Mensaje: " MESSAGE

    if [ "$MESSAGE" = "salir" ] || [ "$MESSAGE" = "exit" ]; then
        echo "👋 Desconectado"
        break
    fi

    if [ -z "$MESSAGE" ]; then
        echo "⚠️  Mensaje vacío, intenta de nuevo"
        continue
    fi

    # Crear payload
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
                "id": "wamid.test$(date +%s%N)",
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

    # Enviar
    echo -n "📤 Enviando... "
    RESPONSE=$(curl -s -X POST \
      -H "Content-Type: application/json" \
      -d "$PAYLOAD" \
      "$WEBHOOK_URL")

    if [ $? -eq 0 ]; then
        echo "✅"
    else
        echo "❌ Error en la petición"
    fi
    echo ""
done
