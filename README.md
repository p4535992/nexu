# NexU

NexU is an open-source local smart-card agent that allows a web application to request certificates and signatures from a smart card without exposing the private key to the browser or to the remote server.

This repository is a friendly fork of [`nowina-solutions/nexu`](https://github.com/nowina-solutions/nexu). The modernization keeps the familiar NexU browser API while replacing obsolete infrastructure with Spring Boot, DSS 6.4 and a Web eID-inspired signing and authentication flow.

> **Modernization status:** work in progress on `spring-boot-modernization-2`. The executable application keeps the historical module and artifact name `nexu-app`.

## Goals

- preserve the simple NexU browser experience;
- keep private-key operations on the user's smart card;
- support PKCS#11 middleware and the Windows certificate store;
- provide an executable JAR, portable desktop packages and native installers with a bundled Java runtime;
- replace the legacy embedded Jetty and assembly-based packaging;
- reduce the historical Maven reactor to a small set of meaningful modules;
- isolate smart-card drivers, HTTP, trusted UI and packaging concerns;
- provide explicit origin checks, short-lived operations and replay protection;
- remain compatible with existing DSS integrations during migration.

## Architecture

The target architecture separates four local concerns:

1. **Core** — protocol-independent domain objects and DSS-backed signing operations.
2. **Card drivers** — PC/SC, PKCS#11 and Windows certificate-store integration.
3. **Local server** — Spring Boot loopback API, including temporary legacy `/rest/**` compatibility.
4. **Application** — JavaFX trusted UI, tray lifecycle, executable Boot JAR and native packaging.

The remote web application remains responsible for challenge storage and validation, certificate trust, document-signature preparation and finalization.

The detailed decision is recorded in [`docs/adr/0001-module-consolidation-and-web-eid-flow.md`](docs/adr/0001-module-consolidation-and-web-eid-flow.md).

## Web eID-inspired flow

The modernization follows the separation used by the Web eID projects while retaining NexU usability.

### Document signing

1. The browser asks the local NexU agent for the signing certificate and supported hash functions.
2. The browser sends the selected certificate to the remote signing application.
3. The remote application prepares the signature structure and returns a hash plus its hash function.
4. The browser asks the local NexU agent to sign that already prepared hash with the selected smart-card key.
5. The browser sends the signature value and signature algorithm to the remote application.
6. The remote application validates the response and finalizes the signed document.

The pre-hashed path calls DSS `SignatureTokenConnection.signDigest(...)`. It does not pass the hash through the historical `sign(ToBeSigned, ...)` method, because that would hash the value a second time and produce an invalid signature.

Conceptually, the modern API maps to the Web eID operations:

- `status`
- `getSigningCertificate`
- `sign(certificate, hash, hashFunction)`

The legacy NexU JavaScript functions remain a compatibility facade during migration.

### Modern local API

The first modern protocol version is `nexu:2.0`:

| Method | Endpoint | Purpose |
| --- | --- | --- |
| `GET` | `/v1/status` | Returns the NexU version, protocol version and supported capabilities. |
| `POST` | `/v1/signing-certificate` | Selects a signing certificate and returns its certificate data, supported hash functions and an opaque local `keyHandle`. |
| `POST` | `/v1/sign` | Signs a Base64-encoded, already prepared hash using the selected `keyHandle`. |
| `GET` | `/nexu-v2.js` | Promise-based browser client implemented with `fetch`, without jQuery. |

Minimal browser flow:

```html
<script src="http://127.0.0.1:9795/nexu-v2.js"></script>
<script>
async function signPreparedHash(hash, hashFunction) {
    const certificate = await NexU.getSigningCertificate({
        certificatePurpose: "SIGNATURE"
    });

    // The remote application prepares and returns hash/hashFunction.
    return NexU.sign(certificate, hash, hashFunction, {
        clearToken: true
    });
}
</script>
```

`keyHandle` contains only local opaque identifiers. It is not a private key and is meaningful only to the running NexU process.

### Browser origin configuration

The local service listens only on loopback. Modern `/v1` browser requests additionally require an explicit origin allowlist:

```properties
cors_allowed_origin=https://sign.example.org,https://test-sign.example.org
```

The historical value `cors_allowed_origin=*` remains available for legacy `/rest/**` compatibility, but it is rejected for browser requests to `/v1/**`.

### Authentication

Authentication is intentionally separate from document signing and follows the Web eID trust boundary:

1. the remote backend creates a high-entropy, short-lived challenge and stores it in the browser session;
2. the local agent signs data bound to the challenge and ASCII-serialized requesting origin;
3. the backend atomically consumes the challenge;
4. the backend treats the submitted certificate as untrusted and validates chain, purpose, validity, revocation, algorithm, origin and signature;
5. authentication succeeds only after all server-side checks pass.

The old `/rest/authenticate` and `/rest/identityInfo` endpoints are retired and respond with HTTP `410 Gone`.

## Legacy protocol compatibility

The Spring Boot server preserves the endpoints required by existing signing clients:

- `GET /nexu-info`
- `GET /nexu.js`
- `GET /favicon.ico`
- `POST /rest/certificates`
- `POST /rest/sign`
- `POST /rest/logout`

The compatibility plugin is compiled directly into `nexu-app`; it is no longer a separate Maven artifact. Legacy requests are not logged with complete payloads, hashes, challenges or signing material.

## Smart-card drivers and middleware

NexU needs access to the operating-system smart-card stack and, for some cards, vendor middleware:

- **Reader transport:** PC/SC service plus a reader/CCID driver.
- **Card middleware:** Windows minidriver/KSP or a vendor PKCS#11 library.
- **NexU:** detects and uses the available provider; it does not replace the operating-system driver.

The planned driver assistant first diagnoses the missing layer by checking the PC/SC service, connected readers, ATR data, Windows providers and known PKCS#11 libraries.

NexU will not silently download or install arbitrary drivers. Assisted installation requires explicit user consent, an official allowlisted vendor/OS source, SHA-256 verification and operating-system publisher-signature verification where available.

Platform preference:

- **Windows:** use the built-in smart-card stack and Windows Update first; offer vendor middleware only when necessary.
- **Linux:** detect `pcscd`, `libpcsclite` and a CCID driver, then offer the distribution package-manager command with explicit consent.
- **macOS:** use the system PC/SC/CryptoTokenKit stack and install vendor middleware only for cards that require it.

On Debian or Ubuntu the generic PC/SC runtime is normally installed with:

```bash
sudo apt install libpcsclite1 pcscd libccid
```

Vendor-specific PKCS#11 libraries remain separate, licensed software and are never bundled without explicit redistribution permission.

## JavaFX decision

JavaFX remains the UI toolkit because NexU needs a small trusted local certificate/PIN interface, system-tray lifecycle and native desktop packaging. The operator executable now uses JavaFX 21.0.11 on Java 17.

The protocol and signing engine remain usable headlessly; UI classes that still physically reside under the consolidated core source tree will move into `nexu-app` without creating another Maven module. Replacing JavaFX with Electron would add a browser runtime, while moving PIN or certificate selection to an ordinary web page would weaken the trusted local boundary.

## Removed and deprecated components

The modernization deliberately removes or retires components that no longer provide a maintainable path forward:

- **MOCCA integration** — removed because the required artifacts are no longer distributed and the adapter was non-functional;
- **raw-challenge legacy authentication** — retired in favour of an origin-bound, one-time challenge protocol;
- **incomplete identity-info flow** — retired;
- **Java Applet assumptions** — obsolete and unsupported by modern browsers;
- **legacy embedded Jetty server** — replaced by Spring Boot;
- **Jetty multi-user request processor** — removed from the modern runtime;
- **manual JRE/OpenJFX assembly** — replaced by `jlink`/`jpackage`;
- **Log4j 1.2 binary** — excluded; Reload4j temporarily provides the old API while Spring Boot uses SLF4J 2 and Logback.

## Modules and build

The project now has **two Maven modules**:

- `nexu-core` — consolidated public API, models, utilities, DSS integration,
  smart-card operations, PKCS#11/PCSC support and Windows keystore support;
- `nexu-app` — Spring Boot loopback server, legacy REST compatibility,
  JavaFX operator UI, executable application and native packaging.

The old module directories are temporary source containers only. Their POM
files are removed and they do not participate in the Maven reactor. Source
files will be moved physically into the two modules in smaller follow-up
commits without changing the published artifacts.

Build everything with Java 17:

```bash
mvn clean package
```

The resulting executable JAR is:

```text
nexu-app/target/nexu-app.jar
```

Run it directly for diagnostics with:

```bash
java -jar nexu-app/target/nexu-app.jar
```

Operators normally use the Windows or Linux native packages and do not need to
install a separate Java runtime.

## Native and portable packages

### Linux portable package

```bash
bash nexu-app/src/jpackage/package-linux.sh \
    nexu-app/target/nexu-app.jar \
    nexu-app/target/jpackage \
    1.24.0
```

This creates an application image and a compressed portable archive under `nexu-app/target/jpackage`.

### Windows portable package and EXE installer

Run from PowerShell on Windows with Java 17 and WiX Toolset available:

```powershell
./nexu-app/src/jpackage/package-windows.ps1 `
    -JarPath nexu-app/target/nexu-app.jar `
    -Destination nexu-app/target/jpackage `
    -AppVersion 1.24.0
```

The script creates a portable ZIP and a native Windows EXE installer. Windows packages must be built on Windows so the Boot JAR contains Windows JavaFX native libraries; Linux packages must likewise be built on Linux.

Each application image contains `LICENSE`, `THIRD_PARTY_NOTICES.md`, the historical licence directory and an editable `nexu-config.properties` template.

## External configuration

External properties override embedded defaults and are searched in this order:

1. `-Dnexu.config.file=/path/to/nexu-config.properties`;
2. `NEXU_CONFIG_FILE`;
3. the `jpackage` launcher directory and application-image root;
4. the current working directory;
5. the directory of a directly executed JAR.

An explicit path that does not exist is treated as an error.

## WAR scope

The local smart-card agent should not be deployed as a remote WAR. A remote application server cannot access the card connected to the end user's computer.

A separate server-side example may be packaged as a WAR for challenge issuance, Web eID-style authentication-token validation, DSS signature preparation and signature finalization. The local agent remains a desktop process.

## Security principles

- The private key never leaves the smart card or operating-system key store.
- PIN entry and certificate selection belong to the trusted local application.
- The local API listens only on loopback interfaces.
- Browser origins are validated against an explicit allowlist.
- Signing and authentication operations use short-lived, single-use identifiers.
- The server validates certificate trust, purpose and algorithms independently of client claims.
- Authentication challenges are bound to the browser session and consumed once.
- PINs, hashes, token handles and signature material are not written to logs.

## Web eID references

The protocol and server-side separation are informed by these open-source projects maintained by the Estonian Information System Authority:

- [`web-eid/web-eid-spring-boot-example`](https://github.com/web-eid/web-eid-spring-boot-example) — archived Spring Boot example for authentication and document signing;
- [`web-eid/web-eid-authtoken-validation-java`](https://github.com/web-eid/web-eid-authtoken-validation-java) — maintained Java challenge-generation and authentication-token validation library.

NexU is not an official Web eID implementation. These projects are architectural and security references while NexU retains its compatibility API and middleware support.

## License

NexU is distributed under the **European Union Public Licence, version 1.2 (EUPL-1.2)**. See [`LICENSE`](LICENSE).

The Web eID reference projects are distributed under the **MIT License**. Complete attribution and third-party notices are available in [`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md).

## Credits

NexU originated at [Nowina Solutions](https://github.com/nowina-solutions/nexu). This fork includes contributions and ideas from the wider NexU community.

The modernization additionally acknowledges the Web eID maintainers for publishing reusable protocol documentation, examples and validation components under open-source licences.
