# ogcproxy
Water Quality Portal (WQP) OGC Proxy

## Local Configuration
This application is configured to be run as a jar.  It can also be run using the command ``` mvn spring-boot:run ``` .
To run in a development environment, create a .env file in the project's home directory with the following:

```
WQP_GEOSERVER_PASSWORD=insert_geoserverpassword_here
WQP_GEOSERVER_HOST=insert_geoserver_host_here
WQP_LAYERBUILDER_HOST=insert_layerbuilder_host_here
SERVER_PORT=8080
SERVER_CONTEXT_PATH=/ogcproxy
WQP_LAYERBUILDER_PORT=80
WQP_GEOSERVER_PORT=80
```

## Building the docker images
The docker image builds the jar and then runs the application.  The application will be available at localhost:8080/ogcproxy
