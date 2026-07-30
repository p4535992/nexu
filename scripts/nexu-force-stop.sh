#!/usr/bin/env sh
set -eu

# Usage:
#   ./nexu-force-stop.sh [port] [path-to-nexu-config.properties]
# Resolution order: port argument, NEXU_PORT, configuration file, default 9795.

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
EXPLICIT_PORT=${1:-}
EXPLICIT_CONFIG=${2:-}

read_port_from_config() {
    config_file=$1
    [ -f "$config_file" ] || return 1

    awk -F= '
        /^[[:space:]]*binding_ports[[:space:]]*=/ {
            value=$2
            gsub(/[[:space:]]/, "", value)
            split(value, ports, /[,;]/)
            if (ports[1] ~ /^[0-9]+$/) {
                print ports[1]
                exit 0
            }
        }
    ' "$config_file"
}

resolve_port() {
    if [ -n "$EXPLICIT_PORT" ]; then
        printf '%s\n' "$EXPLICIT_PORT"
        return
    fi

    if [ -n "${NEXU_PORT:-}" ]; then
        printf '%s\n' "$NEXU_PORT"
        return
    fi

    candidates=""
    if [ -n "$EXPLICIT_CONFIG" ]; then
        candidates=$EXPLICIT_CONFIG
    fi
    candidates="${candidates}
${SCRIPT_DIR}/nexu-config.properties
${SCRIPT_DIR}/app/nexu-config.properties
${HOME:-}/.NexU/nexu-config.properties"

    printf '%s\n' "$candidates" | while IFS= read -r candidate; do
        [ -n "$candidate" ] || continue
        configured_port=$(read_port_from_config "$candidate" 2>/dev/null || true)
        if [ -n "$configured_port" ]; then
            printf '%s\n' "$configured_port"
            exit 0
        fi
    done
}

PORT=$(resolve_port)
PORT=${PORT:-9795}

case "$PORT" in
    ''|*[!0-9]*)
        echo "Invalid NexU port: $PORT" >&2
        exit 2
        ;;
esac

if [ "$PORT" -lt 1 ] || [ "$PORT" -gt 65535 ]; then
    echo "Invalid NexU port: $PORT" >&2
    exit 2
fi

URL="http://127.0.0.1:${PORT}/nexu-info"
echo "Checking NexU endpoint $URL"

if command -v curl >/dev/null 2>&1; then
    if ! NEXU_INFO=$(curl --fail --silent --show-error --max-time 3 "$URL" 2>/dev/null); then
        echo "No NexU instance is responding on port $PORT; nothing to stop."
        exit 0
    fi
elif command -v wget >/dev/null 2>&1; then
    if ! NEXU_INFO=$(wget -q -T 3 -O - "$URL" 2>/dev/null); then
        echo "No NexU instance is responding on port $PORT; nothing to stop."
        exit 0
    fi
else
    echo "curl or wget is required to verify /nexu-info." >&2
    exit 3
fi

if ! printf '%s' "$NEXU_INFO" | grep -Eq '"version"[[:space:]]*:[[:space:]]*"[^"]+"'; then
    echo "Port $PORT answered, but /nexu-info did not contain a NexU version. Refusing to stop any process." >&2
    exit 4
fi

listening_pids() {
    {
        if command -v lsof >/dev/null 2>&1; then
            lsof -nP -t -iTCP:"$PORT" -sTCP:LISTEN 2>/dev/null || true
        fi

        if command -v ss >/dev/null 2>&1; then
            ss -ltnp "sport = :$PORT" 2>/dev/null \
                | sed -n 's/.*pid=\([0-9][0-9]*\).*/\1/p' || true
        fi

        if command -v fuser >/dev/null 2>&1; then
            fuser -n tcp "$PORT" 2>/dev/null \
                | tr ' ' '\n' \
                | sed -n '/^[0-9][0-9]*$/p' || true
        fi
    } | awk '!seen[$0]++ && $0 ~ /^[0-9]+$/'
}

PIDS=$(listening_pids)
if [ -z "$PIDS" ]; then
    echo "NexU answered on port $PORT, but the listening PID could not be resolved." >&2
    echo "Install lsof/iproute2, or run the script with sufficient privileges." >&2
    exit 5
fi

echo "Verified NexU endpoint; requesting shutdown for PID(s): $(printf '%s' "$PIDS" | tr '\n' ' ')"

for process_id in $PIDS; do
    if [ "$process_id" = "$$" ]; then
        echo "Refusing to terminate the shutdown helper process itself." >&2
        exit 6
    fi
    kill -TERM "$process_id" 2>/dev/null || {
        echo "Unable to terminate PID $process_id. Try running with sudo." >&2
        exit 7
    }
done

# Give NexU a brief opportunity to release resources, then force any survivor.
attempt=0
while [ "$attempt" -lt 4 ]; do
    survivors=""
    for process_id in $PIDS; do
        if kill -0 "$process_id" 2>/dev/null; then
            survivors="${survivors} ${process_id}"
        fi
    done
    [ -z "$survivors" ] && break
    sleep 1
    attempt=$((attempt + 1))
done

for process_id in $PIDS; do
    if kill -0 "$process_id" 2>/dev/null; then
        echo "PID $process_id did not stop gracefully; forcing SIGKILL."
        kill -KILL "$process_id" 2>/dev/null || {
            echo "Unable to force-stop PID $process_id. Try running with sudo." >&2
            exit 8
        }
    fi
done

attempt=0
while [ "$attempt" -lt 10 ]; do
    if [ -z "$(listening_pids)" ]; then
        echo "NexU stopped successfully; port $PORT is free."
        exit 0
    fi
    sleep 1
    attempt=$((attempt + 1))
done

echo "NexU processes were signalled, but port $PORT is still in use." >&2
exit 9
