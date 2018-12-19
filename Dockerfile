FROM maven:3.6.0-jdk-8-alpine AS build

WORKDIR /build

COPY pom.xml pom.xml

RUN mvn --batch-mode --errors --strict-checksums --threads 1C org.apache.maven.plugins:maven-dependency-plugin:3.0.2:go-offline

COPY src /build/src
ARG BUILD_COMMAND="mvn --batch-mode --errors --threads 1C clean package verify"
RUN ${BUILD_COMMAND}


FROM usgswma/wma-spring-boot-base

LABEL maintainer="gs-w_eto@usgs.gov"

ENV HEALTHY_RESPONSE_CONTAINS='{"status":"UP"}'
COPY --chown=1000:1000 docker-entrypoint.sh docker-entrypoint.sh
COPY --chown=1000:1000 --from=build /build/target/ogcproxy-*.jar app.jar

CMD ["./docker-entrypoint.sh"]
HEALTHCHECK --interval=30s --timeout=3s \
  CMD curl -k "http://127.0.0.1:8080/actuator/health" | grep -q ${HEALTHY_RESPONSE_CONTAINS} || exit 1