FROM openjdk:11-stretch AS build-env-java
MAINTAINER webmaster@datamanager.kit.edu
LABEL stage=build-env

RUN apt-get update && \
    apt-get upgrade --assume-yes && \
    apt-get install --assume-yes git && \
    mkdir -p /base-repo/

FROM build-env-java AS build-base-repo
MAINTAINER webmaster@datamanager.kit.edu
LABEL stage=build-contains-sources

COPY build.sh /base-repo/

RUN sh /base-repo/build.sh

FROM openjdk:11-stretch AS run-base-repo
MAINTAINER webmaster@datamanager.kit.edu
LABEL stage=run

RUN mkdir -p /var/repository
RUN mkdir -p /base-repo/config/
COPY --from=build-base-repo /base-repo/base-repo/build/libs/base-repo.jar /base-repo/base-repo.jar
COPY --from=build-base-repo /base-repo/base-repo/config/application-default.properties /base-repo/application.properties
COPY run.sh /base-repo/

EXPOSE 8080
ENTRYPOINT ["sh", "/base-repo/run.sh"]
