# KIT Data Manager - Base Repository Service

This project contains the repository service microservice for the KIT DM infrastructure. The service provides
data resource management, e.g. register DataCite-oriented metadata and upload/download content to data resources.

## How to build

In order to build this microservice you'll need:

* Java SE Development Kit 8 or higher

After obtaining the sources change to the folder where the sources are located perform the following steps:

```
user@localhost:/home/user$ git clone --recursive https://github.com/kit-data-manager/base-repo.git
user@localhost:/home/user$ cd base-repo
user@localhost:/home/user/base-repo$ git submodule foreach git pull origin master
Entering 'libraries/service-base'
From git://github.com/kit-data-manager/service-base
 * branch            master     -> FETCH_HEAD
Already up to date.
user@localhost:/home/user/base-repo$
```

This first step will fetch the most recent version of all included submodules from GitHub, currently this 
is only the [service-base module](https://github.com/kit-data-manager/service-base). 

```
user@localhost:/home/user/base-repo$ cd libraries/service-base
user@localhost:/home/user/base-repo/libraries/service-base$ ./gradlew install
BUILD SUCCESSFUL in 1s
3 actionable tasks: 3 executed
user@localhost:/home/user/base-repo/libraries/service-base$ 
```

In the second step, all submodules have to be built and installed into the local Maven repository. If this step has been
done before for the most recent version of all submodules, it can be skipped. 

```
user@localhost:/home/user/base-repo/libraries/service-base$ cd ../../
user@localhost:/home/user/base-repo$ ./gradlew build
BUILD SUCCESSFUL in 1s
6 actionable tasks: 1 executed, 5 up-to-date
user@localhost:/home/user/base-repo$
```

Finally, the actual microservice can be built. As a result, a fat jar containing the entire service is created at 'build/jars/base-repo.jar'.


## How to start

### Prerequisites

* PostgreSQL 9.1 or higher

### Setup
Before you are able to start the repository microservice, you have to modify the application properties according to your local setup. 
Therefor, copy the file 'settings/application.properties' to your project folder and customize it. Special attentioned should be payed to the
properties in the 'datasource' section as well as the 'jwtSecret', which has to match the 'jwtSecret' provided in the configuration of 
an associated KIT DM authentication microservice.

As soon as 'application.properties' is completed, you may start the repository microservice by executing the following command inside the project folder, 
e.g. where the service has been built before:

```
user@localhost:/home/user/base-repo$ 
user@localhost:/home/user/base-repo$ java -jar build/libs/base-repo.jar

  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::        (v2.0.0.RELEASE)
[...]
1970-01-01 00:00:00.000  INFO 56918 --- [           main] o.s.b.w.embedded.tomcat.TomcatWebServer  : Tomcat started on port(s): 8080 (http) with context path ''

```

If your 'application.properties' is not located inside the project folder you can provide it using the command line argument --spring.config.location=<PATH_TO_APPLICATION.YML>
As soon as the microservice is started, you can browse to 

http://localhost:8090/swagger-ui.html

in order to see available RESTful endpoints and their documentation. Furthermore, you can use this Web interface to test single API calls in order to get familiar with the 
service.

## More Information

* [Information about the DataCite metadata schema](https://schema.datacite.org/)

## License

The KIT Data Manager is licensed under the Apache License, Version 2.0.
