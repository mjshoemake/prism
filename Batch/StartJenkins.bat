echo OFF
C:
cd \WorkApps\jenkins
echo Starting Jenkins server...
call java -jar jenkins.war --httpPort=8088
echo Starting Jenkins server... Done.
