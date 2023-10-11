rem Â© Nowina Solutions, 2015-2015
rem
rem ConceÌ�deÌ�e sous licence EUPL, version 1.1 ou â€“ deÌ€s leur approbation par la Commission europeÌ�enne - versions ulteÌ�rieures de lâ€™EUPL (la Â«LicenceÂ»).
rem Vous ne pouvez utiliser la preÌ�sente Å“uvre que conformeÌ�ment aÌ€ la Licence.
rem Vous pouvez obtenir une copie de la Licence aÌ€ lâ€™adresse suivante:
rem
rem http://ec.europa.eu/idabc/eupl5
rem
rem Sauf obligation légale ou contractuelle écrite, le logiciel distribué sous la Licence est distribué «en l’état»,
rem SANS GARANTIES OU CONDITIONS QUELLES QU’ELLES SOIENT, expresses ou implicites.
rem Consultez la Licence pour les autorisations et les restrictions linguistiques spécifiques relevant de la Licence.

@echo off
rem start has a problem with first quoted parameter and this is a workaround
rem because we have absolute paths here (because there no easy way to specify _start in_ directory for a service)
rem we have to wrap them in double-quotes for the very possible case that they have spaces in them
start "" "__{app}"\java\bin\javaw.exe -Djavafx.preloader=lu.nowina.nexu.NexUPreLoader -Dglass.accessible.force=false -jar "__{app}"\nexu.jar