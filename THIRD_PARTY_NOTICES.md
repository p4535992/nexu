# Third-Party Notices

NexU is licensed under the European Union Public Licence, version 1.2. See [`LICENSE`](LICENSE) for the licence governing this repository.

This document records attribution and licence information for historical upstream code, external open-source projects used as implementation references, and major dependencies or runtime components mentioned in the project documentation.

The inclusion of a notice does not imply that an entire third-party project is redistributed with every NexU package. The exact dependency set varies by module, platform and packaging profile. Every binary distribution must therefore include a generated dependency and licence report in addition to this attribution baseline.

## Historical NexU upstream

- Historical repository: [`nowheresly/nexu`](https://github.com/nowheresly/nexu)
- Original project identified by its Maven SCM metadata: `nowina-solutions/nexu`
- Purpose in this repository: historical source and lineage of the NexU codebase
- Licence declared by the historical Maven project: **European Union Public Licence v1.1 (`EUPL-1.1`)**
- Local copy of the historical licence: [`licenses/EUPL-1.1.txt`](licenses/EUPL-1.1.txt)

The historical `nowheresly/nexu` repository does not expose a separate root `LICENSE` file, but its root Maven POM explicitly declares EUPL v1.1 and points its SCM metadata to the original Nowina Solutions repository. Existing upstream copyright and licence notices must remain intact.

The EUPL-1.1 text above is retained for provenance and compliance with inherited source notices. It does not replace the current [`LICENSE`](LICENSE), which states the licence governing this repository.

## Web eID Spring Boot example

- Project: [`web-eid/web-eid-spring-boot-example`](https://github.com/web-eid/web-eid-spring-boot-example)
- Purpose in NexU: architectural and implementation reference for separating local smart-card operations, server-side signing preparation/finalization, authentication challenges, and Spring Boot endpoints
- Licence: MIT License
- Copyright: © 2020–2023 Estonian Information System Authority

```text
MIT License

Copyright (c) 2020-2023 Estonian Information System Authority

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

## Web eID authentication-token validation for Java

- Project: [`web-eid/web-eid-authtoken-validation-java`](https://github.com/web-eid/web-eid-authtoken-validation-java)
- Purpose in NexU: security and implementation reference for one-time challenges, origin-bound authentication tokens, server-side certificate validation, replay protection, and the maintained Spring Boot integration example
- Licence: MIT License
- Copyright: © 2020–2025 Estonian Information System Authority

```text
MIT License

Copyright (c) 2020-2025 Estonian Information System Authority

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

## Major dependencies and runtime components

The following list covers the principal projects explicitly mentioned in the README or used by the historical and modernized builds. It is intentionally a concise inventory, not a replacement for the complete generated report of direct and transitive dependencies.

| Project or component | Use in NexU | Licence summary |
| --- | --- | --- |
| [Spring Boot](https://github.com/spring-projects/spring-boot) | Modern local HTTP server and packaging foundation | Apache License 2.0 (`Apache-2.0`) |
| [OpenJDK](https://github.com/openjdk/jdk) | Java runtime when a JRE is bundled | GNU GPL v2 with the Classpath Exception for applicable files; bundled third-party notices also apply |
| [OpenJFX](https://github.com/openjdk/jfx) | Historical desktop UI and any retained JavaFX UI module | GNU GPL v2 with the Classpath Exception for applicable files; separately licensed bundled components also apply |
| [EU DSS](https://github.com/esig/dss) | Digital-signature APIs and integration compatibility | GNU Lesser General Public License v2.1 (`LGPL-2.1`) |
| [intarsys smartcard-io](https://github.com/mkentaro1/smartcard-io) | Historical smart-card I/O support | BSD 3-Clause License (`BSD-3-Clause`) |
| [jnasmartcardio](https://github.com/jnasmartcardio/jnasmartcardio) | Historical alternative Smart Card I/O provider | Creative Commons Zero v1.0 Universal (`CC0-1.0`) |
| [apdu4j](https://github.com/martinpaljak/apdu4j) | Smart-card and APDU-related utilities where present | MIT License (`MIT`) |

The runtime vendor may add its own notices or distribution terms. For example, a Temurin, Liberica or another OpenJDK-based runtime must be packaged together with the notices supplied by that exact runtime build; the generic OpenJDK entry above is not sufficient by itself.

## Distribution requirements

When NexU incorporates or redistributes source or substantial portions from either Web eID project, the corresponding copyright and MIT permission notice above must remain included.

When NexU redistributes OpenJDK, OpenJFX or another runtime image, all licence files, Classpath Exception notices, assembly exceptions and third-party notices shipped by that exact distribution must remain available in the package.

Before publishing a binary release, the packaging process must generate and include a complete inventory of bundled direct and transitive dependencies and their licence texts. This file is an attribution baseline and is not a substitute for automated dependency-licence analysis or legal review.