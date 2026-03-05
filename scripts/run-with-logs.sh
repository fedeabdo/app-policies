#!/bin/bash

# ==============================
# Script para ejecutar Spring Boot
# y capturar logs
# ==============================

# Obtener directorio del script
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Ir al root del proyecto (1 nivel arriba de scripts/)
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

LOGS_DIR="$PROJECT_ROOT/logs"
LOGS_FILE="$LOGS_DIR/app.log"

# Crear directorio de logs si no existe
mkdir -p "$LOGS_DIR"

# Limpiar log anterior
> "$LOGS_FILE"

echo "🚀 Iniciando Spring Boot y capturando logs..."
echo "📁 Root del proyecto: $PROJECT_ROOT"
echo "📝 Logs en: $LOGS_FILE"
echo ""
echo "⏳ Abre otra terminal y ejecuta:"
echo "   ./scripts/webhook-chat.sh 95783047"
echo ""

# Ir al root del proyecto y ejecutar Spring Boot
cd "$PROJECT_ROOT" || exit

mvn spring-boot:run 2>&1 | tee "$LOGS_FILE"