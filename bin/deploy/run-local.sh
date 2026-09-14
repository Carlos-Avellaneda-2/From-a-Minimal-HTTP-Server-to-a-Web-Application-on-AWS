#!/usr/bin/env bash
# Ejecuta el jar empaquetado desde la raiz del proyecto (donde vive /public).
# Uso: ./deploy/run-local.sh [puerto]
set -euo pipefail

PORT="${1:-8080}"
JAR="target/lab2-http-server.jar"

if [ ! -f "$JAR" ]; then
  echo "No se encontro $JAR. Corre primero: mvn clean package"
  exit 1
fi

PORT="$PORT" PUBLIC_DIR="public" java -jar "$JAR"
