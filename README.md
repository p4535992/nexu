# NexU

NexU is a local signing agent that lets a web application request certificates and electronic signatures without exposing the signing private key to the browser or a remote server.

This repository is a community-friendly fork of [`nowina-solutions/nexu`](https://github.com/nowina-solutions/nexu). The modernized application uses Java 17, Spring Boot 3.5.16, DSS 6.4 and JavaFX 21.0.11 while preserving the existing NexU integration model and legacy browser endpoints.

## Highlights

- Java 17 and a two-module Maven reactor.
- Spring Boot loopback server with legacy and modern signing APIs.
- HTTP on port `9795` by default.
- Optional HTTPS on port `9895` using operator-provided PEM certificate and key files.
- Signing with smart cards, the Windows certificate store, local JKS files and local PKCS#12 files.
- Windows and Linux native packages with a private Java runtime.
- Windows notification-area menu using the native AWT tray backend by default.
- English and Italian desktop interface, with English as the first-run default.
- Persistent language selection.
- A single active JavaFX window at a time.
- NexU key icon on JavaFX title bars.
- Diagnostic-log dialog with the complete path and an explicit button to open the file with the operating-system default text editor.
- Verified shutdown helpers for Windows and Linux.
- Automatic replacement of a previously running NexU instance only after `/nexu-info` confirms that the process bound to the configured port is NexU.
- About dialog links to this GitHub project and the EUPL 1.2 license.

## Project structure

The Maven reactor contains two modules:

- **`nexu-core`** — headless API, models, utilities, DSS signing, PC/SC, PKCS#11, Windows certificate-store and file-keystore support.
- **`nexu-app`** — Spring Boot loopback server, JavaFX operator UI, compatibility endpoints and native packaging.

The former API, model, utility, standalone, server-plugin, bundle and keystore modules were consolidated into these directories. `nexu-core` has no JavaFX, Spring, Servlet or Jetty dependency.

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

Build the complete application with:

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

The default configuration exposes the service only on loopback interfaces.

| Protocol | Default endpoint | Purpose |
| --- | --- | --- |
| HTTP | `http://127.0.0.1:9795/nexu-info` | Legacy-compatible local endpoint and diagnostics. |
| HTTPS | `https://localhost:9895/nexu-info` | HTTPS endpoint for secure browser pages such as the European Commission DSS demo. |

The HTTP and HTTPS ports are configured independently:

```properties
binding_ip=127.0.0.1
binding_ports=9795
binding_ports_https=9895
```

HTTP remains available when HTTPS cannot be started.

## HTTPS configuration

NexU reads the TLS certificate and private key from a `config` directory located beside the active `logs` directory.

```text
NexU data root/
├── logs/
│   └── nexu.log
└── config/
    ├── HTTPS.txt
    ├── localhost.cer
    ├── localhost.key
    └── localhost.p12
```

The required files are:

- `config/localhost.cer` — X.509 certificate in PEM format.
- `config/localhost.key` — matching, unencrypted private key in PEM format.

`config/localhost.p12` is optional and is not used by the Spring Boot connector. It can be useful for importing the localhost certificate into an operating-system or browser trust store.

> `config/localhost.p12` is unrelated to a PKCS#12 signing keystore registered through the NexU desktop workflow. The file under `config` belongs to local HTTPS; a signing `.p12` or `.pfx` contains a user signing identity and can be stored anywhere accessible to the user.

NexU creates the `config` directory and copies an `HTTPS.txt` guide into it. Portable Windows and Linux archives already contain `config/HTTPS.txt`.

When either `localhost.cer` or `localhost.key` is absent, NexU writes an explicit error to the diagnostic log and leaves HTTPS disabled. The HTTP endpoint continues running.

### Generate a self-signed localhost certificate

Run these commands from the `config` directory:

```bash
openssl req -x509 -newkey rsa:2048 -sha256 -nodes -days 3650 \
  -keyout localhost.key -out localhost.cer \
  -subj "/CN=localhost" \
  -addext "subjectAltName=DNS:localhost,IP:127.0.0.1"
```

Optionally create a PKCS#12 file for trust-store import:

```bash
openssl pkcs12 -export -out localhost.p12 \
  -inkey localhost.key -in localhost.cer -passout pass:
```

A self-signed certificate is not trusted automatically. Before a browser-based test, open the HTTPS endpoint directly and accept the browser warning, or import the certificate into the appropriate local trust store.

```text
https://localhost:9895/nexu-info
```

A successful response contains the running NexU version:

```json
{
  "version": "1.24-SNAPSHOT"
}
```

## Test NexU with the European Commission DSS demo

After NexU has started:

1. Verify the HTTP endpoint:

   ```text
   http://127.0.0.1:9795/nexu-info
   ```

2. Verify the HTTPS endpoint:

   ```text
   https://localhost:9895/nexu-info
   ```

3. When using a self-signed certificate, accept the browser certificate warning for `https://localhost:9895` before opening the demo.
4. Open the European Commission Digital Signature Services WebApp Demo:

   <https://ec.europa.eu/digital-building-blocks/DSS/webapp-demo/>

5. Select **Sign a document** and upload a test document.
6. In the NexU signing-device selection, choose a detected smart card, a registered local keystore, or **New keystore**.
7. Complete certificate selection and the PIN or keystore-password prompt.

The direct signing page is also available at:

<https://ec.europa.eu/digital-building-blocks/DSS/webapp-demo/sign-a-document>

The demo page is served over HTTPS, so the local NexU HTTPS endpoint must be running and accepted by the browser.

## Signing key sources

NexU is not limited to certificates stored on smart cards. A signing operation can use any of these supported sources:

- a smart card exposed through PC/SC, a card minidriver/KSP or a vendor PKCS#11 library;
- the Windows certificate store when the certificate has an accessible private key;
- a local **JKS** file with extension `.jks`;
- a local **PKCS#12** file with extension `.p12` or `.pfx`.

A local keystore must contain at least one private-key entry with its corresponding certificate chain and must permit the requested signature algorithm. A certificate-only file cannot produce a signature.

### Register and use a local keystore

Registration happens during a signing operation, not from the **Manage keystores** window:

1. Start NexU.
2. Start a signing operation, for example from the European Commission DSS demo.
3. In **Signature Mean Selection**, select **New keystore**.
4. Select the keystore type:
   - **JKS** for a `.jks` file;
   - **PKCS#12** for a `.p12` or `.pfx` file.
5. Browse to the local keystore file.
6. Continue the signing flow and enter the keystore password when NexU requests it.
7. Select the certificate/private-key entry to use.
8. When NexU asks whether it should remember the keystore, select **Remember** to register it for future signing operations.

NexU stores only the keystore type and file location in its local keystore database. The keystore password is not stored and is requested again when the file must be opened.

After registration, the keystore appears in later **Signature Mean Selection** dialogs alongside detected smart cards and other configured signing sources.

### Manage or remove a registered keystore

Open the NexU notification-area menu and select **Manage keystores** to:

- view saved keystore names, types and full file locations;
- remove a saved registration.

The current **Manage keystores** dialog does not add a new entry directly. Use **New keystore** during a signing operation to add one.

Removing a registration does not delete or modify the `.jks`, `.p12` or `.pfx` file. If a registered file is moved, renamed or replaced, remove the old registration and register the new path during the next signing operation.

Keep local keystore files in a user-protected directory, restrict file permissions and maintain a secure backup. Anyone who obtains both the keystore file and its password may be able to use the contained private key.

## Signing flow

The signing flow follows the separation used by Web eID while retaining NexU compatibility:

1. The browser asks the local agent for a signing certificate.
2. The browser sends the certificate to the remote signing backend.
3. The backend prepares the document signature structure and returns a digest and digest algorithm.
4. NexU signs the prepared digest with the selected smart-card, operating-system-store or file-keystore key.
5. The backend validates the response and finalizes the document.

The prepared digest is signed through DSS `SignatureTokenConnection.signDigest(...)`. It must not be passed to the historical raw-data signing method, which would hash it a second time.

## Modern local API

The modern protocol identifier is `nexu:2.0`.

| Method | Endpoint | Purpose |
| --- | --- | --- |
| `GET` | `/v1/status` | Returns version and supported capabilities. |
| `POST` | `/v1/signing-certificate` | Selects a certificate and returns an opaque local key handle. |
| `POST` | `/v1/sign` | Signs a Base64-encoded prepared digest. |
| `GET` | `/nexu-v2.js` | Promise-based browser client using `fetch`. |

Example:

```html
<script src="http://127.0.0.1:9795/nexu-v2.js"></script>
<script>
async function signPreparedHash(hash, hashFunction) {
    const certificate = await NexU.getSigningCertificate({
        certificatePurpose: "SIGNATURE"
    });

    return NexU.sign(certificate, hash, hashFunction, {
        clearToken: true
    });
}
</script>
```

The returned `keyHandle` is an opaque identifier valid only inside the running NexU process. It is not a private key.

## Browser origins

The local service listens only on loopback. Modern `/v1/**` browser requests require an explicit origin allowlist:

```properties
cors_allowed_origin=https://sign.example.org,https://test-sign.example.org
```

The historical wildcard remains available only for legacy compatibility. It is rejected for modern `/v1/**` browser requests.

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

Windows uses the JDK AWT system-tray backend by default:

```properties
systray_backend=awt
```

This lets Windows position the context menu beside the notification-area icon. Dorkbox remains available for diagnostics:

```properties
systray_backend=dorkbox
```

The menu contains:

- About
- Preferences
- Show logs
- Select language
- Manage keystores
- Exit

The desktop interface starts in English on first run. English and Italian selections are persisted across restarts.

Only one independent JavaFX window can be open at a time. Selecting another tray action restores, focuses and brings the existing window to the front instead of creating or replacing another window. Blocking operations rejected by this rule are released with a user-cancel result so no worker remains suspended.

Every JavaFX stage, alert and choice dialog uses the NexU key icon in its title bar.

### About dialog

The About dialog displays the application and JVM version and provides links to:

- the project repository: <https://github.com/p4535992/nexu>
- the EUPL 1.2 license: <https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12>

The links open in the operating-system default browser.

### Show logs

Selecting **Show logs** opens a localized dialog that displays the complete, selectable log-file path. The file is created first when necessary.

The dialog provides:

- **Open with default text editor**
- **Close**

The file is opened only after the explicit editor button is selected.

## Existing-instance replacement

When `replace_existing_nexu=true`, a new launcher checks the configured HTTP port before starting.

NexU terminates the existing port owner only when all of these conditions are met:

1. `/nexu-info` responds on the configured port.
2. The response contains a NexU version.
3. The operating system resolves the process that owns the listening port.
4. The resolved process is not the current launcher.

If verification fails, no unrelated process is terminated.

## Shutdown helpers

Every native package contains a platform-specific verified shutdown helper:

- Windows: `nexu-force-stop.bat`
- Linux: `nexu-force-stop.sh`

The helpers resolve the HTTP port in this order:

1. command-line argument;
2. `NEXU_PORT` environment variable;
3. `binding_ports` in `nexu-config.properties`;
4. fallback port `9795`.

They verify that `/nexu-info` contains a NexU version before terminating the listening process.

Windows examples:

```bat
nexu-force-stop.bat
nexu-force-stop.bat 9795
nexu-force-stop.bat "" "C:\path\to\nexu-config.properties"
```

Linux examples:

```bash
./nexu-force-stop.sh
./nexu-force-stop.sh 9795
./nexu-force-stop.sh "" /path/to/nexu-config.properties
```

The Linux helper sends `SIGTERM` first and uses `SIGKILL` only when the process does not stop within the grace period.

## Native packages

Packages must be built on the target operating system because JavaFX contains platform-specific native libraries.

### Windows

The Windows release build produces:

- a portable ZIP containing `NexU.exe`, a private Java runtime, `nexu-config.properties`, `LOGS.txt`, `nexu-force-stop.bat`, licenses and `config/HTTPS.txt`;
- a per-user EXE installer with Start menu and desktop shortcuts.

```powershell
./nexu-app/src/jpackage/package-windows.ps1 `
    -JarPath nexu-app/target/nexu-app.jar `
    -Destination nexu-app/target/jpackage `
    -AppVersion 1.24.0
```

Portable layout:

```text
NexU/
├── NexU.exe
├── nexu-config.properties
├── nexu-force-stop.bat
├── LOGS.txt
├── config/
│   └── HTTPS.txt
├── logs/
├── app/
└── runtime/
```

### Linux

The Linux release build produces:

- a portable `tar.gz` application image containing `nexu-force-stop.sh`, `LOGS.txt` and `config/HTTPS.txt`;
- a Debian/Ubuntu `.deb` package.

```bash
bash nexu-app/src/jpackage/package-linux.sh \
    nexu-app/target/nexu-app.jar \
    nexu-app/target/jpackage \
    1.24.0
```

Portable layout:

```text
NexU/
├── bin/
├── lib/
├── nexu-config.properties
├── nexu-force-stop.sh
├── LOGS.txt
├── config/
│   └── HTTPS.txt
└── logs/
```

The packaged Linux shutdown helper has executable mode `0755`.

## External configuration

Configuration is searched in this order:

1. `-Dnexu.config.file=/path/to/nexu-config.properties`
2. `NEXU_CONFIG_FILE`
3. the `jpackage` launcher directory and application-image root
4. the current working directory
5. the directory of a directly executed JAR
6. embedded defaults

Important desktop and server properties include:

```properties
binding_ip=127.0.0.1
binding_ports=9795
binding_ports_https=9895
cors_allowed_origin=*
enable_systray_menu=true
systray_backend=awt
systray_debug=true
replace_existing_nexu=true
show_already_running_dialog=true
log_directory=
log_level=DEBUG
```

## Diagnostic logs

NexU uses SLF4J and the Logback implementation supplied by Spring Boot. Application diagnostic logging defaults to `DEBUG`, while Spring and third-party framework logging remains at `INFO`.

The default current log is:

```text
Windows: %USERPROFILE%\.nexu\logs\nexu.log
Linux:   $HOME/.nexu/logs/nexu.log
```

Portable packages use the `logs` directory beside the application image. The exact active path is written in the first startup entries and documented by `LOGS.txt`.

Logback rotates the file by date and size:

- current file: `nexu.log`
- compressed archives: `archive/nexu.YYYY-MM-DD.N.log.gz`
- maximum file size: 10 MB
- retained periods: 14
- total archive cap: 200 MB

Override the directory through `NEXU_LOG_DIR`, `-Dnexu.log.dir=/path` or `log_directory` in `nexu-config.properties`. Rotation and level can be changed with `log_level`, `rolling_log_file_size`, `rolling_log_file_number` and `rolling_log_total_size_cap`.

HTTPS startup messages are written to the same log. When certificate files are missing, the log lists the exact expected paths.

## Smart-card drivers and middleware

Smart-card use relies on three layers:

1. the operating-system PC/SC service and reader driver;
2. a card minidriver/KSP or vendor PKCS#11 library when required;
3. the NexU adapter that discovers and uses the provider.

NexU does not silently install arbitrary drivers.

- **Windows:** use the built-in smart-card stack and Windows Update first. Install vendor middleware only when required.
- **Linux:** use distribution packages for `pcscd`, `libpcsclite` and CCID reader support.

On Debian or Ubuntu:

```bash
sudo apt install libpcsclite1 pcscd libccid
```

Local JKS and PKCS#12 file keystores do not require a smart-card reader or PC/SC middleware.

## Authentication boundary

Authentication is separate from document signing:

1. The remote backend creates a high-entropy, short-lived challenge and stores it in the browser session.
2. The local agent signs protocol-defined data bound to the challenge and requesting origin.
3. The backend atomically consumes the challenge.
4. The backend treats the submitted certificate as untrusted and validates chain, purpose, validity, revocation, algorithm, origin and signature.
5. Only the backend creates the authenticated session.

The Web eID validation library belongs to the remote application or an integration example. It is not embedded in NexU as a replacement for server-side validation.

## Security principles

- Signing private keys are never transmitted to the browser or remote server.
- Smart-card and operating-system-store keys remain inside their signing provider.
- File-keystore keys remain in the selected local JKS or PKCS#12 file and are accessed locally by NexU for signing.
- Keystore passwords are requested when needed and are not stored in the NexU keystore database.
- PIN entry, keystore-password entry and certificate selection remain in the trusted local application.
- The local service binds only to loopback interfaces.
- Browser origins are validated according to the selected API compatibility mode.
- Signing handles and authentication challenges are short-lived and single-use.
- PINs, passwords, hashes, handles and signature material are not written to logs.
- The remote backend independently validates certificate trust, purpose and algorithms.
- No private TLS key is committed to this repository or bundled in release artifacts.

## Legacy tests

Historical tests that depend on DSS 5 APIs, Log4j 1.x fixtures or retired Jetty components are preserved under:

```text
nexu-core/src/legacy-test
```

They are not part of the default Maven build. Each test must be migrated to DSS 6.4 and the current test stack before returning to `src/test`.

## Web eID references

The security-flow separation is informed by:

- [`web-eid/web-eid-spring-boot-example`](https://github.com/web-eid/web-eid-spring-boot-example)
- [`web-eid/web-eid-authtoken-validation-java`](https://github.com/web-eid/web-eid-authtoken-validation-java)

NexU is not an official Web eID implementation.

## License

NexU is distributed under the **European Union Public Licence, version 1.2 (EUPL-1.2)**. See [`LICENSE`](LICENSE).

Web eID attribution and other third-party notices are recorded in [`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md).
