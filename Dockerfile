FROM maven3.6.0-jdk-11:latest AS build
LABEL maintainer="gs-w_eto_eb_federal_employees@usgs.gov"

# Add pom.xml and install dependencies
COPY pom.xml /build/pom.xml
WORKDIR /build
RUN mvn -B dependency:go-offline

# Add source code and (by default) build the jar
COPY src /build/src
RUN mvn -B clean package

FROM usgswma/openjdk:debian-stretch-openjdk-11.0.2-89c4dd2d55ba476c77aa8fd5274dcb8a1ef115b7

RUN apt-get update && apt-get install --no-install-recommends --no-upgrade -y \
    curl \
 && rm -rf /var/lib/apt/lists/*

COPY --chown=1000:1000 --from=build /build/target/ogcproxy-*.jar app.jar

RUN mkdir -p /data/working && mkdir -p /data/shapefiles && chmod -R ugo+rwx /data

USER $USER


CMD ["java", "-jar", "app.jar"]

HEALTHCHECK --interval=30s --timeout=3s \
  CMD curl -H "Accept: application/json" -v -k "http://127.0.0.1:${SERVER_PORT}${SERVER_CONTEXT_PATH}/actuator/health" | grep -q "{\"status\":\"UP\"}" || exit 1