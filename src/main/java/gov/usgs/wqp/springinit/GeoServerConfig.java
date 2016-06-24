package gov.usgs.wqp.springinit;

import static org.springframework.util.StringUtils.isEmpty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class GeoServerConfig {
	private static final Logger LOG = LoggerFactory.getLogger(GeoServerConfig.class);

	public static final String GEOSERVER_REST = "rest";
	public static final String GEOSERVER_WORKSPACES = "workspaces";
	public static final String GEOSERVER_NAMPESPACES = "namespaces";
	public static final String GEOSERVER_DATASTORES = "datastores";

	@Autowired
	private Environment environment;

	@Bean
	public String geoserverProtocol() {
		return isEmpty(environment.getProperty("wqp.geoserver.proto"))
				? "http" : environment.getProperty("wqp.geoserver.proto");
	}

	@Bean
	public String geoserverHost() {
		return isEmpty(environment.getProperty("wqp.geoserver.host"))
				? "localhost" : environment.getProperty("wqp.geoserver.host");
	}

	@Bean
	public String geoserverPort() {
		return isEmpty(environment.getProperty("wqp.geoserver.port"))
				? "8080" : environment.getProperty("wqp.geoserver.port");
	}

	@Bean
	public String geoserverContext() {
		return isEmpty(environment.getProperty("wqp.geoserver.context"))
				? "geoserver" : environment.getProperty("wqp.geoserver.context");
	}

	@Bean
	public String geoserverBaseURI() {
		return geoserverProtocol() + "://" + geoserverHost() + ":" + geoserverPort() + "/" + geoserverContext();
	}

	@Bean
	public String geoserverWorkspace() {
		return isEmpty(environment.getProperty("wqp.geoserver.workspace"))
				? "wqp_sites" : environment.getProperty("wqp.geoserver.workspace");
	}

	@Bean
	public String geoserverWorkspaceBase() {
		return String.join("/", geoserverBaseURI(), GEOSERVER_REST, GEOSERVER_WORKSPACES, geoserverWorkspace());
	}

	@Bean
	public String geoserverDataStoreBase() {
		return String.join("/", geoserverWorkspaceBase(), GEOSERVER_DATASTORES);
	}

	@Bean
	public long geoserverCatchupTime() {
		// GeoServer has a race condition from when it finished uploading a shapefile to when
		// the layer and datasource for that shapefile are available.  This is a wait time before
		// we mark the layer AVAILABLE
		// 1000ms or 1s
		long geoserverCatchupTime = 1000;
		try {
			geoserverCatchupTime = Long.parseLong(environment.getProperty("wqp.geoserver.catchup.time"));
		} catch (Exception e) {
			LOG.info("WQPLayerBuildingService() Constructor Exception: Failed to parse property [wqp.geoserver.catchup.time] " +
					  "- Keeping GeoServer Catchup Time default to [" + geoserverCatchupTime + "].\n" + e.getMessage() + "\n");
		}
		return geoserverCatchupTime;
	}

	@Bean
	public String geoserverUser() {
		return environment.getProperty("wqp.geoserver.user");
	}

	@Bean
	public String geoserverPass() {
		return environment.getProperty("wqp.geoserver.pass");
	}

}
