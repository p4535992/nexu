# NexU operator guide

NexU is distributed as a desktop application. Operators do not need to install
Java separately because native packages contain a private runtime.

## Windows

Preferred package: `NexU-<version>.exe`.

1. Run the installer as the operator who will use the smart card.
2. Keep the default per-user installation unless local policy requires another
   directory.
3. Launch **NexU** from the Start menu or desktop shortcut.
4. Insert the smart card and open the signing web application.

A portable ZIP is also produced. Extract it to a writable directory and launch
`NexU.exe` from the extracted `NexU` directory.

Windows uses the built-in smart-card service and certificate store first. A
vendor minidriver or PKCS#11 middleware is needed only when the card is not
available through the Windows stack.

## Debian and Ubuntu Linux

Preferred package: `nexu_<version>_<architecture>.deb`.

Install it with the desktop software manager or:

```bash
sudo apt install ./nexu_<version>_<architecture>.deb
```

The Java runtime is included. The operating-system smart-card stack remains a
system dependency. Install it when missing:

```bash
sudo apt install pcscd libpcsclite1 libccid opensc
```

Start NexU from the application menu. A portable `tar.gz` is also produced;
extract it and run:

```bash
./NexU/bin/NexU
```

The portable archive still requires a functioning PC/SC service and reader
permissions.

## What is included

- NexU desktop launcher;
- Java 17 private runtime;
- JavaFX 21 desktop runtime for the target operating system;
- Spring Boot local loopback service;
- DSS signing support;
- PKCS#11 and Windows certificate-store adapters;
- editable `nexu-config.properties`;
- project and third-party licence notices.

## What is not installed silently

NexU does not silently install reader drivers, vendor minidrivers or proprietary
PKCS#11 middleware. These components change trusted operating-system software
and may require administrator approval.

The application should diagnose the missing layer and provide official vendor
or operating-system guidance. Future one-click installation is allowed only for
curated packages with explicit consent, HTTPS allowlisting, SHA-256 verification
and publisher-signature verification.

## Configuration

The packaged application looks for `nexu-config.properties` next to the native
launcher/application image. Administrators can also set:

```text
-Dnexu.config.file=/absolute/path/nexu-config.properties
```

or the environment variable:

```text
NEXU_CONFIG_FILE=/absolute/path/nexu-config.properties
```

For browser integrations, replace the legacy wildcard with the real web origin:

```properties
cors_allowed_origin=https://sign.example.org
```

The local service must remain bound to loopback addresses.

## Diagnostic logs

NexU uses the Logback implementation supplied by Spring Boot and creates a
rotating DEBUG log named `nexu.log`.

For portable packages, logs remain with the extracted application:

```text
Windows portable: <extracted directory>\NexU\logs\nexu.log
Linux portable:   <extracted directory>/NexU/logs/nexu.log
```

On Windows the `logs` directory is therefore at the same level as `NexU.exe`.
Move or copy the whole `NexU` directory when relocating the portable package.

Installed packages and direct JAR execution use the operator data directory:

```text
Windows installed: %USERPROFILE%\.nexu\logs\nexu.log
Linux installed:   $HOME/.nexu/logs/nexu.log
```

Explicit `NEXU_LOG_DIR`, `-Dnexu.log.dir=...` or `log_directory=...` settings
override these defaults. See the `LOGS.txt` file included in portable packages
for rotation and support details.

## Troubleshooting checklist

1. Confirm that NexU is running.
2. Open `logs/nexu.log` in the portable `NexU` directory, or the user-data log
   for an installed package.
3. Confirm that the reader is visible to the operating system.
4. Confirm that the PC/SC service is running.
5. Confirm that the card is visible in the Windows certificate store or through
   the configured PKCS#11 provider.
6. Confirm that the signing website origin is present in the NexU allowlist.
7. Do not include PINs, certificate private data, document hashes or signatures
   in support logs or tickets.
