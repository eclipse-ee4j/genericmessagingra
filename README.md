How to do a build of Generic Resource Adapter for JMS

Required tools and libraries
----------------------------
1. j2ee.jar from J2EE 1.4 (J2EE_HOME/lib/j2ee.jar).
   (If you don't have J2EE 1.4, download it from 
    http://java.sun.com/j2ee/1.4/download.html#sdk)

2. ant build tool.
   (This is also bundled with J2EE 1.4 (J2EE_HOME/lib/ant/bin/ant)
    See http://ant.apache.org/ for more details about ant)

Steps to do a build
-------------------

1. Set environment variable J2EE_JAR to the location of a J2EE 1.4 version of j2ee.jar
   This will be used to locate j2ee.jar (as defined in config/config.properties) 
2. Set the environment variable RA_HOME to genericjmsra directory created by the cvs checkout.
3. change the directory to RA_HOME
4. execute "ant clean build"
5. genericra.rar and genericra.jar will be created under RA_HOME/build/dist
