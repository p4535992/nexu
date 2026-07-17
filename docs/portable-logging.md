# Portable logging contract

The Windows and Linux portable archives contain a `.nexu-portable` marker at
the application-image root. NexU uses this marker only to select the default
log directory:

- Windows: `NexU\logs\nexu.log`, beside `NexU.exe`;
- Linux: `NexU/logs/nexu.log`, at the application-image root.

The marker is removed before building EXE, MSI, DEB and RPM installers, so
installed packages continue to use the operator data directory. Explicit log
directory configuration always takes precedence.
