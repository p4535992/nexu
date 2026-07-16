# ADR 0001: Module consolidation, driver management and Web eID-aligned boundaries

- Status: Accepted for incremental implementation
- Date: 2026-07-16
- Scope: NexU modernization after the Spring Boot and DSS 6.4 migrations

## Context

The historical reactor contains many small modules created around old plugin,
Jetty and packaging boundaries. Spring Boot removes several of those reasons,
but it does not justify collapsing cryptographic, native, protocol and desktop
code into one undifferentiated application.

NexU combines concerns with materially different security and deployment
properties:

- smart-card and operating-system key access;
- a loopback HTTP protocol;
- a trusted desktop UI for certificate selection and PIN entry;
- native and portable packaging;
- remote document finalization and authentication-token validation.

The Web eID authentication-token validation project is the reference for the
authentication flow. Its most important architectural property is the
separation between local signing and remote validation, not its exact Maven
layout.

## Decision

### Target module layout

The project is reduced to **two Maven modules**:

1. `nexu-core`
   - public API, domain models and utilities;
   - DSS-backed raw-data and pre-hashed signing;
   - PKCS#11, PC/SC and Windows keystore integration;
   - reader, card and middleware diagnostics;
   - no Spring Boot server lifecycle or native packaging responsibilities.

2. `nexu-app`
   - Spring Boot loopback server and modern `/v1/**` protocol;
   - temporary `/rest/**` compatibility facade;
   - JavaFX certificate/PIN UI and tray lifecycle;
   - executable Boot JAR and `jlink`/`jpackage` packaging;
   - composes and distributes `nexu-core`.

For this project size, protocol and UI boundaries are enforced by Java packages
and architecture tests rather than additional Maven artifacts. The remote Web
eID-style challenge store and authentication-token validator remain part of
the remote web application, not the local NexU build.

During migration, the old source directories are added as source roots to one
of the two modules. Their POM files are removed immediately. Physical source
moves follow incrementally and must not recreate Maven modules.

### Why retain a separate core module

A single module would make packaging superficially simpler but would couple
cryptographic and card code to Spring Boot and JavaFX. Keeping `nexu-core`
separate allows headless tests, avoids accidental server/UI dependencies in the
signing engine and preserves a reusable boundary for future native messaging.
Two modules are sufficient; further Maven separation would add ceremony without
meaningful isolation.

## Smart-card drivers and middleware

NexU distinguishes three layers:

1. **reader transport** — the operating-system PC/SC service and reader driver;
2. **card middleware** — an OS minidriver/KSP or a vendor PKCS#11 library;
3. **NexU adapter** — Java code that discovers and uses the available provider.

The application must always diagnose before offering installation:

- verify the PC/SC service;
- enumerate connected readers;
- read ATR data when possible;
- detect Windows smart-card providers and known PKCS#11 libraries;
- report the exact missing layer without exposing PINs or APDU payloads.

### Installation policy

NexU will not silently download or install arbitrary drivers. Driver and
middleware installation changes trusted operating-system components and often
requires administrator privileges.

Assisted installation may be added only with all of these controls:

- an explicit user action and elevation confirmation;
- a curated, versioned catalog mapping OS/architecture/card or reader to an
  official vendor or operating-system source;
- HTTPS download from an allowlisted official domain;
- expected SHA-256 digest;
- operating-system code-signature/publisher verification when available;
- visible vendor, version, source and license before installation;
- no silent fallback to mirrors or search-engine results;
- an audit record that excludes PINs, certificates, hashes and signing data.

Platform preference order:

- **Windows:** use the built-in smart-card stack and Windows Update first;
  install vendor middleware only when the card is not exposed through the
  Windows certificate store or a required PKCS#11 provider is missing.
- **Linux:** detect `pcscd`, `libpcsclite` and a CCID reader driver; offer the
  distribution package-manager command with explicit consent rather than
  embedding system libraries in the NexU archive.
- **macOS:** use the system PC/SC/CryptoTokenKit stack; offer vendor middleware
  only for cards that require it.

The first implementation is diagnostics plus official guidance. One-click
installation follows only after a signed catalog format and verification tests
exist.

## Desktop UI decision

JavaFX remains the desktop toolkit because NexU needs a small trusted local UI,
system-tray lifecycle and native packaging on Windows, Linux and macOS.
Replacing it with Electron would add a browser runtime; replacing it with a web
page would move trusted certificate/PIN interaction toward the untrusted
browser boundary; adopting Compose would introduce Kotlin and a second UI
stack without removing native packaging concerns.

JavaFX is restricted to `nexu-app`. Core, drivers and local server remain
headless and must be usable in tests without initializing the JavaFX toolkit.
A diagnostic/headless launch mode may be added for servers, support tools and
CI, but normal certificate selection and PIN interaction stay in the trusted
local application.

The current JavaFX 11.0.2 baseline is obsolete. After the remaining Java 11
modules are moved to the Java 17+ toolchain, JavaFX will be upgraded to the
maintained JavaFX 21 line and packaged with `jlink`/`jpackage`.

## Web eID-aligned authentication flow

Authentication is distinct from document signing.

### Server-side challenge

1. The remote web application generates a cryptographically random nonce with
   at least 256 bits of entropy.
2. The nonce is stored in a browser-session-backed store with a short TTL.
3. The challenge is returned to the browser.
4. Validation retrieves and removes the nonce atomically.

A five-minute maximum TTL is the initial reference value. Production
deployments may use a shorter value.

### Local authentication operation

1. The browser provides the challenge and its ASCII-serialized origin to the
   local NexU agent.
2. NexU selects an authentication certificate, not a document-signing
   certificate.
3. NexU signs the protocol-defined authentication input using the card-backed
   authentication key.
4. NexU returns a versioned token containing only public data and the signature.

The planned token format is `nexu-auth:1.0` and follows the Web eID security
model:

- `unverifiedCertificate`: Base64 DER authentication certificate;
- `algorithm`: explicit JWA-style signature algorithm;
- `signature`: Base64 signature value;
- `format`: token format and version;
- `appVersion`: NexU application identifier/version.

The signed input follows the Web eID field-separation rule: hash the ASCII
origin and challenge independently with the hash function implied by the
selected signature algorithm, then concatenate the fixed-length digests before
signing.

### Remote validation

The remote backend, not the local NexU process, must:

1. atomically consume the session challenge;
2. validate the token format and allowlisted algorithm;
3. reconstruct the signed input from the configured site origin and consumed
   challenge;
4. verify the signature using the submitted certificate public key;
5. treat the submitted certificate as untrusted input;
6. build and validate the chain against explicitly configured trusted
   authorities;
7. validate authentication purpose, key usage, validity and revocation;
8. create the authenticated server session only after all checks succeed.

The configured origin uses the ASCII serialization of an origin, including
Punycode for internationalized host names.

### Document signing remains separate

For document signing:

1. the backend prepares the document signature structure and digest;
2. NexU signs the already prepared digest through DSS `signDigest`;
3. the backend validates the response and finalizes the document.

Authentication challenges must not be reused as document-signing inputs, and
document-signing handles must not be accepted by authentication endpoints.

## Migration order

1. keep DSS 6.4 and the executable `nexu-app` build green;
2. remove obsolete Jetty/REST module boundaries;
3. merge `nexu-api`, `nexu-model` and `nexu-util` into `nexu-core`;
4. extract card/native code into `nexu-card-drivers`;
5. merge server/lifecycle code into `nexu-local-server`;
6. upgrade the modern reactor to one Java 17+ toolchain and JavaFX 21;
7. add driver diagnostics and a signed installation-catalog schema;
8. remove the historical reactor after package and hardware tests pass.

## Consequences

- The modern executable keeps the name `nexu-app`.
- The transitional reactor may temporarily contain more than four modules, but
  every remaining module must map to a named consolidation step.
- The Web eID validation library belongs in a separate backend example or
  integration, not in the local agent as a substitute for server validation.
- Legacy `/rest/**` compatibility remains temporary and does not define the new
  authentication protocol.
