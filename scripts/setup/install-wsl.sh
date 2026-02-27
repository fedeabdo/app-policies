#!/bin/bash

# Script de instalación automática para WhatsApp Bot en WSL/Linux
# Autor: Sistema de Reservas WhatsApp
# Versión: 1.0

set -e  # Salir si hay algún error

echo "================================================"
echo "  Instalación de WhatsApp Reservation Bot"
echo "  para WSL/Linux"
echo "================================================"
echo ""

# Colores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Función para imprimir en verde
print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

# Función para imprimir en amarillo
print_info() {
    echo -e "${YELLOW}➜ $1${NC}"
}

# Función para imprimir en rojo
print_error() {
    echo -e "${RED}✗ $1${NC}"
}

# Verificar si estamos en WSL
if grep -qi microsoft /proc/version; then
    print_info "Detectado: WSL (Windows Subsystem for Linux)"
else
    print_info "Detectado: Linux nativo"
fi

echo ""
print_info "Este script instalará:"
echo "  - Java 17 (OpenJDK)"
echo "  - Maven"
echo "  - Git"
echo "  - Docker y Docker Compose"
echo "  - PostgreSQL Client"
echo "  - Herramientas adicionales"
echo ""

read -p "¿Deseas continuar? (s/n): " -n 1 -r
echo ""
if [[ ! $REPLY =~ ^[Ss]$ ]]; then
    print_error "Instalación cancelada"
    exit 1
fi

echo ""
print_info "Paso 1/7: Actualizando sistema..."
sudo apt update && sudo apt upgrade -y
print_success "Sistema actualizado"

echo ""
print_info "Paso 2/7: Instalando Java 17..."
if command -v java &> /dev/null; then
    JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2)
    print_info "Java ya está instalado: $JAVA_VERSION"
else
    sudo apt install openjdk-17-jdk -y
    print_success "Java 17 instalado"
fi

# Configurar JAVA_HOME
if ! grep -q "JAVA_HOME" ~/.bashrc; then
    echo 'export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64' >> ~/.bashrc
    echo 'export PATH=$JAVA_HOME/bin:$PATH' >> ~/.bashrc
    print_success "JAVA_HOME configurado en .bashrc"
fi

echo ""
print_info "Paso 3/7: Instalando Maven..."
if command -v mvn &> /dev/null; then
    MVN_VERSION=$(mvn -version | head -n 1)
    print_info "Maven ya está instalado: $MVN_VERSION"
else
    sudo apt install maven -y
    print_success "Maven instalado"
fi

echo ""
print_info "Paso 4/7: Instalando Git..."
if command -v git &> /dev/null; then
    GIT_VERSION=$(git --version)
    print_info "Git ya está instalado: $GIT_VERSION"
else
    sudo apt install git -y
    print_success "Git instalado"
fi

echo ""
print_info "Paso 5/7: Instalando Docker..."
if command -v docker &> /dev/null; then
    DOCKER_VERSION=$(docker --version)
    print_info "Docker ya está instalado: $DOCKER_VERSION"
else
    curl -fsSL https://get.docker.com -o get-docker.sh
    sudo sh get-docker.sh
    rm get-docker.sh
    
    # Agregar usuario al grupo docker
    sudo usermod -aG docker $USER
    print_success "Docker instalado"
    print_info "IMPORTANTE: Cierra y vuelve a abrir WSL para usar Docker sin sudo"
fi

echo ""
print_info "Paso 6/7: Instalando Docker Compose..."
if command -v docker-compose &> /dev/null; then
    COMPOSE_VERSION=$(docker-compose --version)
    print_info "Docker Compose ya está instalado: $COMPOSE_VERSION"
else
    sudo apt install docker-compose -y
    print_success "Docker Compose instalado"
fi

# Configurar Docker para iniciar automáticamente en WSL
if grep -qi microsoft /proc/version; then
    if ! grep -q "service docker start" ~/.bashrc; then
        echo '# Iniciar Docker automáticamente en WSL' >> ~/.bashrc
        echo 'if ! service docker status > /dev/null 2>&1; then' >> ~/.bashrc
        echo '    sudo service docker start > /dev/null 2>&1' >> ~/.bashrc
        echo 'fi' >> ~/.bashrc
        print_success "Docker configurado para iniciar automáticamente"
    fi
fi

echo ""
print_info "Paso 7/7: Instalando herramientas adicionales..."
sudo apt install -y \
    curl \
    wget \
    unzip \
    postgresql-client \
    net-tools \
    iputils-ping

print_success "Herramientas adicionales instaladas"

echo ""
echo "================================================"
print_success "¡Instalación completada!"
echo "================================================"
echo ""

# Mostrar versiones instaladas
echo "Versiones instaladas:"
echo "  Java:    $(java -version 2>&1 | head -n 1)"
echo "  Maven:   $(mvn -version 2>&1 | head -n 1)"
echo "  Git:     $(git --version)"
echo "  Docker:  $(docker --version 2>&1 || echo 'Reinicia WSL para usar')"
echo ""

print_info "Próximos pasos:"
echo ""
echo "1. Si instalaste Docker por primera vez, cierra y vuelve a abrir WSL:"
echo "   exit"
echo "   wsl"
echo ""
echo "2. Navega al directorio del proyecto:"
echo "   cd ~/projects/java-whatsapp-bot"
echo ""
echo "3. Configura las variables de entorno:"
echo "   cp .env.example .env"
echo "   nano .env"
echo ""
echo "4. Descarga credentials.json de Google Cloud y colócalo en:"
echo "   src/main/resources/credentials.json"
echo ""
echo "5. Inicia PostgreSQL:"
echo "   docker-compose up -d"
echo ""
echo "6. Compila el proyecto:"
echo "   mvn clean install"
echo ""
echo "7. Ejecuta la aplicación:"
echo "   mvn spring-boot:run"
echo ""

print_success "¡Listo para desarrollar! 🚀"
