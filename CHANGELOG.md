(20240530) added LGPLv2.1 license according to esig/dss practice which was AFAIK the initial reason behind NexU development
(20220329) added Windows 7 64-bit drivers and new SafeNet 5110 ATR to store.xml
(20220216) bumped DSS version to 5.9 (also contains 2e1925e8df commit from
luca.bonora@sharedchains.com fork regarding proper closing when multiple tokens are used)
(20211020) added cache_time_to_live_ms option to nexu-config.properties, and added external configuration support (modifiable nexu-config.properties outside of nexu.jar)
(20210426) created update installation package InnoSetup script (nexu-update), 
		   added different versions to all pom.xml files - must use mvn versions:set / commit to increment
		   fix bug to correctly parse requests from clean DSS CEF demo,
           other bugfixes with caching
(20210317) modified script to include MinVersion (for XP) and to compile with InnoSetup 5 instead of 6 (default)
(20210216) add logic to nexu to select automatically the certificate meant for signing (in case e.g. two exist on the same token)
           modified install scripts
(20201211) added aggregate inno script + create desktop icon for nexu install
(20201123) added info to store.xml
           replacing jps with wmic solution to kill nexu upon uninstall
(20201121) added code to select index 0 detected driver in case environments do not match (GenericCardAdapter)
(20200909) modified innosetup script to copy shortcut into startup folder
           added flag to be able to bypass product selection dialog, if there is only one card detected,
           changed README with a few clarifications
(20200807) added innosetup script (iss) and license text
(20200715) initial changes
(20220329) added Windows 7 64-bit drivers and new SafeNet 5110 ATR to store.xml
(20220216) bumped DSS version to 5.9 (also contains 2e1925e8df commit from
luca.bonora@sharedchains.com fork regarding proper closing when multiple tokens are used)
(20211020) added cache_time_to_live_ms option to nexu-config.properties, and added external configuration support (modifiable nexu-config.properties outside of nexu.jar)

(20210831) Upgrade the progect DSS 5.9.RC1, upgrade the JDK from "Oracle JDK 8" to "OpenJDK 11", upgrade the maven dependencies

(20210426) created update installation package InnoSetup script (nexu-update), 
		   added different versions to all pom.xml files - must use mvn versions:set / commit to increment
		   fix bug to correctly parse requests from clean DSS CEF demo,
           other bugfixes with caching
(20210317) modified script to include MinVersion (for XP) and to compile with InnoSetup 5 instead of 6 (default)
(20210216) add logic to nexu to select automatically the certificate meant for signing (in case e.g. two exist on the same token)
           modified install scripts
(20201211) added aggregate inno script + create desktop icon for nexu install
(20201123) added info to store.xml
           replacing jps with wmic solution to kill nexu upon uninstall
(20201121) added code to select index 0 detected driver in case environments do not match (GenericCardAdapter)
(20200909) modified innosetup script to copy shortcut into startup folder
           added flag to be able to bypass product selection dialog, if there is only one card detected,
           changed README with a few clarifications
(20200807) added innosetup script (iss) and license text
(20200715) initial changes
