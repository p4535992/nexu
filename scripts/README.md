## The problem
Nexu original code references classes in at least two packages that are not in the Maven central, or the DSS distribution repository., namely 
  - `at.gv.egiz.smcc.*` and
  - `eu.europa.esig.dss.token.mocca.*`
  
## Solution
In order to be able to build, instead of distributing these files anew, which might not possible because of copyright constrains, a hack was devised to get these classes from the original Nexu distrubuted file that is available through the official [DSS demo app](https://ec.europa.eu/digital-building-blocks/DSS/webapp-demo), and finally leads to the Releases section of the Nexu original github repository (here)[https://github.com/nowina-solutions/nexu/releases/download/nexu-1.22/nexu-bundle-1.22.zip]. 

## Steps to solve
  - First create the stripped local JAR
    - download `nexu-bundle-1.22.zip` from the above link. It is hefty because it contains bundled JRE distribution
    - find and extract `nexu.jar` from the above archive
    - open `nexu.jar` with a tool that can modify a ZIP file, e.g. `7-zip`
    - delete everything from the archive except for the following directories and files:
      - eu\europa\esig\dss\token\mocca\
      - at\gv\egiz\smcc\
      - META-INF\maven\at.gv.egiz\
      - META-INF\services\
      - META-INF\MANIFEST.MF
      - LICENSE*
  - Save the resulting `nexu.jar` file, stripped of all but the necessary files, as `nexu_for_mocca.jar`.
  - Run commands described in `install_as_mvn_artifact.sh` for the above `nexu_for_mocca.jar` to install this single library as two separate Maven artifacts that are referenced in Nexu code (current fork) - change the location of Maven to suit your Maven installation folder
- 