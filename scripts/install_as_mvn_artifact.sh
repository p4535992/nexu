# this is necessary to be able to compile nexu - it references at.gv.egiz.smcc artifact which is not downloadable and only exists within nexu.jar
/c/tools/apache-maven-3.6.3/bin/mvn install:install-file -Dfile=nexu_for_mocca.jar -DgroupId=lu.nowina -DartifactId=nexu_for_mocca -Dversion=1.22 -Dpackaging=jar -DgeneratePom=true

# and this is necessary to be able to compile dss-demonstrations project
/c/tools/apache-maven-3.6.3/bin/mvn install:install-file -Dfile=nexu_for_mocca.jar -DgroupId=at.gv.egiz -DartifactId=smcc -Dversion=1.3.30 -Dpackaging=jar -DgeneratePom=true