# ADR 0001: Module consolidation and Web eID-aligned security boundaries

- Status: Proposed
- Date: 2026-07-16
- Scope: NexU modernization after the Spring Boot migration

## Context

The historical reactor contains many small modules created around implementation details and old plugin boundaries. Spring Boot removes the need for a separate embedded-server implementation, but it does not justify collapsing the whole application into a single module.

NexU combines concerns with materially different security and deployment properties:

- smart-card and operating-system key access;
- a loopback HTTP protocol;
- a trusted desktop UI for certificate selection and PIN entry;
- native and portable packaging;
- remote document finalization and authentication-token validation.

The Web eID authentication-token validation project is used as the reference for the authentication flow. Its most important architectural property is not its package layout, but the separation between the client-side signing operation and server-side validation.

## Decision

### Target module layout

After the DSS 6.4 migration is stable, the modernization reactor will be reduced to five logical modules:

1. `nexu-core`
   - domain models and public API;
   - certificate filtering and token abstractions;
   - signing operations built on DSS;
   - no Spring, Jakarta Servlet, JavaFX or packaging dependencies.
   - absorbs the current `nexu-api`, `nexu-model`, `nexu-util` and `nexu-public-object-model` modules.

2. `nexu-card-drivers`
   - PKCS#11, PC/SC and MSCAPI adapters;
   - operating-system detection and native integration;
   - absorbs card-specific code currently spread across `nexu-core` and `nexu-windows-keystore-plugin`.

3. `nexu-local-server`
   - Spring Boot loopback server;
   - modern `/v1/**` protocol;
   - temporary `/rest/**` compatibility facade;
   - origin allowlist, request validation and protocol DTOs;
   - absorbs `nexu-spring-boot-server` and `nexu-rest-plugin`.

4. `nexu-desktop-app`
   - JavaFX, tray integration, configuration and lifecycle;
   - trusted certificate/PIN UI;
   - composes the core, drivers and local server;
   - absorbs `nexu-standalone`, `nexu-app`, `nexu-modern-app` and `nexu-multi-user-support`.

5. `nexu-distribution`
   - jlink/jpackage scripts and installer metadata;
   - platform-specific packaging verification;
   - replaces the historical `nexu-bundle` and the obsolete HTTPS assembly path.

The reduction will be performed through package moves and dependency-direction tests, not by using Maven source-directory aggregation as a permanent solution.

### Why not a single Spring Boot module

A single module would make packaging superficially simpler but would weaken important boundaries:

- the card engine should be testable without starting Spring;
- JavaFX must not leak into protocol or cryptographic code;
- native platform dependencies should remain isolated;
- remote authentication validation must never become an implicit responsibility of the local desktop agent;
- the loopback protocol needs independent compatibility and security tests.

Five modules are a reduction from the current reactor while retaining boundaries that have security or deployment value.

## Web eID-aligned authentication flow

Authentication is distinct from document signing.

### Server-side challenge

1. The remote web application generates a cryptographically random nonce with at least 256 bits of entropy.
2. The nonce is stored in a browser-session-backed store with a short TTL.
3. The challenge is returned to the browser.
4. The store exposes a consume-once operation: validation retrieves and removes the nonce atomically.

A five-minute maximum TTL is the initial reference value, matching the Web eID Spring Boot example. Production deployments may use a shorter value.

### Local authentication operation

1. The browser provides the challenge and its ASCII-serialized origin to the local NexU agent.
2. NexU selects an authentication certificate, not a document-signing certificate.
3. NexU signs the protocol-defined authentication input using the card-backed authentication key.
4. NexU returns a versioned token containing only public data and the signature.

The planned token format is `nexu-auth:1.0` and follows the Web eID security model:

- `unverifiedCertificate`: Base64 DER authentication certificate;
- `algorithm`: explicit JWA-style signature algorithm;
- `signature`: Base64 signature value;
- `format`: token format and version;
- `appVersion`: NexU application identifier/version.

The signed input follows the Web eID field-separation rule: hash the ASCII origin and challenge independently with the hash function implied by the selected signature algorithm, then concatenate the fixed-length digests before signing.

### Remote validation

The remote backend, not the local NexU process, must:

1. atomically consume the session challenge;
2. validate the token format and allowlisted algorithm;
3. reconstruct the signed input from the configured site origin and consumed challenge;
4. verify the signature using the submitted certificate public key;
5. treat the submitted certificate as untrusted input;
6. build and validate the chain against explicitly configured trusted intermediate/root authorities;
7. validate authentication purpose, key usage, validity period and revocation status;
8. create the authenticated server session only after all checks succeed.

The configured origin must use the ASCII serialization of an origin, including Punycode for internationalized host names.

### Document signing remains separate

For document signing:

1. the backend prepares the document signature structure and digest;
2. NexU signs the already prepared digest through DSS `signDigest`;
3. the backend validates the response and finalizes the document.

Authentication challenges must not be reused as document-signing inputs, and document-signing handles must not be accepted by authentication endpoints.

## DSS 6.4 migration order

DSS is upgraded before physical module consolidation so compile and runtime regressions can be attributed correctly.

1. switch the managed DSS version to 6.4;
2. compile production sources and adapt changed DSS APIs;
3. restore focused tests for certificate selection, raw-data signing and pre-hashed signing;
4. verify PKCS#11 and MSCAPI construction paths;
5. verify the executable JAR and jpackage artifacts;
6. begin module moves only after the DSS build is green.

## Consequences

- The next pull request focuses on DSS 6.4 and boundary documentation, not mass file moves.
- A later pull request will merge modules incrementally while preserving package-level compatibility where useful.
- The Web eID validation library may be used by a separate backend example or integration module, but it will not be bundled into the local agent as a substitute for server-side validation.
- Legacy `/rest/**` compatibility remains temporary and must not define the new authentication protocol.
