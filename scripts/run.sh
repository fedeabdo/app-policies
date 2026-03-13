#!/bin/bash

# ==============================
# Script principal para ejecutar
# la aplicación WhatsApp Bot
# ==============================

set -e

# Colores
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Obtener directorio del script y root del proyecto
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# Variables
CONTAINER_NAME="whatsapp-bot-postgres"
LOGS_DIR="$PROJECT_ROOT/logs"
LOGS_FILE="$LOGS_DIR/app.log"
BUILD=false

# ==============================
# Funciones
# ==============================

show_help() {
    echo ""
    echo -e "${BLUE}Uso:${NC} ./scripts/run.sh [opciones]"
    echo ""
    echo "Opciones:"
    echo "  -b, --build     Compilar la aplicación antes de ejecutar"
    echo "  -h, --help      Mostrar esta ayuda"
    echo ""
    echo "Ejemplos:"
    echo "  ./scripts/run.sh           # Solo ejecutar"
    echo "  ./scripts/run.sh -b        # Compilar y ejecutar"
    echo "  ./scripts/run.sh --build   # Compilar y ejecutar"
    echo ""
}

log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[OK]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

check_docker() {
    if ! command -v docker &> /dev/null; then
        log_error "Docker no está instalado"
        exit 1
    fi
    
    if ! docker info &> /dev/null; then
        log_error "Docker no está corriendo. Inicialo con: sudo service docker start"
        exit 1
    fi
}

start_database() {
    log_info "Verificando base de datos..."
    
    # Verificar si el contenedor existe y está corriendo
    if docker ps --format '{{.Names}}' | grep -q "^${CONTAINER_NAME}$"; then
        log_success "PostgreSQL ya está corriendo"
        return 0
    fi
    
    # Verificar si existe pero está detenido
    if docker ps -a --format '{{.Names}}' | grep -q "^${CONTAINER_NAME}$"; then
        log_info "Iniciando contenedor PostgreSQL existente..."
        docker start "$CONTAINER_NAME"
    else
        log_info "Creando y levantando PostgreSQL con docker-compose..."
        cd "$PROJECT_ROOT"
        docker-compose up -d postgres
    fi
    
    # Esperar a que PostgreSQL esté listo
    log_info "Esperando a que PostgreSQL esté listo..."
    local max_attempts=30
    local attempt=0
    
    while [ $attempt -lt $max_attempts ]; do
        if docker exec "$CONTAINER_NAME" pg_isready -U postgres &> /dev/null; then
            log_success "PostgreSQL está listo"
            return 0
        fi
        attempt=$((attempt + 1))
        sleep 1
    done
    
    log_error "PostgreSQL no respondió a tiempo"
    exit 1
}

build_app() {
    log_info "Compilando aplicación..."
    cd "$PROJECT_ROOT"
    
    if mvn clean package -DskipTests; then
        log_success "Compilación exitosa"
    else
        log_error "Error en la compilación"
        exit 1
    fi
}

run_app() {
    # Crear directorio de logs
    mkdir -p "$LOGS_DIR"
    > "$LOGS_FILE"
    
    echo ""
    echo -e "${GREEN}==============================${NC}"
    echo -e "${GREEN}  WhatsApp Reservation Bot${NC}"
    echo -e "${GREEN}==============================${NC}"
    echo ""
    log_info "Root del proyecto: $PROJECT_ROOT"
    log_info "Logs en: $LOGS_FILE"
    echo ""
    echo -e "${YELLOW}Tip: Abre otra terminal y ejecuta:${NC}"
    echo "   ./scripts/webhook-chat.sh 95783047"
    echo ""
    echo -e "${BLUE}Iniciando Spring Boot...${NC}"
    echo ""
    
    cd "$PROJECT_ROOT"
    mvn spring-boot:run 2>&1 | tee "$LOGS_FILE"
}

# ==============================
# Parseo de argumentos
# ==============================

while [[ $# -gt 0 ]]; do
    case $1 in
        -b|--build)
            BUILD=true
            shift
            ;;
        -h|--help)
            show_help
            exit 0
            ;;
        *)
            log_error "Opción desconocida: $1"
            show_help
            exit 1
            ;;
    esac
done

# ==============================
# Ejecución principal
# ==============================

echo ""
echo -e "${BLUE}🚀 Iniciando WhatsApp Reservation Bot...${NC}"
echo ""

# 1. Verificar Docker
check_docker

# 2. Iniciar base de datos si no está corriendo
start_database

# 3. Compilar si se solicitó
if [ "$BUILD" = true ]; then
    echo ""
    build_app
fi

# 4. Ejecutar aplicación
echo ""
run_app
