# NexU

NexU is a local signing agent that lets web applications request certificates and electronic signatures without exposing signing private keys to the browser or a remote server.

This repository is a community-friendly fork of [`nowina-solutions/nexu`](https://github.com/nowina-solutions/nexu). The modernized application uses Java 17, Spring Boot 3.5.16, DSS 6.4 and JavaFX 21.0.11 while preserving the existing NexU integration model and legacy browser endpoints.

## Test NexU with the European Commission DSS demo

Use the European Commission Digital Signature Services WebApp Demo to verify the complete NexU browser-signing workflow:

<https://ec.europa.eu/digital-building-blocks/DSS/webapp-demo/sign-a-document>

### Prepare NexU and the browser

1. Start NexU and wait until its notification-area icon appears.
2. Verify the local HTTP endpoint:

   <http://127.0.0.1:9795/nexu-info>

3. Verify the local HTTPS endpoint:

   <https://localhost:9895/nexu-info>

4. If the browser reports that the localhost certificate is not trusted or valid, use the browser's standard advanced or continue procedure to accept it. Confirm that the address is exactly `https://localhost:9895/nexu-info` before proceeding.
5. The same guidance is available from the NexU notification-area menu through **Enable NexU in browser**. The panel displays the verification address and can open it in the default browser. If browser policy prevents acceptance, ask the administrator to trust `config/localhost.crt`.
6. Reload the DSS demo after the local HTTPS page opens without a certificate warning.

The generated certificate applies only to the local NexU endpoint. Do not disable certificate validation globally or for unrelated websites.

### Sign a document step by step

1. Open the DSS **Sign a document** page.
2. Select and upload a test document.
3. Choose the desired signature format and signing options offered by the demo.
4. Start the signing operation.
5. When NexU opens **Signature Mean Selection**, choose one of the available signing sources:
   - a detected smart card;
   - the Windows certificate store;
   - a registered JKS, P12 or PFX keystore;
   - **New keystore** to select a local file that has not yet been registered.
6. Enter the smart-card PIN or local-keystore password when NexU requests it.
7. Select the signing certificate.
8. Confirm the signing operation when NexU requests access to the private key.
9. Wait for the DSS backend to finalize the document.
10. Download the signed document and use the DSS validation page to inspect the resulting signature when required.

During a successful legacy integration, the browser calls:

```text
POST https://localhost:9895/rest/certificates
POST https://localhost:9895/rest/sign
```

If browser developer tools show `https://127.0.0.1:9895`, install a current NexU build or set `nexu_hostname=localhost`. Public signing pages commonly allow `https://localhost:9895` in their Content Security Policy but reject the equivalent numeric loopback URL.

### Two-stage DSS signing flow

The DSS demo uses two separate local operations:

1. **Certificate discovery** — `/rest/certificates` opens the selected signing source and reads the certificates available for selection.
2. **Private-key signing** — after the remote DSS backend prepares the signature digest, `/rest/sign` asks NexU to unlock the selected private key and sign that digest locally.

The certificate is sent to the remote DSS service, but the private key, smart-card PIN and local-keystore password remain inside NexU. They are never sent to the browser or the European Commission service.

### Test with one password prompt

The default setting is:

```properties
close_token=true
```

With this value, NexU closes the token or local keystore after certificate discovery. A later signing request must reopen it, so a file keystore can produce two clearly identified prompts:

- **Keystore certificate access** — opens the JKS or PKCS#12 file and reads the certificates;
- **Private-key signing** — reopens the file and unlocks the selected private key for the signature.

For most JKS, P12 and PFX files, both prompts use the same password. Some smart-card drivers can also request a PIN separately for certificate access and private-key use.

To test the DSS flow with one local-keystore password prompt, use an external `nexu-config.properties` file containing:

```properties
close_token=false
cache_time_to_live_ms=60000
```

Restart NexU after changing the configuration. NexU then shows a combined **Certificate selection and signing** prompt and keeps the password only in process memory for the configured cache period. The password is not written to disk and is not transmitted to the DSS website.

Use the shortest cache duration that reliably covers the remote digest-preparation step. A value between 30 and 60 seconds is normally sufficient for testing. Smart-card middleware may still enforce an additional PIN prompt independently of NexU.

## Highlights

- Java 17 and a two-module Maven reactor.
- Spring Boot loopback server with legacy and modern signing APIs.
- HTTP on port `9795` by default.
- HTTPS on port `9895` with a per-installation self-signed localhost certificate generated on first start.
- Signing with smart cards, the Windows certificate store, JKS files and PKCS#12 files.
- Windows and Linux native packages with a private Java runtime.
- Native AWT notification-area menu on Windows.
- English and Italian desktop interface with persistent language selection.
- A single independent JavaFX window at a time.
- NexU key icon on JavaFX title bars.
- Diagnostic-log dialog with the full path and a resilient text-editor opener.
- Browser-certificate guidance panel with a direct localhost verification link.
- Verified Windows and Linux shutdown helpers.
- About links to this project and the EUPL 1.2 licence.

## Project structure

The Maven reactor contains two modules:

- **`nexu-core`** — headless API, models, utilities, DSS signing, PC/SC, PKCS#11, Windows certificate-store and file-keystore support.
- **`nexu-app`** — Spring Boot loopback server, JavaFX operator interface, browser endpoints and native packaging.

```text
Browser
   │
   │ loopback HTTP or HTTPS
   ▼
nexu-app
   ├── Spring Boot local server
   ├── JavaFX trusted operator UI
   ├── legacy /rest compatibility
   ├── modern /v1 API
   └── native packaging
          │
          ▼
      nexu-core
      ├── DSS 6.4 signing
      ├── certificate and key selection
      ├── PC/SC and PKCS#11 smart cards
      ├── Windows certificate store
      └── JKS and PKCS#12 file keystores
```

Challenge storage, certificate trust validation, authentication-token validation and document finalization belong to the remote web application, not the local desktop agent.

The architecture decision is recorded in [`docs/adr/0001-module-consolidation-and-web-eid-flow.md`](docs/adr/0001-module-consolidation-and-web-eid-flow.md).

## Build from source

```bash
mvn clean package
```

The executable Spring Boot JAR is created at:

```text
nexu-app/target/nexu-app.jar
```

Run it directly for diagnostics with:

```bash
java -jar nexu-app/target/nexu-app.jar
```

Native package users do not need to install Java separately.

## Local endpoints

NexU binds only to loopback interfaces.

| Protocol | Default endpoint | Purpose |
| --- | --- | --- |
| HTTP | `http://127.0.0.1:9795/nexu-info` | Legacy-compatible local endpoint and diagnostics. |
| HTTPS | `https://localhost:9895/nexu-info` | Endpoint for secure browser pages such as the European Commission DSS demo. |

```properties
binding_ip=127.0.0.1
binding_ports=9795
binding_ports_https=9895
```

HTTP remains available when HTTPS cannot be started.

## Automatic localhost HTTPS

The TLS directory is created beside the active `logs` directory:

```text
NexU data root/
├── logs/
│   └── nexu.log
└── config/
    ├── HTTPS.txt
    ├── localhost.crt
    ├── localhost.key
    └── localhost.p12
```

When neither a certificate nor a private key exists, NexU generates a unique per-installation pair on first start:

- `config/localhost.crt` — self-signed X.509 certificate in PEM format;
- `config/localhost.key` — unencrypted PKCS#8 RSA private key in PEM format.

The generated certificate:

- uses a 2048-bit RSA key;
- is signed with SHA-256;
- contains `localhost` and `127.0.0.1` subject alternative names;
- is valid for ten years;
- is generated locally and is not shared with other installations.

The older `config/localhost.cer` certificate name remains supported for existing installations. `localhost.crt` is preferred for new installations.

NexU never overwrites operator-provided TLS material. If only a certificate or only a key is present, HTTPS remains disabled and the exact missing path is written to the diagnostic log. HTTP continues running.

`config/localhost.p12` is optional and is not used by the Spring Boot connector. It can be created for operating-system or browser trust-store import.

> `config/localhost.p12` is unrelated to a PKCS#12 signing keystore. A signing `.p12` or `.pfx` contains a user signing identity and may be stored anywhere accessible to the user.

### Replace the generated certificate

Stop NexU, remove or replace both PEM files together, then restart. Example:

```bash
openssl req -x509 -newkey rsa:2048 -sha256 -nodes -days 3650 \
  -keyout localhost.key -out localhost.crt \
  -subj "/CN=localhost" \
  -addext "subjectAltName=DNS:localhost,IP:127.0.0.1"
```

Optional PKCS#12 export for trust-store import:

```bash
openssl pkcs12 -export -out localhost.p12 \
  -inkey localhost.key -in localhost.crt -passout pass:
```

A self-signed certificate is not trusted automatically. Open the endpoint directly and accept the browser warning, use **Enable NexU in browser** from the notification-area menu, or import the certificate into the appropriate local trust store:

```text
https://localhost:9895/nexu-info
```

A successful response contains the running version:

```json
{
  "version": "1.24-SNAPSHOT"
}
```

## Signing key sources

NexU is not limited to smart cards. It supports:

- smart cards exposed through PC/SC, a card minidriver/KSP or a vendor PKCS#11 library;
- the Windows certificate store when a certificate has an accessible private key;
- local JKS files with extension `.jks`;
- local PKCS#12 files with extension `.p12` or `.pfx`.

A file keystore must contain at least one private-key entry with its certificate chain. A certificate-only file cannot produce a signature.

### Register a local keystore during signing

1. Start a signing operation.
2. Select **New keystore** in **Signature Mean Selection**.
3. Choose **JKS** or **PKCS#12**.
4. Select the `.jks`, `.p12` or `.pfx` file.
5. Enter its password when requested.
6. Select a certificate/private-key entry.
7. Select **Remember** when NexU asks whether the keystore should be registered.

NexU stores only the keystore type and file location. It never stores the keystore password.

## Manage keystores

Open the notification-area menu and select **Manage keystores**.

The window provides:

- **Add smart card** — checks the PC/SC service, installed reader drivers, connected readers and inserted cards. It displays reader names and ATR values. Smart cards are dynamic devices and are not stored as file registrations; a detected card appears automatically in the next signing-device selection.
- **Add local keystore** — selects and immediately registers a `.jks`, `.p12` or `.pfx` file. The password is not requested until the keystore is used for signing.
- **Open keystore file** — asks the operating system to open the selected file with its associated application.
- **Remove** — removes the NexU registration without deleting or modifying the original keystore file.

Moving or renaming a registered file invalidates its saved path. Remove the old registration and add the file again.

Keep local keystore files in a user-protected directory, restrict file permissions and maintain a secure backup. Anyone who obtains both a keystore file and its password may be able to use its private key.

## Signing flow

1. The browser asks NexU for a signing certificate.
2. The browser sends the certificate to the remote signing backend.
3. The backend prepares the document signature structure and returns a digest and digest algorithm.
4. NexU signs the prepared digest with the selected provider.
5. The backend validates the response and finalizes the document.

Prepared digests are signed through DSS `SignatureTokenConnection.signDigest(...)`; they must not be passed to the historical raw-data signing method, which would hash them again.

## Modern local API

Protocol identifier: `nexu:2.0`.

| Method | Endpoint | Purpose |
| --- | --- | --- |
| `GET` | `/v1/status` | Returns version and supported capabilities. |
| `POST` | `/v1/signing-certificate` | Selects a certificate and returns an opaque local key handle. |
| `POST` | `/v1/sign` | Signs a Base64-encoded prepared digest. |
| `GET` | `/nexu-v2.js` | Promise-based browser client. |

Modern `/v1/**` browser requests require an explicit origin allowlist:

```properties
cors_allowed_origin=https://sign.example.org,https://test-sign.example.org
```

The historical wildcard remains available only for legacy compatibility.

## Legacy compatibility

Existing integrations can continue to use:

- `GET /nexu-info`
- `GET /nexu.js`
- `GET /favicon.ico`
- `POST /rest/certificates`
- `POST /rest/sign`
- `POST /rest/logout`

The obsolete `/rest/authenticate` and `/rest/identityInfo` endpoints return HTTP `410 Gone`.

## Windows notification-area menu

Windows uses the JDK AWT tray backend by default:

```properties
systray_backend=awt
```

This lets Windows position the context menu beside the notification-area icon. Dorkbox remains available for diagnostics:

```properties
systray_backend=dorkbox
```

The menu contains About, Enable NexU in browser, Preferences, Show logs, Select language, Manage keystores and Exit.

**Enable NexU in browser** opens a localized panel that explains the localhost certificate-trust step, displays the exact verification endpoint and provides a button to open it in the default browser. It does not install certificates silently or disable browser security checks.

Only one independent JavaFX window can be open at a time. Selecting another action restores and focuses the existing window. Every JavaFX stage uses the NexU key icon.

### Show logs

**Show logs** displays the complete selectable path to the current diagnostic file and provides **Open with default text editor**.

NexU first tries the operating-system file association. When Windows reports that `.log` has no associated application, NexU falls back to Notepad. Linux uses `xdg-open`/`gio` and common graphical text-editor fallbacks; macOS uses `open`.

### About

The About dialog links to:

- <https://github.com/p4535992/nexu>
- <https://interoperable-europe.ec.europa.eu/sites/default/files/custom-page/attachment/2020-03/EUPL-1.2%20EN.txt>

## Existing-instance replacement

When `replace_existing_nexu=true`, the launcher checks `/nexu-info` on the configured HTTP port before terminating anything. It stops the existing process only after verifying a NexU version and resolving the listening PID. Unrelated processes are never terminated when verification fails.

## Shutdown helpers

Native packages contain:

- Windows: `nexu-force-stop.bat`
- Linux: `nexu-force-stop.sh`

Port resolution order:

1. command-line argument;
2. `NEXU_PORT`;
3. `binding_ports` in `nexu-config.properties`;
4. fallback `9795`.

Both helpers verify `/nexu-info` before terminating the listener.

## Native packages

Packages must be built on the target operating system because JavaFX contains platform-specific native libraries.

### Windows

The release build produces:

- portable ZIP with `NexU.exe`, private runtime, configuration, shutdown helper, logs guide, licences and `config/HTTPS.txt`;
- per-user EXE installer with Start menu and desktop shortcuts.

```powershell
./nexu-app/src/jpackage/package-windows.ps1 `
    -JarPath nexu-app/target/nexu-app.jar `
    -Destination nexu-app/target/jpackage `
    -AppVersion 1.24.0
```

### Linux

The release build produces:

- portable `tar.gz` application image;
- Debian/Ubuntu `.deb` package.

```bash
bash nexu-app/src/jpackage/package-linux.sh \
    nexu-app/target/nexu-app.jar \
    nexu-app/target/jpackage \
    1.24.0
```

The packaged Linux shutdown helper is executable with mode `0755`.

## External configuration

Configuration lookup order:

1. `-Dnexu.config.file=/path/to/nexu-config.properties`
2. `NEXU_CONFIG_FILE`
3. jpackage launcher directory and application-image root
4. current working directory
5. directly executed JAR directory
6. embedded defaults

Important properties:

```properties
binding_ip=127.0.0.1
binding_ports=9795
binding_ports_https=9895
cors_allowed_origin=*
close_token=true
cache_time_to_live_ms=10000
enable_systray_menu=true
systray_backend=awt
systray_debug=true
replace_existing_nexu=true
show_already_running_dialog=true
log_directory=
log_level=DEBUG
```

## Diagnostic logs

Default current log:

```text
Windows: %USERPROFILE%\.nexu\logs\nexu.log
Linux:   $HOME/.nexu/logs/nexu.log
```

Portable packages use the `logs` directory beside the application image.

Rotation defaults:

- current file: `nexu.log`
- archives: `archive/nexu.YYYY-MM-DD.N.log.gz`
- maximum file size: 10 MB
- retained periods: 14
- total archive cap: 200 MB

Override the directory with `NEXU_LOG_DIR`, `-Dnexu.log.dir=/path` or `log_directory`.

## Smart-card drivers and middleware

Smart-card use relies on:

1. the operating-system PC/SC service and reader driver;
2. a card minidriver/KSP or vendor PKCS#11 library when required;
3. the NexU adapter.

NexU does not silently install arbitrary drivers.

- **Windows:** use the built-in smart-card stack and Windows Update first.
- **Linux:** install distribution packages for `pcscd`, `libpcsclite` and CCID readers.

```bash
sudo apt install libpcsclite1 pcscd libccid
```

JKS and PKCS#12 file keystores do not require a reader or PC/SC middleware.

## Security principles

- Signing private keys are never transmitted to the browser or remote server.
- Smart-card and operating-system-store keys remain inside their signing provider.
- File-keystore passwords are requested when needed and are not stored by NexU.
- PIN, password and certificate selection remain in the trusted local application.
- The local server binds only to loopback interfaces.
- PINs, passwords, hashes, handles and signature material are not written to logs.
- The remote backend independently validates certificate trust, purpose and algorithms.
- The localhost TLS private key is generated locally per installation and is not committed or bundled as a shared key.

## License

NexU is distributed under the **European Union Public Licence, version 1.2 (EUPL-1.2)**. See [`LICENSE`](LICENSE).

Official English licence text:

<https://interoperable-europe.ec.europa.eu/sites/default/files/custom-page/attachment/2020-03/EUPL-1.2%20EN.txt>

Web eID attribution and other third-party notices are recorded in [`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md).
