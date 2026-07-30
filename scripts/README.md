# NexU shutdown helpers

- `nexu-force-stop.bat [port] [config-file]` for Windows.
- `nexu-force-stop.sh [port] [config-file]` for Linux.

Both helpers resolve the port from the command line, `NEXU_PORT`, or `binding_ports` in `nexu-config.properties`, falling back to `9795`. They verify `http://127.0.0.1:<port>/nexu-info` contains a NexU version before resolving and stopping the listening process.

On Linux, make the script executable once with `chmod +x nexu-force-stop.sh`, or run it with `sh nexu-force-stop.sh`.
