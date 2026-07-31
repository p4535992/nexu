# NexU

NexU is a local signing agent that lets web applications request certificates and electronic signatures without exposing signing private keys to the browser or a remote server.

This repository is a community-friendly fork of [`nowina-solutions/nexu`](https://github.com/nowina-solutions/nexu). The modernized application uses Java 17, Spring Boot 3.5.16, DSS 6.4 and JavaFX 21.0.11 while preserving the existing NexU integration model and legacy browser endpoints.

## Test NexU with the European Commission DSS demo

Use the public European Commission Digital Signature Services demo to verify the complete browser-to-NexU signing flow:

<https://ec.europa.eu/digital-building-blocks/DSS/webapp-demo/>

Direct signing page:

<https://ec.europa.eu/digital-building-blocks/DSS/webapp-demo/sign-a-document>

### 1. Start NexU

Start the installed application or run the executable JAR:

```bash
java -jar nexu-app/target/nexu-app.jar
```

Wait until the notification-area icon appears. The log should report both the HTTP endpoint on port `9795` and the HTTPS endpoint on port `9895`.

### 2. Verify the local endpoints

Open these addresses in the same browser that will run the DSS demo:

```text
http://127.0.0.1:9795/nexu-info
https://localhost:9895/nexu-info
```

Both endpoints should return a JSON object containing the NexU version. The HTTPS endpoint uses a per-installation self-signed certificate. On first use, explicitly accept the browser warning or import `config/localhost.crt` into the current user's trusted root store.

Do not delete `config/localhost.crt` or `config/localhost.key` after trusting the certificate. If they are removed, NexU generates a new pair and the browser must trust the new certificate again.

### 3. Open the DSS signing page

1. Open the direct signing page shown above.
2. Select a document from your computer.
3. Choose the desired signature format and options offered by the demo.
4. Start the signing operation.
5. The page loads NexU's browser client from `https://localhost:9895/nexu.js` and calls `https://localhost:9895/rest/certificates`.
6. NexU opens the trusted local selection window.

If the browser console shows a CSP error for `https://127.0.0.1:9895`, verify that the active external configuration contains:

```properties
nexu_hostname=localhost
```

Then restart NexU and reload the DSS page with a hard refresh.

### 4. Select the signing source

NexU can sign with:

- a detected smart card;
- the Windows certificate store;
- a previously registered JKS, P12 or PFX keystore;
- **New keystore**, to select a local `.jks`, `.p12` or `.pfx` file.

For a local keystore, enter its password and select a certificate that has an associated private key. NexU never sends the keystore password, PIN or private key to the DSS website.

### 5. Understand the two-stage DSS signing flow

The DSS demo signs in two distinct stages:

1. **Certificate-selection stage** — the browser asks NexU for the available certificates through `/rest/certificates`. NexU may need to open the keystore to read the certificate entries.
2. **Private-key signing stage** — the browser sends the selected certificate to the remote DSS service. The service prepares the document signature structure and returns a digest. The browser then calls `/rest/sign`, and NexU unlocks the selected private key to sign that digest locally.

The remote DSS service prepares and finalizes the document, but all password entry and private-key use remain inside NexU on the user's computer.

### 6. Password prompts and `close_token`

The number of password prompts depends on the token lifecycle configuration.

#### Two prompts: `close_token=true`

```properties
close_token=true
```

NexU closes the keystore/token after certificate discovery. When the later `/rest/sign` request arrives, NexU must reopen it. A local JKS or PKCS#12 file can therefore request the same password twice:

- **Keystore certificate access** — enter the password to open the local keystore and read the certificates available for selection.
- **Private-key signing** — enter the password to reopen the keystore and unlock the selected private key for the signature.

For most JKS, P12 and PFX files, both dialogs require the same keystore password. Some keystores can use a different password for a private-key entry.

#### One prompt: `close_token=false`

```properties
close_token=false
cache_time_to_live_ms=60000
```

NexU keeps the unlocked token in memory for the configured cache interval. The dialog is therefore shown once with a combined explanation: the password will be used for both certificate selection and private-key signing during the current DSS operation.

A short cache interval such as 30–60 seconds is recommended. The password is held only in process memory, is not written to disk and is not sent to the browser or DSS server.

Smart-card middleware may still request a PIN more than once when the card or vendor driver enforces separate authentication for certificate access and private-key use.

### 7. Complete and verify the signature

1. Confirm the certificate selection in NexU.
2. Enter the second password or PIN if required by the configured token lifecycle.
3. Wait for the DSS backend to finalize the signed document.
4. Download the result and use the demo's validation function to inspect the signature.

For troubleshooting, open **Show logs** from the NexU notification-area menu and inspect `logs/nexu.log`.

## Highlights

- Java 17 and a two-module Maven reactor.
- Spring Boot loopback server with legacy and modern signing APIs.
- HTTP on port `9795` and HTTPS on port `9895` by default.
- Per-installation self-signed localhost certificate generated on first start.
- Signing with smart cards, the Windows certificate store, JKS files and PKCS#12 files.
- Windows and Linux native packages with a private Java runtime.
- Native AWT notification-area menu on Windows.
- English and Italian desktop interface with persistent language selection.
- A single independent JavaFX window at a time.
- Diagnostic logs with rotation and resilient desktop opening.
- Verified Windows and Linux shutdown helpers.
- EUPL 1.2 licence.

## Project structure

The Maven reactor contains two modules:

- **`nexu-core`** — headless API, models, utilities, DSS signing, PC/SC, PKCS#11, Windows certificate-store and file-keystore support.
- **`nexu-app`** — Spring Boot loopback server, JavaFX operator interface, browser endpoints and native packaging.

```text
Browser
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
      ├── DSS signing
      ├── certificate and key selection
      ├── PC/SC and PKCS#11 smart cards
      ├── Windows certificate store
      └── JKS and PKCS#12 file keystores
```

## Build from source

```bash
mvn clean package
```

The executable application is created at `nexu-app/target/nexu-app.jar`. Native package users do not need to install Java separately.

## Local endpoints

NexU binds only to loopback interfaces.

| Protocol | Default endpoint | Purpose |
| --- | --- | --- |
| HTTP | `http://127.0.0.1:9795/nexu-info` | Legacy-compatible local endpoint and diagnostics. |
| HTTPS | `https://localhost:9895/nexu-info` | Secure endpoint for HTTPS signing pages. |

```properties
binding_ip=127.0.0.1
binding_ports=9795
binding_ports_https=9895
nexu_hostname=localhost
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

When neither a certificate nor a private key exists, NexU generates a unique per-installation pair on first start. The generated certificate uses RSA 2048/SHA-256, contains `localhost` and `127.0.0.1` subject alternative names and is valid for ten years.

The older `config/localhost.cer` filename remains supported. NexU never overwrites operator-provided TLS material. `localhost.p12` is optional and is unrelated to a PKCS#12 signing keystore.

## Signing key sources

NexU supports:

- smart cards exposed through PC/SC, a minidriver/KSP or a vendor PKCS#11 library;
- the Windows certificate store when a certificate has an accessible private key;
- local JKS files (`.jks`);
- local PKCS#12 files (`.p12` and `.pfx`).

A file keystore must contain at least one private-key entry with its certificate chain.

### Register a local keystore during signing

1. Start a signing operation.
2. Select **New keystore** in **Signature Mean Selection**.
3. Choose **JKS** or **PKCS#12**.
4. Select the local file.
5. Enter its password.
6. Select a certificate/private-key entry.
7. Select **Remember** when NexU asks whether the keystore should be registered.

NexU stores only the keystore type and file location. It never stores the keystore password.

## Manage keystores

Open the notification-area menu and select **Manage keystores**.

- **Add smart card** checks the PC/SC service, reader drivers, connected readers and inserted cards. It reports when no reader or card is found.
- **Add local keystore** registers a `.jks`, `.p12` or `.pfx` file without storing its password.
- **Open keystore file** opens the selected file with the operating system's associated application.
- **Remove** removes the NexU registration without deleting the original file.

## APIs

### Modern local API

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

### Legacy compatibility

Existing integrations can continue to use `/nexu-info`, `/nexu.js`, `/favicon.ico`, `/rest/certificates`, `/rest/sign` and `/rest/logout`.

## Windows notification-area menu

Windows uses the JDK AWT tray backend by default:

```properties
systray_backend=awt
```

The menu contains About, Preferences, Show logs, Select language, Manage keystores and Exit. Only one independent JavaFX window can be open at a time.

### Show logs

**Show logs** displays the complete path to the current diagnostic file and provides **Open with default text editor**. When Windows has no `.log` association, NexU falls back to Notepad. Linux uses `xdg-open`/`gio` and common graphical text editors; macOS uses `open`.

## Shutdown helpers

Native packages contain `nexu-force-stop.bat` on Windows and `nexu-force-stop.sh` on Linux. Both verify `/nexu-info` before terminating the listener.

## Native packages

Windows builds produce a portable ZIP and a per-user EXE installer. Linux builds produce a portable TAR.GZ and a Debian/Ubuntu DEB package. Packages include a private Java runtime.

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
nexu_hostname=localhost
cors_allowed_origin=*
close_token=true
cache_time_to_live_ms=10000
enable_systray_menu=true
systray_backend=awt
replace_existing_nexu=true
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

- current file: `nexu.log`;
- archives: `archive/nexu.YYYY-MM-DD.N.log.gz`;
- maximum file size: 10 MB;
- retained periods: 14;
- total archive cap: 200 MB.

## Smart-card drivers and middleware

Smart-card use relies on the operating-system PC/SC service and reader driver, plus a minidriver/KSP or vendor PKCS#11 library when required. NexU does not silently install arbitrary drivers. JKS and PKCS#12 file keystores do not require PC/SC middleware.

## Security principles

- Signing private keys are never transmitted to the browser or remote server.
- Smart-card and operating-system-store keys remain inside their signing provider.
- File-keystore passwords are requested when needed and are not stored on disk.
- PINs, passwords, hashes, handles and signature material are not written to logs.
- The local server binds only to loopback interfaces.
- The localhost TLS private key is generated locally per installation.

## License

NexU is distributed under the **European Union Public Licence, version 1.2 (EUPL-1.2)**. See [`LICENSE`](LICENSE).

Official English licence text:

<https://interoperable-europe.ec.europa.eu/sites/default/files/custom-page/attachment/2020-03/EUPL-1.2%20EN.txt>

Web eID attribution and other third-party notices are recorded in [`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md).
