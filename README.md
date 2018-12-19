# ogcproxy
Water Quality Portal (WQP) OGC Proxy

## Local Configuration
This application is configured to be run as a jar.  It can also be run using the command ``` mvn spring-boot:run ``` .
To run in a development environment, create an application.yml file in the project's home directory with the following:

```
---
  wqp:
    geoserver:
      pass: insert_geoserver_password_here
```

## Building the docker images
The docker image builds the jar and then runs the application.  The application will be available at localhost:8080/
