package gov.usgs.wqp.ogcproxy.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ConfigurationService {

	@Value("${proxy.readLock.timout:30}")
	private Long readLockTimeout;
	@Value("${proxy.writeLock.timout:120}")
	private Long writeLockTimeout;
	@Value("${proxy.thread.sleep:500}")
	private Long threadSleep;

	public Long getReadLockTimeout() {
		return readLockTimeout;
	}
	public Long getWriteLockTimeout() {
		return writeLockTimeout;
	}
	public Long getThreadSleep() {
		return threadSleep;
	}

	//Layer Building
	@Value("${wqp.layerbuilder.proto:http}")
	private String layerbuilderProtocol;
	@Value("${wqp.layerbuilder.host:localhost}")
	private String layerbuilderHost;
	@Value("${wqp.layerbuilder.port:8080}")
	private String layerbuilderPort;
	@Value("${wqp.layerbuilder.context:wqp}")
	private String layerbuilderContext;
	@Value("${wqp.layerbuilder.path:Station/search}")
	private String layerbuilderPath;
	@Value("${layerbuilder.dir.working}")
	private String workingDirectory;
	@Value("${layerbuilder.dir.shapefiles}")
	private String shapefileDirectory;

	public String getWorkingDirectory() {
		return workingDirectory;
	}
	public String getShapefileDirectory() {
		return shapefileDirectory;
	}
	public String getLayerbuilderBaseURI() {
		return layerbuilderProtocol + "://" + layerbuilderHost + ":" + layerbuilderPort + "/" + layerbuilderContext
			+ "/" + layerbuilderPath;
	}

	//GeoServer
	public static final String GEOSERVER_REST = "rest";
	public static final String GEOSERVER_WORKSPACES = "workspaces";
	public static final String GEOSERVER_NAMPESPACES = "namespaces";
	public static final String GEOSERVER_DATASTORES = "datastores";
	public static final String GEOSERVER_RESOURCE = "resource";
	public static final String GEOSERVER_DATA = "data";

	@Value("${wqp.geoserver.proto:http}")
	private String geoserverProtocol;
	@Value("${wqp.geoserver.host:localhost}")
	private String geoserverHost;
	@Value("${wqp.geoserver.port:8080}")
	private String geoserverPort;
	@Value("${wqp.geoserver.context:geoserver}")
	private String geoserverContext;
	@Value("${wqp.geoserver.workspace:wqp_sites}")
	private String geoserverWorkspace;
	@Value("${wqp.geoserver.catchup.time:1000}")
	private long geoserverCatchupTime;
	@Value("${wqp.geoserver.user}")
	private String geoserverUser;
	@Value("${wqp.geoserver.pass}")
	private String geoserverPass;

	public String getGeoserverProtocol() {
		return geoserverProtocol;
	}
	public String getGeoserverHost() {
		return geoserverHost;
	}
	public String getGeoserverPort() {
		return geoserverPort;
	}
	public String getGeoserverContext() {
		return geoserverContext;
	}
	public String getGeoserverWorkspace() {
		return geoserverWorkspace;
	}
	public long getGeoserverCatchupTime() {
		return geoserverCatchupTime;
	}
	public String getGeoserverUser() {
		return geoserverUser;
	}
	public String getGeoserverPass() {
		return geoserverPass;
	}
	public String getGeoserverBaseURI() {
		return geoserverProtocol + "://" + geoserverHost + ":" + geoserverPort + "/" + geoserverContext;
	}
	public String getGeoserverWorkspaceBase() {
		return String.join("/", getGeoserverBaseURI(), GEOSERVER_REST, GEOSERVER_WORKSPACES, geoserverWorkspace);
	}
	public String getGeoserverDataStoreBase() {
		return String.join("/", getGeoserverWorkspaceBase(), GEOSERVER_DATASTORES);
	}

}
