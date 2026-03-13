#!/bin/bash
# Script para probar el sistema de recordatorios

# Si el primer arg empieza con http, es la URL; sino es el comando
if [[ "$1" == http* ]]; then
    BASE_URL="$1"
    CMD="${2:-help}"
    ARG="$3"
else
    BASE_URL="http://localhost:8080"
    CMD="${1:-help}"
    ARG="$2"
fi

echo "=== Test de Recordatorios ==="
echo "URL Base: $BASE_URL"
echo ""

case "$CMD" in
    config)
        echo "📋 Configuración actual de recordatorios:"
        curl -s "$BASE_URL/api/test/reminders/config" | jq .
        ;;
    
    reservations)
        echo "📅 Reservas confirmadas:"
        curl -s "$BASE_URL/api/test/reminders/reservations" | jq .
        ;;
    
    send)
        if [ -z "$ARG" ]; then
            echo "❌ Uso: $0 [url] send <reservation_id>"
            echo "   Ejemplo: $0 send 1"
            exit 1
        fi
        echo "📤 Enviando recordatorio para reserva $ARG..."
        curl -s -X POST "$BASE_URL/api/test/reminders/send/$ARG" | jq .
        ;;
    
    process)
        echo "⚙️ Ejecutando proceso de recordatorios..."
        curl -s -X POST "$BASE_URL/api/test/reminders/process" | jq .
        ;;
    
    history)
        if [ -z "$ARG" ]; then
            echo "❌ Uso: $0 [url] history <reservation_id>"
            exit 1
        fi
        echo "📜 Historial de recordatorios para reserva $ARG:"
        curl -s "$BASE_URL/api/test/reminders/history/$ARG" | jq .
        ;;
    
    *)
        echo "Uso: $0 [url] <comando> [args]"
        echo ""
        echo "Comandos disponibles:"
        echo "  config        - Ver configuración de recordatorios"
        echo "  reservations  - Listar reservas confirmadas"
        echo "  send <id>     - Enviar recordatorio manual a una reserva"
        echo "  process       - Ejecutar proceso de recordatorios"
        echo "  history <id>  - Ver historial de recordatorios de una reserva"
        echo ""
        echo "Ejemplos:"
        echo "  $0 config"
        echo "  $0 http://localhost:8080 reservations"
        echo "  $0 send 1"
        echo "  $0 history 1"
        ;;
esac
