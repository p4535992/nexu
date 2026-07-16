# NexU

NexU is an open-source local smart-card agent that allows a web application to request certificates and signatures from a smart card without exposing the private key to the browser or to the remote server.

This repository is a friendly fork of [`nowina-solutions/nexu`](https://github.com/nowina-solutions/nexu). The current modernization work keeps the familiar NexU browser API while replacing obsolete infrastructure with a modular Spring Boot architecture and a Web eID-inspired signing flow.

> **Modernization status:** work in progress. The historical application remains available, while the new server adapter and packaging are developed incrementally on the modernization branch.

## Goals

- preserve the simple NexU browser experience;
- keep private-key operations on the user's smart card;
- support PKCS#11 middleware and the Windows certificate store;
- provide modular JAR, WAR and portable desktop distributions;
- replace the legacy embedded Jetty and assembly-based packaging;
- isolate the smart-card engine from HTTP, UI and packaging concerns;
- provide explicit origin checks, short-lived operations and replay protection;
- remain compatible with existing DSS integrations during migration.

## Architecture

The target architecture separates three responsibilities:

1. **Local smart-card agent** — detects certificates and performs card-backed signatures through PKCS#11 or MSCAPI.
2. **Spring Boot protocol adapter** — exposes the local loopback HTTP API and delegates operations to the smart-card engine.
3. **Remote signing application** — prepares the data to sign, receives the resulting signature and finalizes the document with DSS or another signing library.

The Spring Boot adapter implements the existing `lu.nowina.nexu.HttpServer` contract, allowing the HTTP layer to be replaced without rewriting all card, PIN and certificate-selection logic at once.

## Web eID-inspired flow

The modernization follows the separation used by the Web eID projects while retaining NexU usability.

### Document signing

1. The browser asks the local NexU agent for the signing certificate and supported signature algorithms.
2. The browser sends the selected certificate to the remote signing application.
3. The remote application prepares the signature structure and returns a hash plus its hash function.
4. The browser asks the local NexU agent to sign that hash with the selected smart-card key.
5. The browser sends the signature value and signature algorithm to the remote application.
6. The remote application validates the response and finalizes the signed document.

Conceptually, the modern API maps to the Web eID operations:

- `status`
- `getSigningCertificate`
- `sign(certificate, hash, hashFunction)`

The legacy NexU JavaScript functions remain a compatibility façade during migration, including certificate discovery and token-based signing calls already used by DSS demonstrations.

### Authentication

Authentication is intentionally separate from document signing. A future authentication module will use the Web eID model:

1. the server creates and stores a one-time cryptographic challenge;
2. the local agent signs data bound to both the challenge and the requesting origin;
3. the server validates the certificate chain, certificate purpose and token signature;
4. the challenge is removed after use to prevent replay attacks.

Authentication certificates received from the client must be treated as untrusted until server-side validation succeeds.

## Legacy protocol compatibility

The Spring Boot adapter is designed to preserve the endpoints expected by existing NexU clients:

- `GET /nexu-info`
- `GET /nexu.js`
- `GET /favicon.ico`
- plugin routes such as `POST /rest/certificates` and `POST /rest/sign`

The local service binds to the loopback interface. Production integrations should use an explicit origin allowlist rather than permissive `Access-Control-Allow-Origin: *` configuration.

## Removed and deprecated components

The modernization deliberately removes or retires components that no longer provide a maintainable path forward:

- **MOCCA integration** — removed from the modernized runtime because the required artifacts are no longer distributed and the adapter was already non-functional;
- **Java Applet assumptions** — obsolete and unsupported by modern browsers;
- **legacy embedded Jetty server** — replaced incrementally by Spring Boot;
- **manual JRE/OpenJFX bundle assembly** — to be replaced by `jlink` and `jpackage` distributions;
- **Log4j 1.x-era logging stack** — scheduled for replacement by SLF4J 2 and the Spring Boot logging stack.

Old configuration values that mention MOCCA may still be parsed during migration, but the modernized runtime does not offer MOCCA as a signing backend.

## Modules

The repository still contains the historical modules while modernization is under way. The first new module is:

- `nexu-spring-boot-server` — Spring Boot implementation of the NexU local HTTP server contract.

A separate Maven reactor is provided so that the modernization can be validated without immediately destabilizing the historical root build:

```bash
mvn -f pom-modernization.xml \
    -pl nexu-spring-boot-server \
    -am \
    package
```

The modernization build currently targets Java 17. Portable distributions will bundle their own runtime, so end users will not need to install a matching JDK.

## Security principles

- The private key must never leave the smart card or operating-system key store.
- PIN entry and certificate selection belong to the trusted local application, not the remote webpage.
- The local API must listen only on loopback interfaces.
- Browser origins must be validated against an allowlist.
- Each signing or authentication operation should have a short-lived, single-use identifier.
- The server must validate certificate trust, certificate purpose and signature algorithms independently of client claims.
- Authentication challenges must be bound to the browser session and consumed exactly once.
- Sensitive values, including PINs and signature material, must not be written to logs.

## Web eID references

The protocol and server-side separation are informed by these open-source projects maintained by the Estonian Information System Authority:

- [`web-eid/web-eid-spring-boot-example`](https://github.com/web-eid/web-eid-spring-boot-example) — archived Spring Boot example for authentication and document signing;
- [`web-eid/web-eid-authtoken-validation-java`](https://github.com/web-eid/web-eid-authtoken-validation-java) — maintained Java challenge-generation and authentication-token validation library, including its current Spring Boot example.

NexU is not an official Web eID implementation. The projects are used as architectural and security references, while NexU keeps its own compatibility API and support for existing smart-card middleware.

## License

NexU is distributed under the **European Union Public Licence, version 1.2 (EUPL-1.2)**. See [`LICENSE`](LICENSE).

The Web eID reference projects are distributed under the **MIT License**:

- `web-eid-spring-boot-example`: copyright © 2020–2023 Estonian Information System Authority;
- `web-eid-authtoken-validation-java`: copyright © 2020–2025 Estonian Information System Authority.

Complete attribution and third-party license notices are available in [`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md). Dependency licenses remain governed by their respective projects and distribution terms.

## Credits

NexU originated at [Nowina Solutions](https://github.com/nowina-solutions/nexu). This fork also includes contributions and ideas from the wider NexU community, including work by `dlemaignent`, `sharedchains`, `IntesysOpenway` and `hello-earth-gh`.

The modernization additionally acknowledges the Web eID maintainers for publishing reusable protocol documentation, examples and validation components under open-source licences.
