# Windows build diagnostic

- Source commit: `ed89f811a633a5f6755d55062f104f92aba8e92b`
- Workflow run: `29566249088`
- Maven exit code: `0`

```text
[INFO] Downloaded from central: https://repo.maven.apache.org/maven2/net/bytebuddy/byte-buddy-agent/1.17.8/byte-buddy-agent-1.17.8.jar (367 kB at 286 kB/s)
[INFO] Downloaded from central: https://repo.maven.apache.org/maven2/org/springframework/spring-test/6.2.19/spring-test-6.2.19.jar (1.0 MB at 798 kB/s)
[INFO] Downloaded from central: https://repo.maven.apache.org/maven2/org/springframework/spring-core/6.2.19/spring-core-6.2.19.jar (2.0 MB at 1.5 MB/s)
[INFO] Downloaded from central: https://repo.maven.apache.org/maven2/org/xmlunit/xmlunit-core/2.10.4/xmlunit-core-2.10.4.jar (178 kB at 137 kB/s)
[INFO] 
[INFO] --- resources:3.4.0:resources (default-resources) @ nexu-app ---
[INFO] Copying 1 resource from src\main\resources to target\classes
[INFO] Copying 25 resources from src\main\resources to target\classes
[INFO] 
[INFO] --- compiler:3.11.0:compile (default-compile) @ nexu-app ---
[INFO] Changes detected - recompiling the module! :dependency
[INFO] Compiling 39 source files with javac [debug target 17] to target\classes
[INFO] /D:/a/nexu/nexu/nexu-app/src/main/java/lu/nowina/nexu/rest/RestHttpPlugin.java: Some input files use or override a deprecated API.
[INFO] /D:/a/nexu/nexu/nexu-app/src/main/java/lu/nowina/nexu/rest/RestHttpPlugin.java: Recompile with -Xlint:deprecation for details.
[INFO] 
[INFO] --- resources:3.4.0:testResources (default-testResources) @ nexu-app ---
[INFO] Not copying test resources
[INFO] 
[INFO] --- compiler:3.11.0:testCompile (default-testCompile) @ nexu-app ---
[INFO] Not compiling test sources
[INFO] 
[INFO] --- surefire:3.5.4:test (default-test) @ nexu-app ---
[INFO] Tests are skipped.
[INFO] 
[INFO] --- jar:3.5.0:jar (default-jar) @ nexu-app ---
[INFO] Building jar: D:\a\nexu\nexu\nexu-app\target\nexu-app.jar
[INFO] 
[INFO] --- spring-boot:3.5.16:repackage (default) @ nexu-app ---
[INFO] Downloading from central: https://repo.maven.apache.org/maven2/org/springframework/boot/spring-boot-buildpack-platform/3.5.16/spring-boot-buildpack-platform-3.5.16.pom
[INFO] Downloaded from central: https://repo.maven.apache.org/maven2/org/springframework/boot/spring-boot-buildpack-platform/3.5.16/spring-boot-buildpack-platform-3.5.16.pom (3.2 kB at 87 kB/s)
[INFO] Downloading from central: https://repo.maven.apache.org/maven2/net/java/dev/jna/jna-platform/5.17.0/jna-platform-5.17.0.pom
[INFO] Downloaded from central: https://repo.maven.apache.org/maven2/net/java/dev/jna/jna-platform/5.17.0/jna-platform-5.17.0.pom (2.3 kB at 56 kB/s)
[INFO] Downloading from central: https://repo.maven.apache.org/maven2/net/java/dev/jna/jna/5.17.0/jna-5.17.0.pom
[INFO] Downloaded from central: https://repo.maven.apache.org/maven2/net/java/dev/jna/jna/5.17.0/jna-5.17.0.pom (2.0 kB at 56 kB/s)
[INFO] Downloading from central: https://repo.maven.apache.org/maven2/org/apache/commons/commons-compress/1.27.1/commons-compress-1.27.1.pom
[INFO] Downloaded from central: https://repo.maven.apache.org/maven2/org/apache/commons/commons-compress/1.27.1/commons-compress-1.27.1.pom (23 kB at 623 kB/s)
[INFO] Downloading from central: https://repo.maven.apache.org/maven2/commons-codec/commons-codec/1.17.1/commons-codec-1.17.1.pom
[INFO] Downloaded from central: https://repo.maven.apache.org/maven2/commons-codec/commons-codec/1.17.1/commons-codec-1.17.1.pom (18 kB at 386 kB/s)
[INFO] Downloading from central: https://repo.maven.apache.org/maven2/org/apache/commons/commons-parent/71/commons-parent-71.pom
[INFO] Downloaded from central: https://repo.maven.apache.org/maven2/org/apache/commons/commons-parent/71/commons-parent-71.pom (78 kB at 1.7 MB/s)
[INFO] Downloading from central: https://repo.maven.apache.org/maven2/org/apache/httpcomponents/client5/httpclient5/5.5.2/httpclient5-5.5.2.pom
[INFO] Downloaded from central: https://repo.maven.apache.org/maven2/org/apache/httpcomponents/client5/httpclient5/5.5.2/httpclient5-5.5.2.pom (6.1 kB at 78 kB/s)
[INFO] Downloading from central: https://repo.maven.apache.org/maven2/org/apache/httpcomponents/client5/httpclient5-parent/5.5.2/httpclient5-parent-5.5.2.pom
[INFO] Downloaded from central: https://repo.maven.apache.org/maven2/org/apache/httpcomponents/client5/httpclient5-parent/5.5.2/httpclient5-parent-5.5.2.pom (17 kB at 194 kB/s)
[INFO] Downloading from central: https://repo.maven.apache.org/maven2/org/apache/httpcomponents/httpcomponents-parent/14/httpcomponents-parent-14.pom
[INFO] Downloaded from central: https://repo.maven.apache.org/maven2/org/apache/httpcomponents/httpcomponents-parent/14/httpcomponents-parent-14.pom (30 kB at 690 kB/s)
[INFO] Downloading from central: https://repo.maven.apache.org/maven2/org/apache/apache/27/apache-27.pom
[INFO] Downloaded from central: https://repo.maven.apache.org/maven2/org/apache/apache/27/apache-27.pom (20 kB at 566 kB/s)
[INFO] Downloading from central: https://repo.maven.apache.org/maven2/org/junit/junit-bom/5.13.3/junit-bom-5.13.3.pom
[INFO] Downloaded from central: https://repo.maven.apache.org/maven2/org/junit/junit-bom/5.13.3/junit-bom-5.13.3.pom (5.7 kB at 162 kB/s)
[INFO] Downloading from central: https://repo.maven.apache.org/maven2/org/apache/httpcomponents/core5/httpcore5/5.3.6/httpcore5-5.3.6.pom
[INFO] Downloaded from central: https://repo.maven.apache.org/maven2/org/apache/httpcomponents/core5/httpcore5/5.3.6/httpcore5-5.3.6.pom (3.9 kB at 110 kB/s)
[INFO] Downloading from central: https://repo.maven.apache.org/maven2/org/apache/httpcomponents/core5/httpcore5-parent/5.3.6/httpcore5-parent-5.3.6.pom
[INFO] Downloaded from central: https://repo.maven.apache.org/maven2/org/apache/httpcomponents/core5/httpcore5-parent/5.3.6/httpcore5-parent-5.3.6.pom (14 kB at 349 kB/s)
[INFO] Downloading from central: https://repo.maven.apache.org/maven2/org/apache/httpcomponents/httpcomponents-parent/13/httpcomponents-parent-13.pom
[INFO] Downloaded from central: https://repo.maven.apache.org/maven2/org/apache/httpcomponents/httpcomponents-parent/13/httpcomponents-parent-13.pom (30 kB at 863 kB/s)
[INFO] Downloading from central: https://repo.maven.apache.org/maven2/org/apache/httpcomponents/core5/httpcore5-h2/5.3.6/httpcore5-h2-5.3.6.pom
[INFO] Downloaded from central: https://repo.maven.apache.org/maven2/org/apache/httpcomponents/core5/httpcore5-h2/5.3.6/httpcore5-h2-5.3.6.pom (3.6 kB at 95 kB/s)
[INFO] Downloading from central: https://repo.maven.apache.org/maven2/org/tomlj/tomlj/1.0.0/tomlj-1.0.0.pom
[INFO] Downloaded from central: https://repo.maven.apache.org/maven2/org/tomlj/tomlj/1.0.0/tomlj-1.0.0.pom (2.8 kB at 71 kB/s)
[INFO] Downloading from central: https://repo.maven.apache.org/maven2/org/antlr/antlr4-runtime/4.7.2/antlr4-runtime-4.7.2.pom
[INFO] Downloaded from central: https://repo.maven.apache.org/maven2/org/antlr/antlr4-runtime/4.7.2/antlr4-runtime-4.7.2.pom (3.6 kB at 52 kB/s)
[INFO] Downloading from central: https://repo.maven.apache.org/maven2/org/antlr/antlr4-master/4.7.2/antlr4-master-4.7.2.pom
[INFO] Downloaded from central: https://repo.maven.apache.org/maven2/org/antlr/antlr4-master/4.7.2/antlr4-master-4.7.2.pom (4.4 kB at 157 kB/s)
[INFO] Downloading from central: https://repo.maven.apache.org/maven2/com/google/code/findbugs/jsr305/3.0.2/jsr305-3.0.2.pom
[INFO] Downloaded from central: https://repo.maven.apache.org/maven2/com/google/code/findbugs/jsr305/3.0.2/jsr305-3.0.2.pom (4.3 kB at 148 kB/s)
[INFO] Downloading from central: https://repo.maven.apache.org/maven2/org/springframework/boot/spring-boot-loader-tools/3.5.16/spring-boot-loader-tools-3.5.16.pom
[INFO] Downloaded from central: https://repo.maven.apache.org/maven2/org/springframework/boot/spring-boot-loader-tools/3.5.16/spring-boot-loader-tools-3.5.16.pom (2.2 kB at 83 kB/s)
[INFO] Downloading from central: https://repo.maven.apache.org/maven2/org/apache/maven/plugins/maven-shade-plugin/3.6.0/maven-shade-plugin-3.6.0.pom
[INFO] Downloaded from central: https://repo.maven.apache.org/maven2/org/apache/maven/plugins/maven-shade-plugin/3.6.0/maven-shade-plugin-3.6.0.pom (12 kB at 375 kB/s)
[INFO] Downloading from central: https://repo.maven.apache.org/maven2/org/apache/maven/plugins/maven-plugins/42/maven-plugins-42.pom
[INFO] Downloaded from central: https://repo.maven.apache.org/maven2/org/apache/maven/plugins/maven-plugins/42/maven-plugins-42.pom (7.7 kB at 275 kB/s)
[INFO] Downloading from central: https://repo.maven.apache.org/maven2/org/codehaus/plexus/plexus-utils/3.5.1/plexus-utils-3.5.1.pom
[INFO] Downloaded from central: https://repo.maven.apache.org/maven2/org/codehaus/plexus/plexus-utils/3.5.1/plexus-utils-3.5.1.pom (8.8 kB at 351 kB/s)
[INFO] Downloading from central: https://repo.maven.apache.org/maven2/org/ow2/asm/asm/9.7/asm-9.7.pom
[INFO] Downloaded from central: https://repo.maven.apache.org/maven2/org/ow2/asm/asm/9.7/asm-9.7.pom (2.4 kB at 91 kB/s)
[INFO] Downloading from central: https://repo.maven.apache.org/maven2/org/ow2/asm/asm-commons/9.7/asm-commons-9.7.pom
[INFO] Downloaded from central: https://repo.maven.apache.org/maven2/org/ow2/asm/asm-commons/9.7/asm-commons-9.7.pom (2.8 kB at 100 kB/s)
[INFO] Downloading from central: https://repo.maven.apache.org/maven2/org/ow2/asm/asm-tree/9.7/asm-tree-9.7.pom
[INFO] Downloaded from central: https://repo.maven.apache.org/maven2/org/ow2/asm/asm-tree/9.7/asm-tree-9.7.pom (2.6 kB at 84 kB/s)
[INFO] Downloading from central: https://repo.maven.apache.org/maven2/org/jdom/jdom2/2.0.6.1/jdom2-2.0.6.1.pom
[INFO] Downloaded from central: https://repo.maven.apache.org/maven2/org/jdom/jdom2/2.0.6.1/jdom2-2.0.6.1.pom (4.6 kB at 148 kB/s)
[INFO] Downloading from central: https://repo.maven.apache.org/maven2/org/apache/commons/commons-compress/1.26.2/commons-compress-1.26.2.pom
[INFO] Downloaded from central: https://repo.maven.apache.org/maven2/org/apache/commons/commons-compress/1.26.2/commons-compress-1.26.2.pom (23 kB at 791 kB/s)
[INFO] Downloading from central: https://repo.maven.apache.org/maven2/org/vafer/jdependency/2.10/jdependency-2.10.pom
[INFO] Downloaded from central: https://repo.maven.apache.org/maven2/org/vafer/jdependency/2.10/jdependency-2.10.pom (14 kB at 468 kB/s)
[INFO] Downloading from central: https://repo.maven.apache.org/maven2/org/springframework/boot/spring-boot-buildpack-platform/3.5.16/spring-boot-buildpack-platform-3.5.16.jar
[INFO] Downloaded from central: https://repo.maven.apache.org/maven2/org/springframework/boot/spring-boot-buildpack-platform/3.5.16/spring-boot-buildpack-platform-3.5.16.jar (325 kB at 13 MB/s)
[INFO] Downloading from central: https://repo.maven.apache.org/maven2/net/java/dev/jna/jna-platform/5.17.0/jna-platform-5.17.0.jar
[INFO] Downloading from central: https://repo.maven.apache.org/maven2/net/java/dev/jna/jna/5.17.0/jna-5.17.0.jar
[INFO] Downloading from central: https://repo.maven.apache.org/maven2/org/apache/commons/commons-compress/1.27.1/commons-compress-1.27.1.jar
[INFO] Downloading from central: https://repo.maven.apache.org/maven2/commons-codec/commons-codec/1.17.1/commons-codec-1.17.1.jar
[INFO] Downloading from central: https://repo.maven.apache.org/maven2/org/apache/commons/commons-lang3/3.16.0/commons-lang3-3.16.0.jar
[INFO] Downloaded from central: https://repo.maven.apache.org/maven2/org/apache/commons/commons-compress/1.27.1/commons-compress-1.27.1.jar (1.1 MB at 31 MB/s)
[INFO] Downloading from central: https://repo.maven.apache.org/maven2/org/apache/httpcomponents/client5/httpclient5/5.5.2/httpclient5-5.5.2.jar
[INFO] Downloaded from central: https://repo.maven.apache.org/maven2/commons-codec/commons-codec/1.17.1/commons-codec-1.17.1.jar (373 kB at 10 MB/s)
[INFO] Downloading from central: https://repo.maven.apache.org/maven2/org/apache/httpcomponents/core5/httpcore5/5.3.6/httpcore5-5.3.6.jar
[INFO] Downloaded from central: https://repo.maven.apache.org/maven2/org/apache/commons/commons-lang3/3.16.0/commons-lang3-3.16.0.jar (674 kB at 15 MB/s)
[INFO] Downloading from central: https://repo.maven.apache.org/maven2/org/apache/httpcomponents/core5/httpcore5-h2/5.3.6/httpcore5-h2-5.3.6.jar
[INFO] Downloaded from central: https://repo.maven.apache.org/maven2/net/java/dev/jna/jna/5.17.0/jna-5.17.0.jar (2.0 MB at 37 MB/s)
[INFO] Downloading from central: https://repo.maven.apache.org/maven2/org/tomlj/tomlj/1.0.0/tomlj-1.0.0.jar
[INFO] Downloaded from central: https://repo.maven.apache.org/maven2/org/apache/httpcomponents/core5/httpcore5/5.3.6/httpcore5-5.3.6.jar (909 kB at 14 MB/s)
[INFO] Downloading from central: https://repo.maven.apache.org/maven2/org/antlr/antlr4-runtime/4.7.2/antlr4-runtime-4.7.2.jar
[INFO] Downloaded from central: https://repo.maven.apache.org/maven2/org/apache/httpcomponents/client5/httpclient5/5.5.2/httpclient5-5.5.2.jar (961 kB at 14 MB/s)
[INFO] Downloading from central: https://repo.maven.apache.org/maven2/com/google/code/findbugs/jsr305/3.0.2/jsr305-3.0.2.jar
[INFO] Downloaded from central: https://repo.maven.apache.org/maven2/org/tomlj/tomlj/1.0.0/tomlj-1.0.0.jar (157 kB at 1.9 MB/s)
[INFO] Downloading from central: https://repo.maven.apache.org/maven2/org/springframework/boot/spring-boot-loader-tools/3.5.16/spring-boot-loader-tools-3.5.16.jar
[INFO] Downloaded from central: https://repo.maven.apache.org/maven2/org/apache/httpcomponents/core5/httpcore5-h2/5.3.6/httpcore5-h2-5.3.6.jar (243 kB at 3.0 MB/s)
[INFO] Downloading from central: https://repo.maven.apache.org/maven2/org/apache/maven/plugins/maven-shade-plugin/3.6.0/maven-shade-plugin-3.6.0.jar
[INFO] Downloaded from central: https://repo.maven.apache.org/maven2/org/antlr/antlr4-runtime/4.7.2/antlr4-runtime-4.7.2.jar (338 kB at 3.7 MB/s)
[INFO] Downloading from central: https://repo.maven.apache.org/maven2/org/codehaus/plexus/plexus-utils/3.5.1/plexus-utils-3.5.1.jar
[INFO] Downloaded from central: https://repo.maven.apache.org/maven2/com/google/code/findbugs/jsr305/3.0.2/jsr305-3.0.2.jar (20 kB at 206 kB/s)
[INFO] Downloading from central: https://repo.maven.apache.org/maven2/org/ow2/asm/asm/9.7/asm-9.7.jar
[INFO] Downloaded from central: https://repo.maven.apache.org/maven2/org/apache/maven/plugins/maven-shade-plugin/3.6.0/maven-shade-plugin-3.6.0.jar (150 kB at 1.4 MB/s)
[INFO] Downloading from central: https://repo.maven.apache.org/maven2/org/ow2/asm/asm-commons/9.7/asm-commons-9.7.jar
[INFO] Downloaded from central: https://repo.maven.apache.org/maven2/org/codehaus/plexus/plexus-utils/3.5.1/plexus-utils-3.5.1.jar (269 kB at 2.3 MB/s)
[INFO] Downloading from central: https://repo.maven.apache.org/maven2/org/ow2/asm/asm-tree/9.7/asm-tree-9.7.jar
[INFO] Downloaded from central: https://repo.maven.apache.org/maven2/org/springframework/boot/spring-boot-loader-tools/3.5.16/spring-boot-loader-tools-3.5.16.jar (468 kB at 3.9 MB/s)
[INFO] Downloading from central: https://repo.maven.apache.org/maven2/org/jdom/jdom2/2.0.6.1/jdom2-2.0.6.1.jar
[INFO] Downloaded from central: https://repo.maven.apache.org/maven2/org/ow2/asm/asm/9.7/asm-9.7.jar (125 kB at 995 kB/s)
[INFO] Downloading from central: https://repo.maven.apache.org/maven2/org/vafer/jdependency/2.10/jdependency-2.10.jar
[INFO] Downloaded from central: https://repo.maven.apache.org/maven2/org/ow2/asm/asm-commons/9.7/asm-commons-9.7.jar (73 kB at 544 kB/s)
[INFO] Downloaded from central: https://repo.maven.apache.org/maven2/net/java/dev/jna/jna-platform/5.17.0/jna-platform-5.17.0.jar (1.4 MB at 9.6 MB/s)
[INFO] Downloaded from central: https://repo.maven.apache.org/maven2/org/ow2/asm/asm-tree/9.7/asm-tree-9.7.jar (52 kB at 368 kB/s)
[INFO] Downloaded from central: https://repo.maven.apache.org/maven2/org/jdom/jdom2/2.0.6.1/jdom2-2.0.6.1.jar (328 kB at 2.1 MB/s)
[INFO] Downloaded from central: https://repo.maven.apache.org/maven2/org/vafer/jdependency/2.10/jdependency-2.10.jar (416 kB at 2.6 MB/s)
[INFO] Replacing main artifact D:\a\nexu\nexu\nexu-app\target\nexu-app.jar with repackaged archive, adding nested dependencies in BOOT-INF/.
[INFO] The original artifact has been renamed to D:\a\nexu\nexu\nexu-app\target\nexu-app.jar.original
[INFO] ------------------------------------------------------------------------
[INFO] Reactor Summary for NexU 1.24-SNAPSHOT:
[INFO] 
[INFO] NexU ............................................... SUCCESS [  0.003 s]
[INFO] NexU core .......................................... SUCCESS [ 16.099 s]
[INFO] NexU operator application .......................... SUCCESS [ 11.073 s]
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  31.459 s
[INFO] Finished at: 2026-07-17T08:24:30Z
[INFO] ------------------------------------------------------------------------
```
