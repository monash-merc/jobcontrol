Administration guide
====================

Dependencies
------------
* Java 7+
* Maven 3
* Bower
* Apache Tomcat 7+
* `SSH-AuthZ server`_
* Guacamole_
* `MASSIVE Guacamole authorisation plugin`_

Guacamole should be installed on the same server as Strudel Web, and protected by the same mechanism as SSH-AuthZ (e.g. AAF_)

.. _Guacamole: http://guac-dev.org/
.. _MASSIVE Guacamole authorisation plugin: https://github.com/jasonrig/massive-guacamole-remote.git
.. _AAF: http://aaf.edu.au/

Building
--------
Building is easy with Maven - simply clone the repository and run :code:`mvn package` from the repository root directory,
where the pom.xml file is. Maven will fetch all dependencies and produce a war package in :code:`/target`. This war file
should then be deployed on a servlet container of your choice (e.g. Apache Tomcat).

Configuration
-------------
Strudel Web requires that a configuration file called :code:`strudel-web.properties` is installed in the class path of
the servlet container. On Debian for Apache Tomcat 7, this could be :code:`/usr/share/tomcat7/`, for example.

Example configuration
~~~~~~~~~~~~~~~~~~~~~
.. literalinclude:: ../config_example/strudel-web.properties
   :linenos:

* :code:`allow-invalid-cert`: If true, SSL certificate exceptions are ignored when fetching system configurations (line 11)
* :code:`oauth-redirect`: This URL assists in constructing the OAuth request redirect URL. It should always end with "/api/oauth/callback", and should begin with the the public URL for your Strudel Web installation.

**ssh-cert-backend** block:

A repeating block of parameters that describe OAuth endpoints, and client credentials, for an `SSH-AuthZ server`_ installation.
There may be many SSH-AuthZ servers listed, with each parameter block incrementing the integer suffix.

.. _SSH-AuthZ server: https://github.com/monash-merc/ssh-authz

**system-configuration** block:

A repeating block of system configurations. Each must contain a name (:code:`system-configuration-name`), a
URL from which to fetch the Strudel JSON configuration file as used by the desktop application
(:code:`system-configuration-json-url`), and a list of allowed authorisation backends
(:code:`system-configuration-auth-backends`). If multiple authorisation backends are allowed for a given configuration,
the list may be specified either as a comma-separated list, or by repeating the parameter as necessary (see `Apache Commons Configuration`_ docs).
:code:`system-configuration-auth-backends` **must exactly match a corresponding** :code:`ssh-cert-backend-name`.

.. _Apache Commons Configuration: https://commons.apache.org/proper/commons-configuration/userguide_v1.10/howto_properties.html#Lists_and_arrays

**guac** block:

:code:`guacd-host` is the host to which the VNC connection is forwarded. Currently this should always be localhost.
:code:`guac-mysql-...` all relate to MySQL database connection credentials (host, port, user name, password, and database name).