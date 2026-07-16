from __future__ import annotations

from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[2]


def replace_exact(text: str, pattern: str, replacement: str, description: str) -> str:
    updated, count = re.subn(pattern, replacement, text, count=1, flags=re.DOTALL)
    if count != 1:
        raise RuntimeError(f"Expected one replacement for {description}, found {count}")
    return updated


root_pom = ROOT / "pom.xml"
text = root_pom.read_text(encoding="utf-8")
text = replace_exact(
    text,
    r"\s*<modules>.*?</modules>",
    "\n\t<modules>\n\t\t<module>nexu-core</module>\n\t\t<module>nexu-app</module>\n\t</modules>",
    "root module list",
)
text = text.replace("<java.version>11</java.version>", "<java.version>17</java.version>", 1)
text = text.replace("<maven.compiler.source>11</maven.compiler.source>", "<maven.compiler.source>17</maven.compiler.source>", 1)
text = text.replace("<maven.compiler.target>11</maven.compiler.target>", "<maven.compiler.target>17</maven.compiler.target>", 1)
text = re.sub(
    r"<javafx.version>[^<]+</javafx.version>(?:<!--.*?-->)?",
    "<javafx.version>21.0.11</javafx.version>",
    text,
    count=1,
)
text = text.replace(
    "<description>Multi-browser, multi-platform remote signature tool.</description>",
    "<description>Spring Boot and JavaFX local smart-card signing agent.</description>",
    1,
)
root_pom.write_text(text, encoding="utf-8")

# These directories remain temporarily as source containers referenced by the
# two consolidated modules. Removing their POM files makes the Maven boundary
# unambiguous while keeping source history easy to review.
legacy_poms = [
    "nexu-api/pom.xml",
    "nexu-model/pom.xml",
    "nexu-util/pom.xml",
    "nexu-standalone/pom.xml",
    "nexu-spring-boot-server/pom.xml",
    "nexu-windows-keystore-plugin/pom.xml",
    "nexu-rest-plugin/pom.xml",
    "nexu-https-plugin/pom.xml",
    "nexu-multi-user-support/pom.xml",
    "nexu-public-object-model/pom.xml",
    "nexu-bundle/pom.xml",
]
for relative in legacy_poms:
    path = ROOT / relative
    if path.exists():
        path.unlink()

modernization_pom = ROOT / "pom-modernization.xml"
if modernization_pom.exists():
    modernization_pom.unlink()

readme = ROOT / "README.md"
readme_text = readme.read_text(encoding="utf-8")
readme_text = replace_exact(
    readme_text,
    r"## Modules and build.*?(?=## Native and portable packages)",
    """## Modules and build

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

""",
    "README module section",
)
readme.write_text(readme_text, encoding="utf-8")

adr = ROOT / "docs/adr/0001-module-consolidation-and-web-eid-flow.md"
adr_text = adr.read_text(encoding="utf-8")
adr_text = replace_exact(
    adr_text,
    r"### Target module layout.*?(?=### Why not one Spring Boot module)",
    """### Target module layout

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

""",
    "ADR module layout",
)
adr_text = adr_text.replace(
    "### Why not one Spring Boot module",
    "### Why retain a separate core module",
    1,
)
adr_text = re.sub(
    r"A single module would make packaging superficially simpler but would weaken\nimportant boundaries:.*?Four modules are small enough to understand while preserving boundaries that\nhave genuine security, platform or test value\.",
    """A single module would make packaging superficially simpler but would couple
cryptographic and card code to Spring Boot and JavaFX. Keeping `nexu-core`
separate allows headless tests, avoids accidental server/UI dependencies in the
signing engine and preserves a reusable boundary for future native messaging.
Two modules are sufficient; further Maven separation would add ceremony without
meaningful isolation.""",
    adr_text,
    count=1,
    flags=re.DOTALL,
)
adr.write_text(adr_text, encoding="utf-8")
