# KIT Data Manager - Base Repository Service

[![Build Status](https://github.com/kit-data-manager/base-repo/actions/workflows/gradle.yml/badge.svg)](https://github.com/kit-data-manager/base-repo/actions/workflows/gradle.yml)
[![Codecov](https://codecov.io/gh/kit-data-manager/base-repo/branch/master/graph/badge.svg)](https://codecov.io/gh/kit-data-manager/base-repo)
![License](https://img.shields.io/github/license/kit-data-manager/base-repo.svg)
[![Docker Cloud Build Status](https://img.shields.io/docker/cloud/build/kitdm/base-repo)](https://hub.docker.com/r/kitdm/base-repo/tags)
![Docker Image Version (latest semver)](https://img.shields.io/docker/v/kitdm/base-repo)
[![SQAaaS badge shields.io](https://img.shields.io/badge/sqaaas%20software-silver-lightgrey)](https://api.eu.badgr.io/public/assertions/onNKx_lhTn68bPKnMAg-eQ "SQAaaS silver badge achieved")
[![DOI](https://zenodo.org/badge/DOI/10.5281/zenodo.7660036.svg)](https://doi.org/10.5281/zenodo.7660036)

This project contains the repository service microservice for the KIT DM infrastructure. The service provides
data resource management, e.g. register DataCite-oriented metadata and upload/download content to data resources.

## How to build

In order to build this microservice you'll need:

* Java SE Development Kit 17 or higher

After obtaining the sources change to the folder where the sources are located perform the following steps:

```bash
user@localhost:/home/user/base-repo$ ./gradlew -Dprofile=minimal build
Running gradle version: 7.4.2
Building base-repo version: 1.3.0
JDK version: 11
Using minimal profile for building base-repoo
<-------------> 0% EXECUTING [0s]
[...]
user@localhost:/home/user/base-repo$
```

The Gradle wrapper will now take care of downloading the configured version of Gradle, checking out all required libraries, build these
libraries and finally build the base-repo microservice itself. As a result, a fat jar containing the entire service is created at 'build/libs/base-repo.jar'.

## How to start

### Prerequisites

* PostgreSQL 9.1 or higher
* RabbitMQ 3.7.3 or higher (in case you want to use the messaging feature, which is recommended)
* Elastic 8.X or higher (in case you want to use the search feature)

### Setup

Before you are able to start the repository microservice, you have provide a configuration file according to your local setup.
Therefor, copy the file 'config/application-default.properties' to your project folder, rename it to 'application.properties' and customize it as required. Special attentioned should be payed to the datasource url as well as to the repository base path. Also, the property 'repo.messaging.enabled' should be changed to 'true' in case you want to use the messaging feature of the repository.

As soon as you finished modifying 'application.properties', you may start the repository microservice by executing the following command inside the project folder,
e.g. where the service has been built before:

```bash
user@localhost:/home/user/base-repo$ ./build/libs/base-repo.jar

  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::        (v2.7.5)
[...]
1970-01-01 00:00:00.000  INFO 56918 --- [           main] o.s.b.w.embedded.tomcat.TomcatWebServer  : Tomcat started on port(s): 8080 (http) with context path ''

```

If your 'application.properties' is not located inside the project folder you can provide it using the command line argument --spring.config.location=<PATH_TO_APPLICATION.PROPERTIES>

As soon as the microservice is started, you can browse to

<http://localhost:8090/swagger-ui.html>

in order to see available RESTful endpoints and their documentation.

### Enhanced Startup

At certain points, base-repo offers and will offer extension points allowing to add custom features that are not part of the default distribution, e.g. custom message handlers. If you are familiar with software development, it might be no big deal to include an additional dependency to 'build.gradle' of base-repo. However, in some cases this might not be desirable or possible. Therefor, base-repo allows to place additional libraries required at runtime in a separate folder which is then loaded as soon as the microservice starts and made available using the dependency injection feature of Spring Boot.

In order to tell Spring Boot where to look for additional libraries, you have to define an environment variable JAVA_OPTS looking as follows:

```bash
export JAVA_OPTS="-cp .:./config:./base-repo.jar -Dloader.path=./base-repo.jar,./lib/,."
```

The first part '-cp' has to contain three elements divided by ':':

1. The configuration folder where your application.properties is located (this element can be omitted, if application.properties
is located in the current folder),
2. the current folder,
3. and the microservice jar file.

The second part '-Dloader.path' basically contains the same information as '-cp' but with the difference, that the config folder is not required, whereas the folder
containing all additional libraries has to be provided, in our case it's './lib'.

Please keep in mind that all arguments shown in the example assume, that you are in the same folder where your microservice jar file is located and that you start the service
by calling './base-repo.jar'. If your microservice jar is located elsewhere, you should consider to provide absolute paths for all arguments above.
In case you want to choose a different folder for placing your additional libraries, you have to rename it in JAVA_OPTS accordingly.

What you now have to do before you start the microservice is to place additional jar files (and required dependencies!) in the 'lib' folder. At the next startup, the new functionality should be available.

## More Information

* [Getting Started & Documentation](https://kit-data-manager.github.io/webpage/base-repo/index.html)
* [API documentation](https://kit-data-manager.github.io/webpage/base-repo/documentation/api-docs.html)
* [Docker container]([https://hub.docker.com/r/kitdm/base-repo](https://github.com/kit-data-manager/base-repo/pkgs/container/base-repo%2Fbase-repo))
* [Information about the DataCite metadata schema](https://schema.datacite.org/)

## License

The KIT Data Manager is licensed under the Apache License, Version 2.0.
