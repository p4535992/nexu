ï»¿rem Ã‚Â© Nowina Solutions, 2015-2015
rem
rem ConceÃŒï¿½deÃŒï¿½e sous licence EUPL, version 1.1 ou Ã¢â‚¬â€œ deÃŒâ‚¬s leur approbation par la Commission europeÃŒï¿½enne - versions ulteÃŒï¿½rieures de lÃ¢â‚¬â„¢EUPL (la Ã‚Â«LicenceÃ‚Â»).
rem Vous ne pouvez utiliser la preÃŒï¿½sente Ã…â€œuvre que conformeÃŒï¿½ment aÃŒâ‚¬ la Licence.
rem Vous pouvez obtenir une copie de la Licence aÃŒâ‚¬ lÃ¢â‚¬â„¢adresse suivante:
rem
rem http://ec.europa.eu/idabc/eupl5
rem
rem Sauf obligation leÌ�gale ou contractuelle eÌ�crite, le logiciel distribueÌ� sous la Licence est distribueÌ� Â«en lâ€™eÌ�tatÂ»,
rem SANS GARANTIES OU CONDITIONS QUELLES QUâ€™ELLES SOIENT, expresses ou implicites.
rem Consultez la Licence pour les autorisations et les restrictions linguistiques spÃ©cifiques relevant de la Licence.

@echo off
rem start has a problem with first quoted parameter and this is a workaround
rem because we have absolute paths here (because there no easy way to specify _start in_ directory for a service)
rem we have to wrap them in double-quotes for the very possible case that they have spaces in them
rem start "" "__{app}"\java\bin\javaw.exe -Djavafx.preloader=lu.nowina.nexu.NexUPreLoader -Dglass.accessible.force=false -Xdebug -Xrunjdwp:transport=dt_socket,address=8989,server=y,suspend=n -jar "__{app}"\nexu.jar
rem start .\java\bin\javaw -Djavafx.preloader=lu.nowina.nexu.NexUPreLoader -Dglass.accessible.force=false -Xdebug -Xrunjdwp:transport=dt_socket,address=8989,server=y,suspend=n -jar nexu.jar
rem start .\java\bin\javaw -verbose --module-path .\javafx-sdk-11\lib --add-modules=javafx.controls --add-modules=javafx.swing -Djavafx.preloader=lu.nowina.nexu.NexUPreLoader -Dglass.accessible.force=false -Xdebug -Xrunjdwp:transport=dt_socket,address=8989,server=y,suspend=n -jar nexu.jar
start .\java11\bin\javaw -verbose --module-path .\javafx-sdk-11\lib --add-modules=javafx.controls,javafx.fxml,javafx.swing,java.xml,java.xml.crypto,java.smartcardio -Djavafx.preloader=lu.nowina.nexu.NexUPreLoader -Dglass.accessible.force=false -Xdebug -Xrunjdwp:transport=dt_socket,address=8989,server=y,suspend=n -jar nexu.jar