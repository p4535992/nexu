# Third-Party Notices

NexU is licensed under the European Union Public Licence, version 1.2. See [`LICENSE`](LICENSE) for the licence governing NexU itself.

This document records attribution and licence information for external open-source projects used as dependencies, implementation references, or sources of adapted material in the modernization work.

The inclusion of a notice does not imply that an entire third-party project is redistributed with every NexU package. A generated dependency and licence report should also be included in each binary distribution.

## Web eID Spring Boot example

- Project: [`web-eid/web-eid-spring-boot-example`](https://github.com/web-eid/web-eid-spring-boot-example)
- Purpose in NexU: architectural and implementation reference for separating local smart-card operations, server-side signing preparation/finalization, authentication challenges, and Spring Boot endpoints.
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
- Purpose in NexU: security and implementation reference for one-time challenges, origin-bound authentication tokens, server-side certificate validation, replay protection, and the maintained Spring Boot integration example.
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

## Distribution requirements

When NexU incorporates or redistributes source or substantial portions from either Web eID project, the corresponding copyright and MIT permission notice above must remain included.

Before publishing a binary release, the packaging process should generate and include a complete inventory of bundled dependencies and their licence texts. This file is an attribution baseline and is not a substitute for an automated dependency-licence report.
