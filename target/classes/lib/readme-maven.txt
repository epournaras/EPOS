Unforunately several epos libraries related to protopeer cannot be found
on Maven repositories and due to licensing the source code cannot be distribute. 
For these reasons they are setup as jars in local repositories to make the project
run smoother. Please consider the below in case you need to tinker with the EPOS
poject and the way it is build/deployed.

### Manual Installlation ###
With the console open in this folder, the user should run the commands below.

mvn install:install-file -Dfile=ProtoPeer.jar -DgroupId=protopeer \
    -DartifactId=core -Dversion=1.0 -Dpackaging=jar

mvn install:install-file -Dfile=DSUtil.jar -DgroupId=protopeer \
-DartifactId=dsutil -Dversion=1.0 -Dpackaging=jar

mvn install:install-file -Dfile=TreeGateway.jar -DgroupId=protopeer \
-DartifactId=tree -Dversion=1.0 -Dpackaging=jar


### Local Repository Solution ### 
This solution creates a self contained maven repo inside the project.
Very useful to create an independed epos project that does not need any 
console interactions from the user to be setup and requires only a maven
compliant IDE and JRE.

mvn org.apache.maven.plugins:maven-install-plugin:2.5.2:install-file \
-Dfile=ProtoPeer.jar -DgroupId=protopeer \
-DartifactId=core -Dversion=1.0 \
-Dpackaging=jar -DlocalRepositoryPath=.

mvn org.apache.maven.plugins:maven-install-plugin:2.5.2:install-file \
-Dfile=DSUtil.jar -DgroupId=protopeer \
-DartifactId=dsutil -Dversion=1.0 \
-Dpackaging=jar -DlocalRepositoryPath=.

mvn org.apache.maven.plugins:maven-install-plugin:2.5.2:install-file \
-Dfile=TreeGateway.jar -DgroupId=protopeer \
-DartifactId=tree -Dversion=1.0 \
-Dpackaging=jar -DlocalRepositoryPath=.

Author: Thomas Asikis, asikist@ethz.ch