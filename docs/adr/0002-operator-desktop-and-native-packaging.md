# ADR 0002: JavaFX operator application and native packaging

- Status: Accepted
- Date: 2026-07-16
- Scope: Windows and Linux operator distribution

## Context

NexU is a local smart-card agent used by an operator. It needs a trusted local
surface for certificate selection, PIN interaction, reader diagnostics and
application lifecycle. The application must be installable without requiring
the operator to install a separate Java runtime.

Web eID is an architectural reference for protocol separation and native
messaging, but its native application is implemented with C++ and Qt rather
than Electron. Replacing the current UI with Electron would add Chromium and
Node.js while retaining the Java/Spring Boot process or requiring a full
cryptographic-core rewrite.

## Decision

### Desktop toolkit

NexU keeps JavaFX as its desktop toolkit and upgrades the executable runtime to
JavaFX 21.0.11.

JavaFX is confined to `nexu-app`. The cryptographic core, card drivers and local
Spring Boot server must remain headless and testable without initializing the
JavaFX toolkit.

The transitional modules may still compile against the historical parent
baseline while they are consolidated, but the final executable package must
contain only the platform-specific JavaFX 21 runtime. CI rejects JavaFX 11
artifacts in the Boot JAR.

### Operator deliverables

The supported operator deliverables are:

- Windows portable ZIP with `NexU.exe` and a private Java runtime;
- Windows per-user EXE installer with Start menu entry and desktop shortcut;
- Linux portable `tar.gz` with a launcher and private Java runtime;
- Debian/Ubuntu `.deb` package with an application-menu entry;
- executable Spring Boot JAR for diagnostics and managed deployments.

The Windows installer uses a stable upgrade UUID so later releases can upgrade
the existing installation rather than appearing as unrelated products.

The application is built on each target operating system. Cross-platform
re-use of a Boot JAR is prohibited because JavaFX native libraries are
platform-specific.

### Smart-card middleware

The NexU package includes the application and Java runtime, not arbitrary
proprietary smart-card drivers.

- Windows uses the operating-system smart-card stack and certificate store
  first. Vendor middleware is installed only when required.
- Linux uses the system PC/SC service and reader packages. The `.deb` package
  does not embed `pcscd`, kernel/udev configuration or proprietary middleware.
- A future assisted installer may offer official middleware only after explicit
  consent, publisher/signature verification and SHA-256 verification against a
  curated catalog.

### Protocol direction

The Spring Boot loopback API remains available for NexU compatibility. Native
messaging may be introduced later for the modern protocol, following the Web
eID separation between browser integration and the trusted local process. It
does not require replacing JavaFX.

## Consequences

- Operators launch a normal native executable and do not manage Java.
- The application retains the existing Java/DSS/card implementation while the
  old Maven modules are consolidated.
- Installer and JavaFX version checks are part of CI.
- Electron and a Qt/C++ rewrite are not part of the current modernization.
- Driver diagnostics and guided installation remain separate from the UI
  toolkit choice.
