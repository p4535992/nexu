# NexU

NexU is a local signing agent that allows web applications to request certificates and electronic signatures without exposing signing private keys to the browser or a remote server.

This repository is a community-maintained fork of [`nowina-solutions/nexu`](https://github.com/nowina-solutions/nexu). It modernizes the original integration model with Java 17, Spring Boot 3.5.16, DSS 6.4 and JavaFX 21.0.11 while preserving the legacy browser endpoints used by existing signing applications.

## Project scope, technology transition and safety notice

Digital identity and electronic-signature ecosystems are evolving. In the European Union, the European Digital Identity Framework and the European Digital Identity Wallet are introducing mobile-wallet and remote-signing alternatives that can reduce dependence on physical smart cards in many future services. This does not mean that smart cards are already obsolete in every country, organisation or regulated workflow, and migration timelines vary.

Official references:

- [European Digital Identity Wallet — European Commission](https://digital-strategy.ec.europa.eu/en/factpages/european-digital-identity-wallet)
- [Regulation (EU) 2024/1183 establishing the European Digital Identity Framework — EUR-Lex](https://eur-lex.europa.eu/eli/reg/2024/1183/oj)

NexU remains available for organisations and users that still need browser-mediated local signing with smart cards, the Windows certificate store, JKS files or PKCS#12 keystores, including existing integrations that cannot yet move to wallet-based or remote-signing solutions.

NexU is free and open-source software distributed under EUPL-1.2 and maintained on a best-effort community basis. It is not a commercial support service, a qualified trust service, a security certification or a guarantee that every card, driver, browser, operating system, keystore or remote signing application will behave correctly. Defects, security issues and environment-specific incompatibilities may remain despite testing and review.

Before using NexU for production, legal, financial, regulated or otherwise sensitive signatures:

1. review the source code, configuration and dependencies according to your risk level;
2. test the complete workflow in a non-production environment with the actual cards, middleware, keystores, browsers and signing backend;
3. independently validate the resulting signed documents and certificate chains;
4. protect keystores and backups, apply least-privilege permissions, and monitor logs and project updates;
5. obtain an independent security, compliance and legal assessment when required.

The software is provided **as is**, without warranties, under EUPL-1.2 and applicable law. Users are responsible for deciding whether it is suitable for their environment and for testing it before deployment. The maintainers and contributors cannot guarantee that every defect has been identified. The [`LICENSE`](LICENSE) text controls if this notice and the licence differ. This notice is not legal or security advice.

## Test NexU with the European Commission DSS demo

Use the European Commission DSS WebApp Demo to test the complete browser-signing workflow:

<https://ec.europa.eu/digital-building-blocks/DSS/webapp-demo/sign-a-document>

### Prepare NexU and the browser

1. Start NexU and wait for its notification-area icon.
2. Verify HTTP: <http://127.0.0.1:9795/nexu-info>.
3. Verify HTTPS: <https://localhost:9895/nexu-info>.
4. If the browser reports that the localhost certificate is untrusted, confirm that the address is exactly `https://localhost:9895/nexu-info`, then use the browser's standard advanced/continue procedure.
5. The same instructions are available through **Enable NexU in browser** in the NexU tray menu. If browser policy prevents acceptance, ask an administrator to trust `config/localhost.crt`.
6. Reload the DSS page after the local HTTPS endpoint opens without a certificate warning.

The generated certificate applies only to the local NexU endpoint. Never disable certificate validation globally or for unrelated websites.

### Sign a document step by step

1. Open **Sign a document** and upload a test file.
2. Choose the desired signature format and options.
3. Start signing.
4. In **Signature Mean Selection**, choose:
   - a detected smart card;
   - the Windows certificate store;
   - a registered JKS, P12 or PFX keystore;
   - **New keystore** for an unregistered local file.
5. Enter the smart-card PIN or keystore password when NexU requests it.
6. Select the signing certificate.
7. Confirm private-key access when requested.
8. Wait for the DSS backend to finalize the document.
9. Download and independently validate the signed document.

A successful legacy integration calls:

```text
POST https://localhost:9895/rest/certificates
POST https://localhost:9895/rest/sign
```

If developer tools show `https://127.0.0.1:9895`, install a current build or set `nexu_hostname=localhost`.

### Two-stage DSS signing flow

The DSS demo uses two local operations:

1. **Certificate discovery** — `/rest/certificates` opens the selected signing source and reads available certificates.
2. **Private-key signing** — after the remote DSS backend prepares the digest, `/rest/sign` unlocks the selected private key and signs that digest locally.

The certificate is sent to DSS, but the private key, PIN and keystore password remain inside NexU and are never sent to the website.

### Test with one password prompt

The default is:

```properties
close_token=true
```

NexU closes the token after certificate discovery, so a local keystore can produce two contextual prompts:

- **Keystore certificate access** — opens the file and reads certificates;
- **Private-key signing** — reopens the file and unlocks the selected key.

For one combined local-keystore prompt, use:

```properties
close_token=false
cache_time_to_live_ms=60000
```

Restart NexU after changing the configuration. The password is cached only in process memory for the configured period and is not written to disk. Use the shortest duration that reliably covers digest preparation. Smart-card middleware may still enforce separate PIN prompts.

## Highlights

- Java 17 and a two-module Maven reactor.
- Spring Boot loopback server with legacy `/rest` and modern `/v1` APIs.
- HTTP on `9795` and HTTPS on `9895` by default.
- Per-installation self-signed localhost certificate generated on first start.
- Smart cards, Windows certificate store, JKS and PKCS#12 signing sources.
- Windows and Linux native packages with a private Java runtime.
- English and Italian JavaFX interface and notification-area menu.
- Rotating diagnostic logs and verified shutdown helpers.

## Signing key sources

NexU supports:

- smart cards through PC/SC, minidriver/KSP or vendor PKCS#11 middleware;
- Windows certificate-store keys;
- JKS files (`.jks`);
- PKCS#12 files (`.p12`, `.pfx`).

A file keystore must contain a private-key entry and certificate chain. NexU stores only a registered keystore's type and path, never its password.

### Register or manage a local keystore

During signing, choose **New keystore**, select JKS or PKCS#12, choose the file, enter its password, select a certificate and choose **Remember** when requested.

The tray-menu **Manage keystores** panel provides:

- **Add smart card** — checks PC/SC service, readers and inserted cards;
- **Add local keystore** — registers a JKS, P12 or PFX file;
- **Open keystore file** — opens the selected file with the operating-system association;
- **Remove** — removes the registration without deleting the original file.

Keep keystore files in a user-protected directory and maintain a secure backup.

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

The tray menu contains **About**, **Enable NexU in browser**, **Preferences**, **Show logs**, **Select language**, **Manage keystores** and **Exit**. The browser-enablement panel opens the exact configured localhost HTTPS endpoint but never installs certificates silently or disables browser security checks.

## Automatic localhost HTTPS

NexU creates the following files beside the active `logs` directory:

```text
config/HTTPS.txt
config/localhost.crt
config/localhost.key
```

When both certificate and key are absent, NexU generates a unique per-installation RSA certificate containing SAN entries for `localhost` and `127.0.0.1`. It never overwrites operator-provided or partial TLS material. The legacy name `localhost.cer` remains supported.

The private key `localhost.key` must remain local and protected. `localhost.p12`, when present, is optional diagnostic/import material and is unrelated to a user's PKCS#12 signing keystore.

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
  -AppVersion 1.24.0
```

Linux:

```bash
bash nexu-app/src/jpackage/package-linux.sh \
  nexu-app/target/nexu-app.jar \
  nexu-app/target/jpackage \
  1.24.0
```

Packages include a private runtime and verified `nexu-force-stop.bat` or `nexu-force-stop.sh` helper. The helpers verify `/nexu-info` before terminating a listener.

## APIs

Modern protocol identifier: `nexu:2.0`.

| Method | Endpoint | Purpose |
| --- | --- | --- |
| `GET` | `/v1/status` | Version and capabilities |
| `POST` | `/v1/signing-certificate` | Select certificate and return an opaque handle |
| `POST` | `/v1/sign` | Sign a prepared Base64 digest |
| `GET` | `/nexu-v2.js` | Promise-based browser client |

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

Rotation defaults:

- maximum file size: 10 MB;
- retained periods: 14;
- total archive cap: 200 MB.

**Show logs** displays the current path and opens it with the operating-system association, falling back to Notepad on Windows when `.log` has no association.

## Security principles

- Private keys remain inside their smart card, operating-system provider or local keystore.
- PINs and passwords remain in the trusted local application and are not logged.
- The local server binds only to loopback interfaces.
- The remote backend must independently validate certificate trust, purpose, algorithms and the resulting signature.
- The localhost TLS key is generated locally per installation and is not distributed as a shared key.

## License

NexU is distributed under the **European Union Public Licence, version 1.2 (EUPL-1.2)**. See [`LICENSE`](LICENSE).

Official English text:

<https://interoperable-europe.ec.europa.eu/sites/default/files/custom-page/attachment/2020-03/EUPL-1.2%20EN.txt>

Third-party notices are recorded in [`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md).
