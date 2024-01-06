How to do a build of Generic Resource Adapter for JMS

Required tools and libraries
----------------------------
1. [removed]

2. ant build tool.
   (This is also bundled with J2EE 1.4 (J2EE_HOME/lib/ant/bin/ant)
    See http://ant.apache.org/ for more details about ant)

Steps to do a build
-------------------

1. [removed]
2. Set the environment variable RA_HOME to genericjmsra directory created by the cvs checkout.
3. change the directory to RA_HOME
4. execute "ant clean build"
5. genericra.rar and genericra.jar will be created under RA_HOME/build/dist

Build using Docker
-------------------

1. Install [Docker](https://docs.docker.com/engine/install/).
2. Download `Dockerfile.export.build`.
3. From same directory of the file run the below command:
```bash
$ docker build -f Dockerfile.export.build --output=build --target=export-stage .
```
4. `genericra.rar` and `genericra.jar` will be created under `build` directory.
