#!/bin/bash

# ==============================
# Script legacy - usa run.sh
# ==============================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Ejecutar run.sh con los mismos argumentos
exec "$SCRIPT_DIR/run.sh" "$@"