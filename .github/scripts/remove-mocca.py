from pathlib import Path
import re
import shutil
import xml.etree.ElementTree as ET


def replace_required(text: str, old: str, new: str, label: str) -> str:
    if old not in text:
        raise RuntimeError(f"Expected {label} was not found")
    return text.replace(old, new, 1)


def write_xml(path: Path, content: str) -> None:
    path.write_text(content, encoding="utf-8")
    ET.parse(path)


root_pom = Path("pom.xml")
pom = root_pom.read_text(encoding="utf-8")
pom = replace_required(
    pom,
    "<url>https://github.com/nowina-solutions/nexu</url>",
    "<url>https://github.com/p4535992/nexu</url>",
    "project URL",
)
pom = replace_required(pom, "\n\t\t<module>sscd-mocca-adapter</module>", "", "commented MOCCA module")
pom = replace_required(pom, "\n\t\t<mocca.version>1.3.30</mocca.version>", "", "MOCCA version property")
pom = replace_required(
    pom,
    "<name>European Union Public Licence (EUPL) v1.1</name>\n\t\t\t<url>http://ec.europa.eu/idabc/eupl.html</url>",
    "<name>European Union Public Licence (EUPL) v1.2</name>\n\t\t\t<url>https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12</url>",
    "root POM licence metadata",
)
pom = replace_required(
    pom,
    "\n\t\t<repository>\n\t\t\t<id>egiz-mocca</id>\n\t\t\t<url>https://apps.egiz.gv.at/maven/</url>\n\t\t</repository>",
    "",
    "MOCCA Maven repository",
)
pom = replace_required(
    pom,
    "\n\t\t\t<dependency>\n\t\t\t\t<groupId>at.gv.egiz</groupId>\n\t\t\t\t<artifactId>smcc</artifactId>\n\t\t\t\t<version>${mocca.version}</version>\n\t\t\t</dependency>",
    "",
    "SMCC dependency management",
)
pom = replace_required(
    pom,
    "\n\n\t\t\t<!--  MOD 4535992\n\t\t\t<dependency>\n\t\t\t\t<groupId>lu.nowina</groupId>\n\t\t\t\t<artifactId>nexu_for_mocca</artifactId>\n\t\t\t\t<version>1.22</version>\n\t\t\t</dependency>\n\t\t\tEND MOD 4535992 -->",
    "",
    "local MOCCA workaround",
)
pom = replace_required(
    pom,
    "\n\t\t\t<dependency>\n\t\t\t\t<groupId>eu.europa.ec.joinup.sd-dss</groupId>\n\t\t\t\t<artifactId>sscd-mocca-adapter</artifactId>\n\t\t\t\t<version>${dss.version}</version>\n\t\t\t</dependency>\t\t",
    "",
    "MOCCA adapter dependency management",
)
write_xml(root_pom, pom)

core_pom_path = Path("nexu-core/pom.xml")
core_pom = core_pom_path.read_text(encoding="utf-8")
core_pom, replacements = re.subn(
    r"\n\t\t<!-- unisystems change -->\n\t\t<!-- MOD 4535992\n\t\t<dependency>\n\t\t\t<groupId>lu\.nowina</groupId>\n\t\t\t<artifactId>nexu_for_mocca</artifactId>\n\t\t</dependency>\s*\n\t\t-->",
    "",
    core_pom,
    count=1,
)
if replacements != 1:
    raise RuntimeError("Expected nexu-core MOCCA workaround was not found")
write_xml(core_pom_path, core_pom)

spring_pom_path = Path("nexu-spring-boot-server/pom.xml")
spring_pom = spring_pom_path.read_text(encoding="utf-8")
spring_pom, replacements = re.subn(
    r"\n\s*<!-- The modernized runtime uses PKCS#11 and MSCAPI\. The old\n\s*MOCCA artifacts are no longer published and the current\n\s*adapter was already non-functional\. -->\n\s*<exclusion>\n\s*<groupId>at\.gv\.egiz</groupId>\n\s*<artifactId>smcc</artifactId>\n\s*</exclusion>\n\s*<exclusion>\n\s*<groupId>eu\.europa\.ec\.joinup\.sd-dss</groupId>\n\s*<artifactId>sscd-mocca-adapter</artifactId>\n\s*</exclusion>",
    "",
    spring_pom,
    count=1,
)
if replacements != 1:
    raise RuntimeError("Expected Spring Boot MOCCA exclusions were not found")
write_xml(spring_pom_path, spring_pom)

advanced_path = Path("nexu-core/src/main/java/lu/nowina/nexu/flow/operation/AdvancedCreationFeedbackOperation.java")
advanced = advanced_path.read_text(encoding="utf-8")
advanced = replace_required(
    advanced,
    "((feedback.getSelectedAPI() == ScAPI.MOCCA) || (feedback.getSelectedAPI() == ScAPI.MSCAPI) ||\n                            (feedback.getApiParameter() != null))",
    "((feedback.getSelectedAPI() == ScAPI.MSCAPI) || (feedback.getApiParameter() != null))",
    "MOCCA feedback condition",
)
advanced_path.write_text(advanced, encoding="utf-8")

scapi_path = Path("nexu-api/src/main/java/lu/nowina/nexu/api/ScAPI.java")
scapi = scapi_path.read_text(encoding="utf-8")
scapi = replace_required(
    scapi,
    '\t@XmlEnumValue("MOCCA") MOCCA("MOCCA");',
    '\t/** Retained only to read historical configuration; no runtime backend exists. */\n\t@Deprecated\n\t@XmlEnumValue("MOCCA") MOCCA("MOCCA");',
    "MOCCA compatibility enum",
)
scapi_path.write_text(scapi, encoding="utf-8")

for properties_path in (
    Path("nexu-core/src/main/resources/bundles/nexu.properties"),
    Path("nexu-core/src/main/resources/bundles/nexu_fr.properties"),
    Path("nexu-core/src/main/resources/bundles/nexu_nl.properties"),
):
    lines = [
        line
        for line in properties_path.read_text(encoding="utf-8").splitlines()
        if not line.startswith("api.mocca=")
    ]
    properties_path.write_text("\n".join(lines) + "\n", encoding="utf-8")

doc_path = Path("docs/spring-boot-modernization.md")
doc = doc_path.read_text(encoding="utf-8")
doc = replace_required(
    doc,
    "- No rewrite of PIN, certificate selection, PKCS#11, MSCAPI, or MOCCA flows.",
    "- MOCCA is removed; PKCS#11 and MSCAPI remain the supported signing backends.",
    "stale MOCCA migration documentation",
)
doc_path.write_text(doc, encoding="utf-8")

shutil.rmtree("sscd-mocca-adapter")
for obsolete in (
    Path("scripts/README.md"),
    Path("scripts/install_as_mvn_artifact.sh"),
    Path(".github/workflows/apply-mocca-cleanup.yml"),
):
    if obsolete.exists():
        obsolete.unlink()

# This script is removed by the same migration commit.
Path(__file__).unlink()

for pom_path in (root_pom, core_pom_path, spring_pom_path):
    ET.parse(pom_path)

for scan_root in (Path("pom.xml"), Path("nexu-api"), Path("nexu-core"), Path("nexu-spring-boot-server")):
    paths = [scan_root] if scan_root.is_file() else scan_root.rglob("*")
    for path in paths:
        if path.is_file() and path.suffix in {".xml", ".java", ".properties"}:
            content = path.read_text(encoding="utf-8", errors="ignore")
            if re.search(r"<artifactId>(smcc|sscd-mocca-adapter|nexu_for_mocca)</artifactId>", content):
                raise RuntimeError(f"Active MOCCA Maven artifact remains in {path}")
