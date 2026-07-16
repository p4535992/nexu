# NexU

NexU is a local smart-card agent that lets a web application request certificates and signatures without exposing the private key to the browser or remote server.

This repository is a friendly fork of [`nowina-solutions/nexu`](https://github.com/nowina-solutions/nexu). The modernized application uses Java 17, Spring Boot 3.5.16, DSS 6.4 and JavaFX 21.0.11 while preserving the familiar NexU integration model.

## Current structure

The Maven reactor contains exactly two modules:

- **`nexu-core`** — API, models, utilities, DSS signing, PC/SC, PKCS#11 and Windows keystore support;
- **`nexu-app`** — Spring Boot loopback server, JavaFX operator UI, compatibility endpoints and native packaging.

The former API, model, utility, standalone, server-plugin, bundle and keystore modules were physically merged into these two directories. Obsolete Jetty, MOCCA and manual Java-runtime assembly trees were removed.

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

Operators normally use the native Windows or Linux packages and do not need to install Java separately.

## Architecture

NexU keeps one meaningful library boundary:

```text
Browser
   │
   │ loopback HTTP / future native messaging
   ▼
nexu-app
   ├── Spring Boot local server
   ├── JavaFX trusted operator UI
   ├── legacy /rest compatibility
   └── native packaging
          │
          ▼
      nexu-core
      ├── DSS 6.4 signing
      ├── certificate and key selection
      ├── PC/SC and PKCS#11
      └── Windows certificate store
```

Challenge storage, certificate trust validation, authentication-token validation and document finalization belong to the remote web application, not the local desktop agent.

The detailed decision is recorded in [`docs/adr/0001-module-consolidation-and-web-eid-flow.md`](docs/adr/0001-module-consolidation-and-web-eid-flow.md).

## Web eID-inspired signing flow

The signing flow follows the separation used by Web eID while retaining NexU compatibility:

1. the browser asks the local agent for a signing certificate;
2. the browser sends the certificate to the remote signing backend;
3. the backend prepares the document signature structure and returns a digest plus digest algorithm;
4. NexU signs that prepared digest with the card-backed key;
5. the backend validates the response and finalizes the document.

The prepared digest is signed through DSS `SignatureTokenConnection.signDigest(...)`. It must not be passed to the historical raw-data signing method, which would hash it a second time.

### Modern local API

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

### Browser origins

The local service listens only on loopback. Modern browser requests require an explicit origin allowlist:

```properties
cors_allowed_origin=https://sign.example.org,https://test-sign.example.org
```

The historical wildcard remains available only for legacy compatibility. It is rejected for modern `/v1/**` browser requests.

## Authentication boundary

Authentication is separate from document signing:

1. the remote backend creates a high-entropy, short-lived challenge and stores it in the browser session;
2. the local agent signs protocol-defined data bound to the challenge and requesting origin;
3. the backend atomically consumes the challenge;
4. the backend treats the submitted certificate as untrusted and validates chain, purpose, validity, revocation, algorithm, origin and signature;
5. only the backend creates the authenticated session.

The Web eID validation library belongs to the remote application or an integration example. It is not embedded in NexU as a replacement for server-side validation.

The obsolete `/rest/authenticate` and `/rest/identityInfo` endpoints return HTTP `410 Gone`.

## Legacy compatibility

Existing integrations can continue to use:

- `GET /nexu-info`
- `GET /nexu.js`
- `GET /favicon.ico`
- `POST /rest/certificates`
- `POST /rest/sign`
- `POST /rest/logout`

The compatibility implementation is compiled directly into `nexu-app`; it is not a separate Maven artifact.

## Smart-card drivers and middleware

NexU relies on three layers:

1. the operating-system PC/SC service and reader driver;
2. a card minidriver/KSP or vendor PKCS#11 library when required;
3. the NexU adapter that discovers and uses the provider.

NexU does not silently install arbitrary drivers.

- **Windows:** use the built-in smart-card stack and Windows Update first. Vendor middleware is offered only when required.
- **Linux:** use the distribution packages for `pcscd`, `libpcsclite` and CCID reader support.

On Debian or Ubuntu:

```bash
sudo apt install libpcsclite1 pcscd libccid
```

Any future assisted installer must require explicit consent, an official allowlisted source, SHA-256 verification and publisher-signature verification where available.

## Operator packages

### Windows

The Windows build produces:

- a portable ZIP containing `NexU.exe` and a private Java runtime;
- a per-user EXE installer with Start menu and desktop shortcuts.

```powershell
./nexu-app/src/jpackage/package-windows.ps1 `
    -JarPath nexu-app/target/nexu-app.jar `
    -Destination nexu-app/target/jpackage `
    -AppVersion 1.24.0
```

### Linux

The Linux build produces:

- a portable `tar.gz` application image;
- a Debian/Ubuntu `.deb` package.

```bash
bash nexu-app/src/jpackage/package-linux.sh \
    nexu-app/target/nexu-app.jar \
    nexu-app/target/jpackage \
    1.24.0
```

Packages must be built on the target operating system because JavaFX contains platform-specific native libraries.

## External configuration

Configuration is searched in this order:

1. `-Dnexu.config.file=/path/to/nexu-config.properties`;
2. `NEXU_CONFIG_FILE`;
3. the `jpackage` launcher directory and application-image root;
4. the current working directory;
5. the directory of a directly executed JAR;
6. embedded defaults.

## Security principles

- Private keys never leave the smart card or operating-system key store.
- PIN entry and certificate selection remain in the trusted local application.
- The local service binds only to loopback interfaces.
- Browser origins are validated against an explicit allowlist.
- Signing handles and authentication challenges are short-lived and single-use.
- PINs, hashes, handles and signature material are not written to logs.
- The remote backend independently validates certificate trust, purpose and algorithms.

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
