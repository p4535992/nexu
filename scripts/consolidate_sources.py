#!/usr/bin/env python3
"""Physically consolidate the historical NexU source tree into two Maven modules."""

from pathlib import Path
import shutil
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
NS = {"m": "http://maven.apache.org/POM/4.0.0"}
ET.register_namespace("", NS["m"])
ET.register_namespace("xsi", "http://www.w3.org/2001/XMLSchema-instance")


def merge_tree(source: Path, target: Path, excluded_prefixes=()) -> None:
    if not source.exists():
        return
    for item in sorted(source.rglob("*")):
        if not item.is_file():
            continue
        relative = item.relative_to(source)
        relative_text = relative.as_posix()
        if any(
            relative_text == prefix
            or relative_text.startswith(prefix.rstrip("/") + "/")
            for prefix in excluded_prefixes
        ):
            continue
        destination = target / relative
        destination.parent.mkdir(parents=True, exist_ok=True)
        if destination.exists():
            if destination.read_bytes() != item.read_bytes():
                print(f"Keeping modern destination over legacy file: {destination}")
            item.unlink()
        else:
            shutil.move(str(item), str(destination))


def remove_matching(parent, tag, predicate) -> None:
    for child in list(parent.findall(tag, NS)):
        if predicate(child):
            parent.remove(child)


def artifact_id(node) -> str:
    value = node.find("m:artifactId", NS)
    return value.text if value is not None and value.text else ""


def is_dependency(node, group_id: str, artifact: str) -> bool:
    group = node.find("m:groupId", NS)
    name = node.find("m:artifactId", NS)
    return (
        group is not None
        and name is not None
        and group.text == group_id
        and name.text == artifact
    )


def clean_module_pom(path: Path, application: bool = False) -> None:
    tree = ET.parse(path)
    project = tree.getroot()

    properties = project.find("m:properties", NS)
    if properties is not None:
        release = properties.find("m:maven.compiler.release", NS)
        if release is not None:
            properties.remove(release)

    dependencies = project.find("m:dependencies", NS)
    if application and dependencies is not None:
        remove_matching(
            dependencies,
            "m:dependency",
            lambda node: is_dependency(node, "org.eclipse.jetty", "jetty-server"),
        )

    build = project.find("m:build", NS)
    if build is not None:
        resources = build.find("m:resources", NS)
        if resources is not None:
            remove_matching(
                resources,
                "m:resource",
                lambda node: node.find("m:directory", NS) is not None
                and (node.find("m:directory", NS).text or "").startswith("../"),
            )

        test_resources = build.find("m:testResources", NS)
        if test_resources is not None:
            remove_matching(
                test_resources,
                "m:testResource",
                lambda node: node.find("m:directory", NS) is not None
                and (node.find("m:directory", NS).text or "").startswith("../"),
            )
            if not list(test_resources):
                build.remove(test_resources)

        plugins = build.find("m:plugins", NS)
        if plugins is not None:
            remove_matching(
                plugins,
                "m:plugin",
                lambda node: artifact_id(node) == "build-helper-maven-plugin",
            )
            if application:
                for plugin in plugins.findall("m:plugin", NS):
                    if artifact_id(plugin) != "maven-compiler-plugin":
                        continue
                    configuration = plugin.find("m:configuration", NS)
                    if configuration is not None:
                        excludes = configuration.find("m:excludes", NS)
                        if excludes is not None:
                            configuration.remove(excludes)

    ET.indent(tree, space="    ")
    tree.write(path, encoding="UTF-8", xml_declaration=True)


def clean_root_pom() -> None:
    path = ROOT / "pom.xml"
    tree = ET.parse(path)
    project = tree.getroot()
    build = project.find("m:build", NS)
    if build is not None:
        plugins = build.find("m:plugins", NS)
        if plugins is not None:
            remove_matching(
                plugins,
                "m:plugin",
                lambda node: artifact_id(node) == "maven-install-plugin",
            )
    ET.indent(tree, space="    ")
    tree.write(path, encoding="UTF-8", xml_declaration=True)


def main() -> None:
    for module in (
        "nexu-api",
        "nexu-model",
        "nexu-util",
        "nexu-windows-keystore-plugin",
    ):
        merge_tree(ROOT / module / "src/main/java", ROOT / "nexu-core/src/main/java")
        merge_tree(
            ROOT / module / "src/main/resources", ROOT / "nexu-core/src/main/resources"
        )
        merge_tree(
            ROOT / module / "src/test/java", ROOT / "nexu-core/src/legacy-test/java"
        )
        merge_tree(
            ROOT / module / "src/test/resources",
            ROOT / "nexu-core/src/legacy-test/resources",
        )

    core_tests = ROOT / "nexu-core/src/test"
    if core_tests.exists():
        merge_tree(core_tests / "java", ROOT / "nexu-core/src/legacy-test/java")
        merge_tree(
            core_tests / "resources", ROOT / "nexu-core/src/legacy-test/resources"
        )
        shutil.rmtree(core_tests, ignore_errors=True)

    merge_tree(
        ROOT / "nexu-standalone/src/main/java",
        ROOT / "nexu-app/src/main/java",
        excluded_prefixes=("lu/nowina/nexu/jetty",),
    )
    merge_tree(
        ROOT / "nexu-standalone/src/main/resources",
        ROOT / "nexu-app/src/main/resources",
    )
    merge_tree(
        ROOT / "nexu-spring-boot-server/src/main/java",
        ROOT / "nexu-app/src/main/java",
    )
    merge_tree(
        ROOT / "nexu-spring-boot-server/src/main/resources",
        ROOT / "nexu-app/src/main/resources",
    )
    merge_tree(
        ROOT / "nexu-spring-boot-server/src/test/java",
        ROOT / "nexu-app/src/test/java",
    )
    merge_tree(
        ROOT / "nexu-spring-boot-server/src/test/resources",
        ROOT / "nexu-app/src/test/resources",
    )
    merge_tree(
        ROOT / "nexu-rest-plugin/src/main/java", ROOT / "nexu-app/src/main/java"
    )
    merge_tree(
        ROOT / "nexu-rest-plugin/src/main/resources",
        ROOT / "nexu-app/src/main/resources",
    )

    for directory in (
        "nexu-api",
        "nexu-model",
        "nexu-util",
        "nexu-standalone",
        "nexu-spring-boot-server",
        "nexu-windows-keystore-plugin",
        "nexu-rest-plugin",
        "nexu-https-plugin",
        "nexu-multi-user-support",
        "nexu-public-object-model",
        "nexu-bundle",
        "nexu-proxy",
    ):
        shutil.rmtree(ROOT / directory, ignore_errors=True)

    clean_module_pom(ROOT / "nexu-core/pom.xml")
    clean_module_pom(ROOT / "nexu-app/pom.xml", application=True)
    clean_root_pom()

    legacy_readme = ROOT / "nexu-core/src/legacy-test/README.md"
    legacy_readme.parent.mkdir(parents=True, exist_ok=True)
    legacy_readme.write_text(
        "# Legacy tests\n\n"
        "These tests are preserved outside Maven default test sources because they "
        "depend on removed DSS 5 APIs, Log4j 1.x fixtures or retired server "
        "implementations. Migrate each test to DSS 6.4 and JUnit 5 before returning "
        "it to `src/test`.\n",
        encoding="utf-8",
    )


if __name__ == "__main__":
    main()
