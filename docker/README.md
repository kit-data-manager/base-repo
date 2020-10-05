![Docker Cloud Build Status](https://img.shields.io/docker/cloud/build/kitdm/base-repo)
![Docker Image Version (latest by date)](https://img.shields.io/docker/v/kitdm/base-repo)

# Docker Configuration - base-repo

This repository contains a the docker configuration files for the base-repo service of KIT DM 2.0 repository platform. It is build and hosted at [DockerHub](https://hub.docker.com/) and can be found under the namespace ***kitdm***. 

## Prerequisites

* docker (tested with 18.09.2)

## Building and Startup

Typically, there is no need for locally building images as all version are accessible via [DockerHub](https://hub.docker.com/) ([kitdm/base-repo](https://hub.docker.com/r/kitdm/base-repo)).

Running for example a base-repo instance can be achieved as follows:

```
user@localhost:/home/user/$ docker run -p 8080:8080 kitdm/base-repo
[...]
user@localhost:/home/user/$
```

In some cases, you may want to change the configuration of the service instance. All service-specific configuration is located in each image at

```/base-repo/conf/application.properties```

You can easily overwrite this file by creating an own Dockerfile, which looks as follows in case of the base-repo service:

```
FROM kitdm/base-repo:latest

COPY application.properties /base-repo/config/application.properties
```

Afterwards, you have to build the modified image locally by calling:

```
user@localhost:/home/user/my-base-repo/$ docker build .
[...]
user@localhost:/home/user/my-base-repo/$
```

Now, you can start the container using your modified configuration.

## License

The KIT Data Manager is licensed under the Apache License, Version 2.0.
