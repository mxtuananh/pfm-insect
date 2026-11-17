#!/usr/bin/env bash
# Build and run the pfm backend for local development. Never writes into ../pfm.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"; S="$ROOT/.server"; PFM="$ROOT/../pfm"
export PATH="$PATH:$LOCALAPPDATA/Android/Sdk/platform-tools"
mkdir -p "$S"
if [ ! -f "$S/local.env" ]; then
  printf 'DB_PASSWORD=%s\nJWT_SECRET=%s\n' \
    "$(head -c 24 /dev/urandom | base64 | tr -dc 'A-Za-z0-9' | head -c 20)" \
    "$(head -c 48 /dev/urandom | base64 | tr -dc 'A-Za-z0-9' | head -c 48)" > "$S/local.env"
fi
set -a; . "$S/local.env"; set +a
if [ "${1:-}" = "--rebuild" ] || [ ! -d "$S/src/build/install/pfm" ]; then
  rm -rf "$S/src"; mkdir -p "$S/src"
  git -C "$PFM" archive HEAD | tar -x -C "$S/src"
  (cd "$S/src" && ./gradlew --no-daemon --offline installDist -x test)
fi
docker start pfm-insect-db >/dev/null 2>&1 || docker run -d --name pfm-insect-db -e POSTGRES_DB=pfm -e POSTGRES_USER=pfm \
  -e POSTGRES_PASSWORD="$DB_PASSWORD" -e POSTGRES_INITDB_ARGS=--encoding=UTF8 \
  -p 127.0.0.1:15433:5432 -v pfm-insect-db:/var/lib/postgresql postgres:18 >/dev/null
sleep 5
export DB_URL=r2dbc:postgresql://localhost:15433/pfm DB_USER=pfm JWT_ISSUER=http://localhost:8080/ \
  JWT_AUDIENCE=pfm-users JWT_REALM=pfm OPENAPI_ENABLED=true APP_VERSION=dev
CP="$S/src/build/install/pfm/lib/*"; command -v cygpath >/dev/null && CP="$(cygpath -w "$S/src/build/install/pfm/lib")\*"
nohup java -Duser.timezone=UTC -cp "$CP" io.ktor.server.netty.EngineMain > "$S/server.log" 2>&1 &
sleep 20; adb reverse tcp:8080 tcp:8080 || true
curl -s localhost:8080/health; echo
