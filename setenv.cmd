REM Convenience script for Windows which sets the environment so you can run a build and run tests
REM You will need to change this to appropriate directories for your installation

REM Note that in addition to this, you need Ant in your path
REM See the readme for details

REM Set environment so you can perform a build

SET RA_HOME=C:\mq\genericjmsra

REM Set J2EE_JAR to a JavaEE 1.4 version of j2ee.jar
SET J2EE_JAR=C:\mq\j2ee\j2ee.jar

REM Set Environment so you can run tests

REM SET S1AS_HOME=C:\Sun\glassfish-2.1.2-v04c

SET S1AS_HOME=C:\GlassFish\glassfish-3.1-b31-12_06_2010\glassfish3\glassfish

SET PATH=%S1AS_HOME%/bin;%PATH%

REM These proxy settings don't seem to work (they do get passed to the JVM, I've checked)
REM SET ANT_OPTS=-Dhttp.proxyHost=emea-proxy.uk.oracle.com -Dhttp.proxyPort=80

