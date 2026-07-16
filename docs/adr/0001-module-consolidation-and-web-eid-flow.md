# ADR 0001: Two-module layout and Web eID-aligned boundaries

- Status: Accepted and implemented
- Date: 2026-07-16
- Scope: NexU desktop modernization with Spring Boot, JavaFX and DSS 6.4

## Context

The historical project used many small Maven modules for Jetty plugins, public
models, packaging assemblies, smart-card adapters and desktop launchers. Most of
those boundaries no longer represented independent deployable components after
the move to a Spring Boot loopback server and native `jpackage` distributions.

NexU is a small local application, but it still has one meaningful dependency
boundary: the signing/card engine must not depend on the Spring Boot or desktop
application lifecycle.

The Web eID projects are used as a reference for security-flow separation. The
important property is not their exact build layout; it is the separation between
local private-key operations and remote challenge, trust and document
validation.

## Decision

### Maven modules

The repository has exactly two Maven modules:

1. `nexu-core`
   - public API and domain models;
   - shared utilities;
   - DSS 6.4 signing operations, including `signDigest` for prepared hashes;
   - PC/SC, PKCS#11 and Windows keystore integration;
   - card, reader and middleware discovery.

2. `nexu-app`
   - Spring Boot loopback server;
   - modern `/v1/**` protocol and legacy `/rest/**` compatibility;
   - JavaFX operator UI and tray lifecycle;
   - executable Boot JAR;
   - Windows and Linux `jpackage` distributions.

The former modules were physically merged into these directories. Their POMs,
assembly trees, bundled Java runtimes and obsolete Jetty implementations were
deleted. Package boundaries and architecture tests are preferred over creating
additional Maven artifacts for this project size.

### Core boundary

`nexu-core` must not own the Spring Boot server lifecycle or native installer
configuration. Some historical JavaFX controllers still live in the core source
tree and are tracked as migration debt; moving them into `nexu-app` must not
create another Maven module.

Historical tests that depend on DSS 5 APIs, Log4j 1.x fixtures or retired server
implementations are preserved under `nexu-core/src/legacy-test`. They are not
part of the default build. Tests return to `src/test` only after migration to DSS
6.4 and the current test stack.

## Web eID-aligned flows

### Document signing

1. The browser requests a signing certificate from the local NexU agent.
2. The browser sends the certificate to the remote signing backend.
3. The backend prepares the document signature structure and returns the digest
   and digest algorithm.
4. NexU signs the already prepared digest with the card-backed key through DSS
   `SignatureTokenConnection.signDigest(...)`.
5. The backend validates the response and finalizes the signed document.

The prepared digest must never be passed to the historical raw-data signing
method, because that would hash it a second time.

### Authentication

Authentication remains separate from document signing:

1. The remote backend creates a high-entropy, short-lived challenge and stores
   it in the browser session.
2. The local agent signs protocol-defined data bound to both the challenge and
   requesting origin.
3. The backend atomically consumes the challenge.
4. The backend treats the submitted certificate as untrusted and validates the
   chain, purpose, validity, revocation status, algorithm, origin and signature.
5. Only the remote backend creates the authenticated session.

The Web eID authentication-token validation library belongs to the remote web
application or an integration example. It is not embedded in the local desktop
agent as a substitute for server-side validation.

## Smart-card drivers and middleware

NexU distinguishes three layers:

1. operating-system reader transport and PC/SC service;
2. card middleware such as a Windows minidriver/KSP or vendor PKCS#11 library;
3. the NexU adapter that discovers and uses an available provider.

The application diagnoses the missing layer before offering guidance. It may
check the PC/SC service, enumerate readers, inspect ATR data, detect Windows
providers and locate known PKCS#11 libraries.

NexU does not silently download or install arbitrary drivers. Assisted
installation requires:

- an explicit operator action;
- an official allowlisted source;
- SHA-256 verification;
- publisher or operating-system signature verification where available;
- visible vendor, version, source and license information;
- explicit elevation confirmation when administrative rights are needed.

Windows uses the built-in smart-card stack and Windows Update first. Linux uses
the distribution packages for `pcscd`, `libpcsclite` and CCID support. Vendor
middleware remains separate unless redistribution is explicitly permitted.

## Desktop UI

JavaFX remains the trusted operator interface and is packaged with the private
Java runtime. The executable uses Java 17 and JavaFX 21.0.11.

Electron is not adopted because it would add Chromium and Node.js without
removing the Java/DSS process or the operating-system smart-card requirements.
Native messaging may be introduced later for browser integration without
changing the JavaFX UI choice.

## Consequences

- A normal `mvn clean package` builds the complete project.
- The operator receives one executable application, not a collection of module
  artifacts.
- Only `nexu-core` appears as an internal library inside the Boot JAR.
- The old Jetty server, MOCCA adapter, assembly module and bundled Java 8/JavaFX
  11 runtimes are removed.
- Legacy REST compatibility remains temporary and does not define the new
  authentication protocol.
- The next cleanup focus is package-level modernization inside the two modules,
  especially moving remaining UI code out of the core and migrating legacy
  tests.
