# NexU shutdown helpers

- `nexu-force-stop.bat [port] [config-file]` for Windows.
- `nexu-force-stop.sh [port] [config-file]` for Linux.

Both helpers resolve the port from the command line, `NEXU_PORT`, or `binding_ports` in `nexu-config.properties`, falling back to `9795`. They verify `http://127.0.0.1:<port>/nexu-info` contains a NexU version before resolving and stopping the listening process.

The packaging scripts place the platform-specific helper in the root of every NexU application image:

- the Windows portable ZIP and EXE installer contain `nexu-force-stop.bat` beside `NexU.exe`;
- the Linux portable archive and Debian package contain executable `nexu-force-stop.sh` in the installed NexU application directory.

Examples:

```bat
nexu-force-stop.bat
nexu-force-stop.bat 9795
nexu-force-stop.bat "" "C:\path\to\nexu-config.properties"
```

```sh
./nexu-force-stop.sh
./nexu-force-stop.sh 9795
./nexu-force-stop.sh "" /path/to/nexu-config.properties
```

The Linux package preserves executable mode `0755`. When running the source-tree copy directly, use `chmod +x scripts/nexu-force-stop.sh` once or invoke it with `sh scripts/nexu-force-stop.sh`.
