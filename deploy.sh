#!/bin/bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk1.8.0_60.jdk/Contents/Home/
DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
cd $DIR
mvn clean && mvn package
scp target/strudel-web.war debian@autht.massive.org.au:~/
ssh debian@autht.massive.org.au << EOF
sudo rm /var/lib/tomcat7/webapps/strudel-web.war
sudo rm -R /var/lib/tomcat7/webapps/strudel-web/
sudo mv ~debian/strudel-web.war /var/lib/tomcat7/webapps/
EOF
