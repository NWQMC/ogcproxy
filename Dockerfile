FROM maven:3.6.0-jdk-8-alpine AS build

WORKDIR /build

COPY pom.xml pom.xml

RUN mvn clean

COPY src /build/src
ARG BUILD_COMMAND="mvn package"
RUN ${BUILD_COMMAND}


FROM usgswma/openjdk:debian-stretch-openjdk-11.0.2-89c4dd2d55ba476c77aa8fd5274dcb8a1ef115b7

LABEL maintainer="gs-w_eto@usgs.gov"

ENV HEALTHY_RESPONSE_CONTAINS='{"status":"UP"}'
COPY --chown=1000:1000 --from=build /build/target/ogcproxy-*.jar app.jar

HEALTHCHECK --interval=30s --timeout=3s \
  CMD curl -k "http://127.0.0.1:8080/actuator/health" | grep -q ${HEALTHY_RESPONSE_CONTAINS} || exit 1