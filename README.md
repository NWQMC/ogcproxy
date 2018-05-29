# ogcproxy
Water Quality Portal (WQP) OGC Proxy

## Local Configuration
This application is configured to be run in a Tomcat container. The development configuration (context.xml and ogcproxy.properties) can be copied to your local Tomcat.

Security can be enabled by adding the following to the Tomcat's context.xml:

```
    <Parameter name="spring.profiles.active" value="default,swagger,internal" />
    <Parameter name="oauthResourceKeyUri" value = "<<url for token_key>>"/>
    <Parameter name="oauthResourceId" value="wqp"/>
```
