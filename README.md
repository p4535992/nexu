# NexU

To fill in the gap left by the demise of Java Applets to communicate with smartcards, 
Nowina has developed an innovative, open-source multi-browser multi-platform remote 
signature tool called NexU.

## Overview 

http://nowina.lu/solutions/java-less-browser-signing-nexu/

# NOTE

## My Smartcard is not been read why ???

If nexu do not "see" the connected smartcard try to delete the folder 

`C:\Users\<YOUR USER WIDNOWS>\AppData\Local\Nowina\NexU-Nowina`

The new store xml should be write in the path:

`C:\Users\<YOUR USER WINDOWS>\AppData\Local\Nowina\NexU`

The xml file for store information, has many options here a example of the file `luxtrust-database.xml` :

```
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<database>
	<pkcs11LibPath>C:\applet\win32\pkcs11wrapper.dll</pkcs11LibPath>
</database>
```

# INFORMATION ABOUT THIS FORK

This project is a friendly fork of the [nowina-solutions/nexu project](https://github.com/nowina-solutions/nexu). We will provide pull requests as needed.

The purpose of this fork is to provide caching ability for user entered data, for the length of the signing session
Features:

The main features we are targeting are :

* provide an easier installation procedure for Ubuntu LTS
* support of OpenJDK 11
* fix the [javafx mess](https://stackoverflow.com/questions/18547362/javafx-and-openjdk)
* caches entered password for the duration of a single session
* caches selected product for the duration of a single session
* created InnoSetup script and incorporated the setup package build process in the Maven POM file for nexu-standalone project
* added flag to be able to bypass product/token selection dialog, if exactly one card was found (as if the user selected it)
* upgrade the esign DSS to 5.9RC1
* added cache_time_to_live_ms option to nexu-config.properties, and added external configuration support (modifiable nexu-config.properties outside of nexu.jar)

NB: ...\nexu-bundle\src\main\resources\inno-setup-script.iss is a handy script that creates a setup package with InnoSetup.

InnoSetup has to be installed on the development machine. When building nexu-bundle, the following extras have been added in the fork:

1) additional Maven targets, win32 and win64 that bundle nexu with 32-bit and 64-bit JRE respectively (have to change target and build two times)
2) exec-maven-plugin that runs inno-setup-script.iss during package phase of Maven build - the result is in \target\nexu\nexu\inno directory

The install package will install NexU and place it into Windows Startup folder.

**IMPORTANT:** Due to strange issues with the javaFX library for java version 11 onwards (the library is no longer embedd in the jdk), the java machine to be used to start the nexu application must be the one packaged by Bellsoft https://bell-sw.com/pages/downloads/.

**How to debug: (https://github.com/nowina-solutions/nexu/issues/31)**

- [edit] I figured out that nexu-standalone is not supposed to run as a jar - it is supposed to be referenced in nexu-app, and nexu-app should be launched with java -jar ... command - this way the resources referenced in nexu-standalone are relative to root classpath, which is nexu-app - anyway, the references make sense then. - nexu-app SHOULD NOT be referenced by nexu-standalone of course, because this creates cyclic reference, and doesn't make sense anyway.

- [edit] The above way of running nexu seems to create a problem for Netbeans - because nexu-app uses Maven shade plugin to create the final JAR, Netbeans doesn't seem to detect the Main Class for execution, which is actually situated in a dependency artifact, nexu-standalone, and is appended to the MANIFEST.MF during the creation of the shaded JAR (... the only way I found to be able to debug NexU this way was to start up NexU from command line, supplying JDWP options to Java, and then to attach the Netbeans debugger to this process:

```
java -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=8000 -jar nexu-app-1.24-SNAPSHOT-jar-with-dependencies.jar
```

**Additional Info**

(attention: java.exe should belong to x64 or x86 version of JDK, depending on what kind of driver was installed)
"C:\Program Files\Java\jdk1.8.0_251\bin\java.exe" -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=8000 -jar nexu-app-shaded.jar

take care of the project version - when it does not end with -SNAPSHOT then Maven will try to find artifacts in the remote repository first and will fail
when compiling with netbeans, should explicitly change properties->build->compile JDK to 1.8 because higher JDK would need inclusion of extra modules

full build order: nexu-api, nexu-model, nexu-util, nexu-core, nexu-standalone, nexu-rest-plugin, nexu-https-plugin, nexu-multi-user-support, nexu-windows-keystore-plugin, nexu-app, nexu-bundle
usual build order: nexu-api (if e.g. added AppConfig setting), nexu-core (if e.g. changed one of the Operation's that take part in the Flow), nexu-standalone, nexu-app, nexu-bundle -
carefull with that, because a change in nexu-standalone will not be detected automatically when building nexu-bundle (e.g. in netbeans)
store.xml is in standalone

to change version in all projects:
cd nexu-master-modified
c:\tools\apache-maven-3.6.3\bin\mvn versions:set -DnewVersion=1.23-modified-03-SNAPSHOT
c:\tools\apache-maven-3.6.3\bin\mvn versions:commit

to build win32 & win64 versions, always do clean & build, and check that the final setup packages have different sizes.

Also, if you care to compile nexu to be able to debug it, egiz/smcc dependency is missing - to workaround this problem, just run install_as_mvn_artifact script, which installs the whole nexu.jar as the missing dependency (it contains the necessary classes inside)

IMPORTANT: before installing nexu.jar as artifact, copy it to e.g. nexu1.jar and strip it of all class files apart from egiz ones

## The configuration

### The "Close Token" Property

On this fork we have implemented some drastic "features", which might be considered questionable security-wise - albeit very useful. On the file "nexu-config.properties" there is a new property called "close_token" by default is true if is setted to false the server-side caching the user PKCS11 token password for the duration of a single signing session. 

There are use cases in which this can be very useful to the operator, but other cases in which precisely for safety reasons it must be avoided.

### System

For now this is tested on windows machine 64bit

# Additional info

- Due to the lack of maven projects to download runtime for OpenJDK 11 and JavaFX 11 (or OpenJFX) and for ease of integration some unpacked versions of the two have been made for the integration of the bundle. This mechanism needs to be revised.

- The project "nexu-proxy" is still in developing...

- [Setup the toolchain for maven](https://maven.apache.org/guides/mini/guide-using-toolchains.html)

- [Setup Smartcardio for JDk 11](https://nicedoc.io/jnasmartcardio/jnasmartcardio)


# LICENSE

- BSD License [intarsys smartcard-io](https://github.com/mkentaro1/smartcard-io/blob/master/License.txt) 
- CC0 License [jnasmartcardio](https://github.com/jnasmartcardio/jnasmartcardio/blob/master/LICENSE)
- MIT License [apdu4j](https://github.com/martinpaljak/apdu4j/blob/master/LICENSE)
- Apache License 2.0 [Spring boot](https://github.com/spring-projects/spring-boot/blob/main/LICENSE.txt)
- GNU General Public License 2.1 (LPGL) [eSign DSS](https://github.com/esig/dss/blob/master/LICENSE)
- GNU General Public License 2.0 (GPL) [OpenJdk](https://openjdk.java.net/legal/gplv2+ce.html)
- GNU General Public License 2.0 (GPL) [JavaFX 11 or OpenJFX](https://github.com/openjdk/jfx/blob/master/LICENSE)

# Credits

Ty to all the other developer with their contribution and all their fork...

- [dlemaignent](https://github.com/dlemaignent/nexu) for [use jsonp to avoid cors errors](https://github.com/dlemaignent/nexu/commit/60aa14245f5e2ffce70aa21d214367e36f4b458b)
- [sharedchains](https://github.com/sharedchains/nexu/) for [Fixed failure removing and inserting SmartCard Token ](https://github.com/sharedchains/nexu/commit/7b2d18f361d59ba5351efc4035a8f1c6aa19fbed)
- [IntesysOpenway](https://github.com/IntesysOpenway) for [some modification and the starting nexu-proxy](https://github.com/IntesysOpenway)
- [hello-earth-gh](https://github.com/hello-earth-gh) for [various changes and fixes](https://github.com/hello-earth-gh)
