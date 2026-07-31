# NexU

NexU is a local signing agent that allows web applications to request certificates and electronic signatures without exposing signing private keys to the browser or a remote server.

This repository is a community-maintained fork of [`nowina-solutions/nexu`](https://github.com/nowina-solutions/nexu). NexU 1.25 modernizes the original integration model with Java 21, Spring Boot 4.1.0, Spring Framework 7, Tomcat 11, DSS 6.4 and JavaFX 21.0.11 while preserving the legacy browser endpoints used by existing signing applications.

## Project scope and safety notice

NexU remains available for organisations and users that need browser-mediated local signing with smart cards, the Windows certificate store, JKS files or PKCS#12 keystores.

NexU is free and open-source software distributed under EUPL-1.2 and maintained on a best-effort community basis. It is not a commercial support service, a qualified trust service, a security certification or a guarantee that every card, driver, browser, operating system, keystore or remote signing application will behave correctly.

Before using NexU for production, legal, financial, regulated or otherwise sensitive signatures:

1. review the source code, configuration and dependencies according to your risk level;
2. test the complete workflow with the actual cards, middleware, keystores, browsers and signing backend;
3. independently validate the resulting signed documents and certificate chains;
4. protect keystores and backups and apply least-privilege permissions;
5. obtain an independent security, compliance and legal assessment when required.

The software is provided **as is**, without warranties, under EUPL-1.2 and applicable law.

## Test NexU with the European Commission DSS demo

Use the European Commission DSS WebApp Demo to test the complete browser-signing workflow:

<https://ec.europa.eu/digital-building-blocks/DSS/webapp-demo/sign-a-document>

### Prepare NexU and the browser

1. Start NexU and wait for its notification-area icon.
2. Verify HTTP: <http://127.0.0.1:9795/nexu-info>.
3. Verify HTTPS: <https://localhost:9895/nexu-info>.
4. If the browser reports that the localhost certificate is untrusted, confirm that the address is exactly `https://localhost:9895/nexu-info`, then use the browser's standard advanced/continue procedure.
5. The same instructions are available through **Enable NexU in browser** in the NexU tray menu.
6. Reload the DSS page after the local HTTPS endpoint opens without a certificate warning.

A successful legacy integration calls:

```text
POST https://localhost:9895/rest/certificates
POST https://localhost:9895/rest/sign
```

The certificate is sent to DSS, but private keys, PINs and keystore passwords remain inside NexU and are never sent to the website.

### Password prompts

The default is:

```properties
close_token=true
```

NexU closes the token after certificate discovery, so a local keystore can produce separate prompts for certificate access and private-key signing.

For one combined local-keystore prompt, use:

```properties
close_token=false
cache_time_to_live_ms=60000
```

Restart NexU after changing the configuration. Passwords are cached only in process memory for the configured period and are not written to disk.

## Highlights

- NexU application and protocol version 1.25.
- Java 21 and a two-module Maven reactor.
- Spring Boot 4.1 and Spring Framework 7 loopback server.
- Embedded Tomcat 11 with the Jakarta Servlet baseline.
- Legacy `/rest` endpoints and modern `/v1` endpoints.
- HTTP on `9795` and HTTPS on `9895` by default.
- Per-installation self-signed localhost certificate generated on first start.
- Smart cards, Windows certificate store, JKS and PKCS#12 signing sources.
- Windows and Linux native packages with a private Java runtime.
- English and Italian JavaFX interface and notification-area menu.
- Rotating diagnostic logs and verified shutdown helpers.

## Windows notification-area menu

On Windows, NexU uses the JDK AWT notification-area backend by default:

```properties
systray_backend=awt
```

The menu contains **About**, **Enable NexU in browser**, **Preferences**, **Show logs**, **Select language**, **Manage keystores** and **Exit**.

**Manage keystores** supports:

- checking PC/SC readers and inserted cards;
- registering JKS, P12 and PFX files without storing passwords;
- opening a registered keystore with the operating-system association;
- removing a NexU registration without deleting the original file.

## Signing key sources

NexU supports:

- smart cards through PC/SC, minidriver/KSP or vendor PKCS#11 middleware;
- Windows certificate-store keys;
- JKS files (`.jks`);
- PKCS#12 files (`.p12`, `.pfx`).

A file keystore must contain a private-key entry and certificate chain. NexU stores only the keystore type and path, never its password.

## Local endpoints and browser integration

NexU binds to loopback interfaces only.

| Protocol | Default endpoint | Purpose |
| --- | --- | --- |
| HTTP | `http://127.0.0.1:9795/nexu-info` | Legacy endpoint and diagnostics |
| HTTPS | `https://localhost:9895/nexu-info` | Secure browser integration |

```properties
binding_ip=127.0.0.1
binding_ports=9795
binding_ports_https=9895
nexu_hostname=localhost
```

## Automatic localhost HTTPS

NexU creates the following files beside the active logs directory:

```text
config/HTTPS.txt
config/localhost.crt
config/localhost.key
```

When both certificate and key are absent, NexU generates a unique per-installation RSA certificate containing SAN entries for `localhost` and `127.0.0.1`. It never overwrites operator-provided or partial TLS material. The legacy name `localhost.cer` remains supported.

## Build and native packages

```bash
mvn clean package
java -jar nexu-app/target/nexu-app.jar
```

Native packages must be built on their target operating system because JavaFX contains platform-specific libraries.

Windows:

```powershell
./nexu-app/src/jpackage/package-windows.ps1 `
  -JarPath nexu-app/target/nexu-app.jar `
  -Destination nexu-app/target/jpackage `
  -AppVersion 1.25.0
```

Linux:

```bash
bash nexu-app/src/jpackage/package-linux.sh \
  nexu-app/target/nexu-app.jar \
  nexu-app/target/jpackage \
  1.25.0
```

Packages include a private runtime and verified `nexu-force-stop.bat` or `nexu-force-stop.sh` helper.

## APIs

Protocol identifier: `nexu:1.25`.

| Method | Endpoint | Purpose |
| --- | --- | --- |
| `GET` | `/v1/status` | Version and capabilities |
| `POST` | `/v1/signing-certificate` | Select a certificate and return a local key handle |
| `POST` | `/v1/sign` | Sign a prepared Base64 digest |

Modern `/v1/**` browser calls require an explicit origin allowlist:

```properties
cors_allowed_origin=https://sign.example.org
```

Legacy integrations may continue using `/nexu-info`, `/nexu.js`, `/rest/certificates`, `/rest/sign` and `/rest/logout`.

## Configuration and logs

External configuration lookup starts with `-Dnexu.config.file`, then `NEXU_CONFIG_FILE`, the launcher/application directory, current directory, JAR directory and embedded defaults.

Important properties:

```properties
cors_allowed_origin=*
close_token=true
cache_time_to_live_ms=10000
enable_systray_menu=true
systray_backend=awt
replace_existing_nexu=true
log_level=DEBUG
```

Default portable log: `logs/nexu.log`. Archived files use `logs/archive/nexu.YYYY-MM-DD.N.log.gz`.

## Security principles

- Private keys remain inside their smart card, operating-system provider or local keystore.
- PINs and passwords remain in the trusted local application and are not logged.
- The local server binds only to loopback interfaces.
- The remote backend must independently validate certificate trust, purpose, algorithms and the resulting signature.
- The localhost TLS key is generated locally per installation and is not distributed as a shared key.

## License

NexU is distributed under the **European Union Public Licence, version 1.2 (EUPL-1.2)**. See [`LICENSE`](LICENSE).

Third-party notices are recorded in [`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md).
