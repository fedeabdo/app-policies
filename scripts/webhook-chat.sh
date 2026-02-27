#!/bin/bash

# Cliente de chat interactivo para probar el webhook de WhatsApp
# Captura respuestas en tiempo real desde el endpoint del servidor

WEBHOOK_URL="http://localhost:8080/webhook"
RESPONSE_URL="http://localhost:8080/webhook/response"
LOG_FILE="/tmp/whatsapp_test_$(date +%s).log"
TEMP_DIR="/tmp/whatsapp_bot_$$"

# Crear directorio temporal
mkdir -p "$TEMP_DIR"

# Colores ANSI
RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
MAGENTA='\033[0;35m'
GRAY='\033[0;37m'
NC='\033[0m'

# Validar argumentos
if [ -z "$1" ]; then
    echo -e "${RED}❌ Uso: ./test-webhook-chat.sh <numero>${NC}"
    echo -e "   Ejemplo: ./test-webhook-chat.sh 95783047"
    rm -rf "$TEMP_DIR"
    exit 1
fi

PHONE_NUMBER="598$1"

# Verificar que Spring Boot esté corriendo
if ! curl -s "$WEBHOOK_URL/health" > /dev/null 2>&1; then
    echo -e "${RED}❌ Error: No se puede conectar a $WEBHOOK_URL${NC}"
    echo -e "${YELLOW}   ¿Spring Boot está corriendo?${NC}"
    echo -e "${YELLOW}   Ejecuta: ./run-with-logs.sh${NC}"
    rm -rf "$TEMP_DIR"
    exit 1
fi

# Limpiar pantalla
clear
echo -e "${CYAN}╔════════════════════════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║${NC}${GREEN}        🤖 SAX-BOT CHAT - Simulador de WhatsApp${NC}${CYAN}              ║${NC}"
echo -e "${CYAN}╠════════════════════════════════════════════════════════════╣${NC}"
echo -e "${CYAN}║${NC} 📱 Número: ${GREEN}$PHONE_NUMBER${NC}"
echo -e "${CYAN}║${NC} 🔗 URL: ${GREEN}$WEBHOOK_URL${NC}"
echo -e "${CYAN}║${NC}"
echo -e "${CYAN}║${NC} Escribe ${YELLOW}'salir'${NC} o ${YELLOW}'exit'${NC} para terminar"
echo -e "${CYAN}╚════════════════════════════════════════════════════════════╝${NC}"
echo ""

# Función para obtener timestamp
get_timestamp() {
    date "+${GRAY}[%H:%M:%S]${NC}"
}

# Función para obtener la respuesta del bot
get_bot_response() {
    local phone="$1"
    curl -s "$RESPONSE_URL/$phone" 2>/dev/null
}

# Función para formatear texto con saltos de línea correctos
format_response() {
    local text="$1"
    # Convertir \n en saltos reales
    echo -e "$text"
}

# Loop principal
message_count=0
bot_message_count=0

while true; do
    echo -n -e "${BLUE}📱 Tú:${NC} "
    read MESSAGE

    # Validar entrada
    if [ "$MESSAGE" = "salir" ] || [ "$MESSAGE" = "exit" ] || [ "$MESSAGE" = "quit" ]; then
        echo ""
        echo -e "${CYAN}────────────────────────────────────────────────────────────${NC}"
        echo -e "${YELLOW}👋 Desconectado${NC}"
        echo -e "${GRAY}Total de mensajes enviados: $message_count${NC}"
        echo -e "${GRAY}Log guardado en: $LOG_FILE${NC}"
        echo ""

        # Limpiar
        rm -rf "$TEMP_DIR"
        exit 0
    fi

    if [ -z "$MESSAGE" ]; then
        continue
    fi

    # Registrar en log local
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] Tú: $MESSAGE" >> "$LOG_FILE"

    # Crear payload
    PAYLOAD=$(cat <<'PAYLOAD_EOF'
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
                "wa_id": "PHONE_PLACEHOLDER"
              }
            ],
            "messages": [
              {
                "from": "PHONE_PLACEHOLDER",
                "id": "wamid.test_TIMESTAMP",
                "timestamp": "EPOCH_TIMESTAMP",
                "type": "text",
                "text": {
                  "body": "MESSAGE_PLACEHOLDER"
                }
              }
            ]
          },
          "field": "messages"
        }
      ],
      "timestamp": "EPOCH_TIMESTAMP"
    }
  ]
}
PAYLOAD_EOF
)

    # Reemplazar placeholders
    PAYLOAD="${PAYLOAD//PHONE_PLACEHOLDER/$PHONE_NUMBER}"
    PAYLOAD="${PAYLOAD//MESSAGE_PLACEHOLDER/$MESSAGE}"
    EPOCH=$(date +%s)
    PAYLOAD="${PAYLOAD//EPOCH_TIMESTAMP/$EPOCH}"
    PAYLOAD="${PAYLOAD//TIMESTAMP_/$EPOCH}"

    # Mostrar estado de envío
    echo -n -e "$(get_timestamp) ${YELLOW}📤 Enviando...${NC} "

    echo curl -s -X POST \
      -H "Content-Type: application/json" \
      -d "$PAYLOAD" \
      "$WEBHOOK_URL" 

    # Enviar petición
    RESPONSE=$(curl -s -X POST \
      -H "Content-Type: application/json" \
      -d "$PAYLOAD" \
      "$WEBHOOK_URL" 2>&1)

    CURL_EXIT=$?

    if [ $CURL_EXIT -eq 0 ]; then
        echo -e "${GREEN}✅${NC}"
    else
        echo -e "${RED}❌ Error${NC}"
        echo "[$(date '+%Y-%m-%d %H:%M:%S')] Error: $RESPONSE" >> "$LOG_FILE"
        echo ""
        continue
    fi

    ((message_count++))

    # Esperar a que se procese el mensaje y capturar respuesta
    echo -e "$(get_timestamp) ${MAGENTA}⏳ Procesando...${NC}"

    sleep 1

    # Obtener respuesta del bot
    BOT_RESPONSE=$(get_bot_response "$PHONE_NUMBER")

    if [ -n "$BOT_RESPONSE" ]; then
        echo -e "$(get_timestamp) ${GREEN}🤖 Bot:${NC}"
        echo ""
        # Formatear respuesta con colores
        echo -e "$BOT_RESPONSE" | sed 's/^/   /'
        echo ""
        ((bot_message_count++))

        # Registrar en log
        echo "[$(date '+%Y-%m-%d %H:%M:%S')] Bot: $BOT_RESPONSE" >> "$LOG_FILE"
    else
        echo -e "$(get_timestamp) ${GRAY}⏳ Sin respuesta del bot aún...${NC}"
        echo ""
    fi

done
