# Spring Boot modernization

This branch introduces the NexU modernization incrementally, preserving the existing browser protocol and smart-card flows while replacing the legacy HTTP and packaging layers.

## Initial scope

1. Add a Spring Boot HTTP adapter compatible with the existing `HttpServer` interface.
2. Preserve `/nexu-info`, `/nexu.js`, and `/rest/**` behavior.
3. Keep JavaFX and the current smart-card engine unchanged during the first step.
4. Add portable packaging based on a bundled Java runtime after the server adapter is stable.

## Non-goals of the first step

- No immediate DSS major-version upgrade.
- No rewrite of PIN, certificate selection, PKCS#11, MSCAPI, or MOCCA flows.
- No change to the JavaScript API consumed by DSS demonstrations.
