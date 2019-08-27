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
	@Value("${wqp.layerbuilder.url}")
	private String layerbuilderUrl;

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
		return layerbuilderUrl;
	}

	//GeoServer
	public static final String GEOSERVER_REST = "rest";
	public static final String GEOSERVER_WORKSPACES = "workspaces";
	public static final String GEOSERVER_NAMPESPACES = "namespaces";
	public static final String GEOSERVER_DATASTORES = "datastores";
	public static final String GEOSERVER_RESOURCE = "resource";
	public static final String GEOSERVER_DATA = "data";

	@Value("${wqp.geoserver.host:localhost}")
	private String geoserverHost;

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

	public String getGeoserverHost() {
		return geoserverHost;
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
		return  geoserverHost + "/" + geoserverContext;
	}
	public String getGeoserverWorkspaceBase() {
		return String.join("/", getGeoserverBaseURI(), GEOSERVER_REST, GEOSERVER_WORKSPACES, geoserverWorkspace);
	}
	public String getGeoserverDataStoreBase() {
		return String.join("/", getGeoserverWorkspaceBase(), GEOSERVER_DATASTORES);
	}

}
