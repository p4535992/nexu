from pathlib import Path
import xml.etree.ElementTree as ET


ROOT_POM = Path("pom.xml")
OLD_VERSION = "<dss.version>5.11</dss.version><!-- 5.3, 5.9 -->"
NEW_VERSION = "<dss.version>6.4</dss.version>"


def main() -> None:
    content = ROOT_POM.read_text(encoding="utf-8")

    if NEW_VERSION in content:
        print("DSS 6.4 is already configured")
        return

    if OLD_VERSION not in content:
        raise RuntimeError("Expected DSS 5.11 version property was not found")

    content = content.replace(OLD_VERSION, NEW_VERSION, 1)
    ROOT_POM.write_text(content, encoding="utf-8")

    # Fail immediately if the replacement produced invalid Maven XML.
    ET.parse(ROOT_POM)
    print("Updated managed DSS version from 5.11 to 6.4")


if __name__ == "__main__":
    main()
