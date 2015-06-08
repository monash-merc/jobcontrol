# Job Control

## Building
Building is easy with Maven - simply clone the repository and run `mvn package` from the repository root directory, where the pom.xml file is. Maven will fetch all dependencies and produce a war package in `/target`.

## Deployment
Sample configuration files are located in `/config_example`. After customising, this file must be found in the classpath of the application. To deploy the server, copy the war file into a Java Servlet container, such as Apache Tomcat. This deployment may be accessed via Tomcat directly, or through Apache HTTPd with the Tomcat connector.

This server requires access to a MySQL database and a Guacamole VNC proxy that uses the [MASSIVE Guacamole authorization plugin](https://github.com/monash-merc/massive-guacamole-remote).
