package gov.usgs.wqp.ogcproxy.services.wqp;

import static org.springframework.util.StringUtils.isEmpty;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.web.servlet.ModelAndView;

import gov.usgs.wqp.ogcproxy.exceptions.OGCProxyException;
import gov.usgs.wqp.ogcproxy.exceptions.OGCProxyExceptionID;
import gov.usgs.wqp.ogcproxy.model.FeatureDAO;
import gov.usgs.wqp.ogcproxy.model.cache.DynamicLayerCache;
import gov.usgs.wqp.ogcproxy.model.features.SimplePointFeature;
import gov.usgs.wqp.ogcproxy.model.ogc.services.OGCServices;
import gov.usgs.wqp.ogcproxy.model.parameters.SearchParameters;
import gov.usgs.wqp.ogcproxy.model.parser.xml.wqx.SimplePointParser;
import gov.usgs.wqp.ogcproxy.utils.ShapeFileUtils;
import gov.usgs.wqp.ogcproxy.utils.TimeProfiler;
import gov.usgs.wqp.ogcproxy.utils.WQPUtils;

public class WQPLayerBuildingService {
	private static final Logger LOG = LoggerFactory.getLogger(WQPLayerBuildingService.class);
	
	@Autowired
	private WQPDynamicLayerCachingService layerCachingService;

	private static final String WMS_GET_CAPABILITIES_CONTENT = "<Layer queryable=\"1\">" +
			"<Name>wqp_sites</Name>" +
			"<Title>wqp_sites</Title>" +
			"<Abstract />" +
			"<KeywordList>" +
			"<Keyword>features</Keyword>" +
			"<Keyword>wqp_sites</Keyword>" +
			"</KeywordList>" +
			"<CRS>EPSG:4326</CRS>" +
			"<EX_GeographicBoundingBox>" +
			"<westBoundLongitude>-179.144806</westBoundLongitude>" +
			"<eastBoundLongitude>179.76416</eastBoundLongitude>" +
			"<southBoundLatitude>18.913826</southBoundLatitude>" +
			"<northBoundLatitude>71.332649</northBoundLatitude>" +
			"</EX_GeographicBoundingBox>" +
			"<BoundingBox CRS=\"CRS:84\" minx=\"-179.144806\" miny=\"18.913826\"" +
			" maxx=\"179.76416\" maxy=\"71.332649\" />" +
			"<BoundingBox CRS=\"EPSG:4326\" minx=\"18.913826\" miny=\"-179.144806\"" +
			" maxx=\"71.332649\" maxy=\"179.76416\" />" +
			"<Style>" +
			"<Name>point</Name>" +
			"<Title>Default Point</Title>" +
			"<Abstract>A sample style that draws a point</Abstract>" +
			"</Style>" +
			"</Layer>";
	
	/**
	 * WFS GetFeature allows the use of the searchParams parameter.  Declare it in the GetCapabilities
	 * document with the ows:AnyValue indicator (http://schemas.opengis.net/ows/1.1.0/owsDomainType.xsd)
	 */
	private static final String WFS_GET_CAPABILITIES_CONTENT = "<ows:Parameter name=\"searchParams\">" +
			"<ows:AnyValue />" +
			"</ows:Parameter>" +
			"<ows:Parameter name=\"typeName\">" +
			"<ows:AllowedValues>" +
			"<ows:Value>wqp_sites</ows:Value>" +
			"</ows:AllowedValues>" +
			"</ows:Parameter>";
	
	@Autowired
	private Environment environment;
	private static boolean initialized;
	
	private static String geoserverProtocol   = "http";
	private static String geoserverHost       = "localhost";
	private static String geoserverPort       = "8080";
	private static String geoserverContext    = "geoserver";
	private static String wqpWorkspace        = "qw_portal_map";
	private static String geoserverUser       = "";
	private static String geoserverPass       = "";
	private static String geoserverBaseURI    = "";
	private static String geoserverRest       = "rest";
	private static String geoserverWorkspaces = "workspaces";
	private static String geoserverNamespaces = "namespaces";
	private static String geoserverDataStores = "datastores";
	
	private static String simpleStationProtocol = "http";
	private static String simpleStationHost     = "cida-eros-wqpdev.er.usgs.gov";
	private static String simpleStationPort     = "8080";
	private static String simpleStationContext  = "/qw_portal_core";
	private static String simpleStationPath     = "/simplestation/search";
	private static String simpleStationRequest  = "";
	
	private static String workingDirectory      = "";
	private static String shapefileDirectory    = "";
	
	// 15 minutes, default is infinite
	private static int connection_ttl            = 15 * 60 * 1000;
	private static int connections_max_total     = 256;
	private static int connections_max_route     = 32;
	// 5 minutes, default is infinite
	private static int client_socket_timeout     = 5 * 60 * 1000;
    // 15 seconds, default is infinite
	private static int client_connection_timeout = 15 * 1000;
	
  	// GeoServer has a race condition from when it finished uploading a shapefile to when
	// the layer and datasource for that shapefile are available.  This is a wait time before
	// we mark the layer AVAILABLE
    // 1000ms or 1s
	private static long geoserverCatchupTime     = 1000;
	
	private CloseableHttpClient httpClient;
	
	private static final WQPLayerBuildingService INSTANCE = new WQPLayerBuildingService();
	
	/**
	 * Private Constructor for Singleton Pattern
	 */
	private WQPLayerBuildingService() {
	}
	
	/**
	 * Singleton accessor
	 *
	 * @return WQPLayerBuildingService instance
	 */
	public static WQPLayerBuildingService getInstance() {
		return INSTANCE;
	}
	
	@PostConstruct
	public void initialize() {
		LOG.debug("WQPLayerBuildingService.initialize() called");
		
		/*
		 * Since we are using Spring DI we cannot access the environment bean
		 * in the constructor.  We'll just use a locked initialized variable
		 * to check initialization after instantiation and set the env
		 * properties here.
		 */
		if (initialized) {
			return;
		}
		synchronized (WQPLayerBuildingService.class) {
			if (initialized) {
				return;
			}
			initialized = true;
			/*
			 * Get the GeoServer properties that we will be hitting
			 */
			String tmp = environment.getProperty("wqp.geoserver.proto");
			if (!isEmpty(tmp)) {
				geoserverProtocol = tmp;
			}
			tmp = environment.getProperty("wqp.geoserver.host");
			if (!isEmpty(tmp)) {
				geoserverHost = tmp;
			}
			tmp = environment.getProperty("wqp.geoserver.port");
			if (!isEmpty(tmp)) {
				geoserverPort= tmp;
			}
			tmp = environment.getProperty("wqp.geoserver.context");
			if (!isEmpty(tmp)) {
				geoserverContext = tmp;
			}
			tmp = environment.getProperty("wqp.geoserver.workspace");
			if (!isEmpty(tmp)) {
				wqpWorkspace = tmp;
			}
			tmp = environment.getProperty("wqp.geoserver.user");
			if (!isEmpty(tmp)) {
				geoserverUser = tmp;
			}
			tmp = environment.getProperty("wqp.geoserver.pass");
			if (!isEmpty(tmp)) {
				geoserverPass = tmp;
			}
			
			geoserverBaseURI = geoserverProtocol + "://" + geoserverHost + ":" + geoserverPort + "/" + geoserverContext;
			
			/*
			 * Get all URL properties for calling WQP for data
			 */
			tmp = environment.getProperty("layerbuilder.simplestation.proto");
			if (!isEmpty(tmp)) {
				simpleStationProtocol = tmp;
			}
			simpleStationRequest = simpleStationProtocol;
			tmp = environment.getProperty("layerbuilder.simplestation.host");
			if (!isEmpty(tmp)) {
				simpleStationHost = tmp;
			}
			simpleStationRequest += "://" + simpleStationHost;
			tmp = environment.getProperty("layerbuilder.simplestation.port");
			if (!isEmpty(tmp)) {
				simpleStationPort = tmp;
			}
			simpleStationRequest +=  ":" + simpleStationPort;
			tmp = environment.getProperty("layerbuilder.simplestation.context");
			if (!isEmpty(tmp)) {
				simpleStationContext = tmp;
			}
			simpleStationRequest += simpleStationContext;
			tmp = environment.getProperty("layerbuilder.simplestation.path");
			if (!isEmpty(tmp)) {
				simpleStationPath = tmp;
			}
			simpleStationRequest += simpleStationPath;
			LOG.debug("WQPLayerBuildingService() Constructor Info: Setting SimpleStation Request to [" + simpleStationRequest + "]");
			
			/*
			 * Configure working directories
			 */
			tmp = environment.getProperty("layerbuilder.dir.working");
			if (!isEmpty(tmp)) {
				workingDirectory = tmp;
			}
			tmp = environment.getProperty("layerbuilder.dir.shapefiles");
			if (!isEmpty(tmp)) {
				shapefileDirectory = tmp;
			}
			try {
				geoserverCatchupTime = Long.parseLong(environment.getProperty("layerbuilder.geoserver.catchup.time"));
			} catch (Exception e) {
				LOG.error("WQPLayerBuildingService() Constructor Exception: Failed to parse property [layerbuilder.geoserver.catchup.time] " +
						  "- Keeping GeoServer Catchup Time default to [" + geoserverCatchupTime + "].\n" + e.getMessage() + "\n");
			}
			
			/*
			 * Build httpclient
			 */
			// Initialize connection manager, this is thread-safe.  if we use this
			// with any HttpClient instance it becomes thread-safe.
			PoolingHttpClientConnectionManager clientConnectionManager = new PoolingHttpClientConnectionManager(
					connection_ttl, TimeUnit.MILLISECONDS);
			clientConnectionManager.setMaxTotal(connections_max_total);
			clientConnectionManager.setDefaultMaxPerRoute(connections_max_route);

			RequestConfig config = RequestConfig.custom().setConnectTimeout(client_connection_timeout)
					.setSocketTimeout(client_socket_timeout).build();

			httpClient = HttpClients.custom().setConnectionManager(clientConnectionManager)
					.setDefaultRequestConfig(config).build();
		}
	}
	
	public String addGetCapabilitiesInfo(OGCServices serviceType, String serverContent) {
		/*
		 * For now we are assuming all GetCapabilities responses are XML.
		 *
		 * We are going to take the easy way out.  Instead of creating an XML
		 * document and parsing it and figuring out where specific elements are
		 * blah blah blah, we are just going to insert our specific XML blob in
		 * the location it needs to be.
		 *
		 * The main reason I am doing this is because its fast and cheap.  A
		 * very large String of XML is smaller in memory than an entire XML DOM
		 * object of the same string.
		 */
		StringBuffer newContent = new StringBuffer();
		
		switch (serviceType) {
			case WMS:
				/*
				 * For WMS, our XLM blob just needs to live inside the parent
				 * <Layer> element.  Since it can live ANYWHERE in the parent
				 * <Layer></Layer> element we will just look for the LAST
				 * closing </Layer> tag and insert our stuff before it.
				 */
				int closingParentTag = serverContent.lastIndexOf("</Layer>");
				if (closingParentTag == -1) {
					LOG.warn("WQPLayerBuildingService.addGetCapabilitiesInfo() Warning: WMS GetCapabilities response from mapping service does not contain a closing </Layer> element.  Returning silently...");
					return serverContent;
				}
				
				newContent.append(serverContent.substring(0, closingParentTag));
				newContent.append(WMS_GET_CAPABILITIES_CONTENT);
				newContent.append(serverContent.substring(closingParentTag, serverContent.length()));
				break;
			
			case WFS:
				/*
				 * WFS GetCapabilities response is different than WMS.  Most of our
				 * WFS requests will center around GetFeature so we need to add
				 * the "searchParams" parameter definition to the GetFeature operation
				 * description.
				 *
				 * We will look for string token: "<ows:Operation name="GetFeature">"
				 * and then look for the closing "</ows:DCP>" tag (a required child
				 * element for an operation http://schemas.opengis.net/ows/1.1.0/owsOperationsMetadata.xsd).
				 *
				 * Once we found the closing </ows:DCP> tag of the GetFeature operation, we insert
				 * our XML after it and append the rest of the document below it.  If we dont find
				 * this tag we'll just insert it right before the closing </ows:Operation> tag.
				 */
				int getFeatureTag = serverContent.lastIndexOf("<ows:Operation name=\"GetFeature\">");
				if (getFeatureTag == -1) {
					LOG.warn("WQPLayerBuildingService.addGetCapabilitiesInfo() Warning: WFS GetCapabilities response from mapping service does not contain a <ows:Operation name=\"GetFeature\"> element.  Returning silently...");
					return serverContent;
				}
				
				int insertTag = serverContent.indexOf("</ows:DCP>", getFeatureTag);
				if (insertTag == -1) {
					LOG.warn("WQPLayerBuildingService.addGetCapabilitiesInfo() Warning: WFS GetCapabilities response from mapping service does not contain a closing </ows:DCP> element from the location of the <ows:Operation name=\"GetFeature\"> tag.  Looking for closing Operation tag.");
					
					insertTag = serverContent.indexOf("</ows:Operation>", getFeatureTag);
					if (insertTag == -1) {
						LOG.warn("WQPLayerBuildingService.addGetCapabilitiesInfo() Warning: WFS GetCapabilities response from mapping service does not contain a closing </ows:Operation> element from the location of the <ows:Operation name=\"GetFeature\"> tag.  Returning silently...");
						return serverContent;
					}
				} else {
					insertTag += "</ows:DCP>".length();
				}
				
				newContent.append(serverContent.substring(0, insertTag));
				newContent.append(WFS_GET_CAPABILITIES_CONTENT);
				newContent.append(serverContent.substring(insertTag, serverContent.length()));
				break;
			
			default:
				break;
		}

		return newContent.toString();
	}
	
	public ModelAndView getCacheStatus() {
		ModelAndView mv = new ModelAndView("wqp_cache_status.jsp");
		
		mv.addObject("site", "WQP Layer Building Service");
		mv.addObject("cache", layerCachingService.getCacheStore().values());
		
		return mv;
	}
	
	public ModelAndView clearCache() {
		ModelAndView mv = new ModelAndView("wqp_cache_cleared.jsp");
		
		mv.addObject("site", "WQP Layer Building Service");
		mv.addObject("count", layerCachingService.clearCache());
		
		return mv;
	}
	
	public String buildDynamicLayer(SearchParameters<String, List<String>> searchParams) throws OGCProxyException {
		String layerName = DynamicLayerCache.DYNAMIC_LAYER_PREFIX + searchParams.unsignedHashCode();
		SimpleFeatureType featureType;
		
		try {
			featureType = SimplePointFeature.getFeatureType();
		} catch (Exception e) {
			String msg = "WQPLayerBuildingService.buildDynamicLayer() EXCEPTION : Unable to create SimpleFeatureBuilder.  Throwing Exception...";
			LOG.error(msg, e);
			OGCProxyExceptionID id = OGCProxyExceptionID.GEOTOOLS_FEATUREBUILDER_ERROR;
			throw new OGCProxyException(id, "WQPLayerBuildingService", "buildDynamicLayer()", msg);
		}

		File dataFile = getSimpleStationData(searchParams, layerName);
		TimeProfiler.startTimer("WQPLayerBuildingService.buildDynamicLayer() INFO: ShapeFileConverter Overall Time");

		List<FeatureDAO> featureList;
		try {
			/*
			 * Parse the input
			 */
			LOG.debug("WQPLayerBuildingService.buildDynamicLayer() INFO: o ----- Parsing input (" + dataFile.getAbsolutePath() + ")");
			featureList = parseInput(dataFile.getAbsolutePath(), featureType);
			LOG.debug("WQPLayerBuildingService.buildDynamicLayer() INFO: o ----- Parsing Complete");
		} finally {
			if (!dataFile.delete()) {
				LOG.error("unable to delete file:" + dataFile.getAbsolutePath());
			}
		}
		
		/*
		 * Create the shapefile
		 */
		LOG.debug("WQPLayerBuildingService.buildDynamicLayer() INFO: o ----- Creating Shapefile (" + layerName + ")");
		if ( ! createShapeFile(shapefileDirectory, layerName, true, featureList, featureType) ) {
			// we force zip file as GeoServer requires it
			String msg = "WQPLayerBuildingService.buildDynamicLayer() EXCEPTION : Creating the shapefile failed.  Throwing Exception...";
			LOG.error(msg);
			OGCProxyExceptionID id = OGCProxyExceptionID.SHAPEFILE_CREATION_ERROR;
			throw new OGCProxyException(id, "WQPLayerBuildingService", "buildDynamicLayer()", msg);
		}
		LOG.debug("WQPLayerBuildingService.buildDynamicLayer() INFO: o ----- Creating Shapefile Complete");

		/* 
		 * Upload the shapefile to geoserver
		 */
		uploadShapefile(layerName);
		
		TimeProfiler.endTimer("WQPLayerBuildingService.buildDynamicLayer() INFO: ShapeFileConverter Overall Time", LOG);
		
		return layerName;
	}
	
	private File getSimpleStationData(SearchParameters<String, List<String>> searchParams, String layerName) throws OGCProxyException {
		
		String dataFilename = WQPUtils.retrieveSearchParamData(httpClient, searchParams, simpleStationRequest, workingDirectory, layerName);
		
		if (isEmpty(dataFilename)) {
			/*
			 * Did not receive any data from the server for this request.  Cannot create layer.
			 */
			String msg = "WQPLayerBuildingService.buildDynamicLayer() INFO: SimpleStation search for search key [" + searchParams.unsignedHashCode() + "] returned no results.";
            LOG.debug(msg);
    		
            OGCProxyExceptionID id = OGCProxyExceptionID.SIMPLESTATION_FILE_ERROR;
            throw new OGCProxyException(id, "WQPUtils", "retrieveSearchParamData()", msg);
		}

		return new File(dataFilename);
	}
	
	private List<FeatureDAO> parseInput(String filename, SimpleFeatureType featureType) throws OGCProxyException {
		List<FeatureDAO> featureList = new ArrayList<>();
		try {
			TimeProfiler.startTimer("WQX_OB_XML Parse Execution Time");
			SimplePointParser spp = new SimplePointParser(filename, new SimpleFeatureBuilder(featureType));
			featureList.addAll(spp.parseSimplePointSource());
			TimeProfiler.endTimer("WQX_OB_XML Parse Execution Time", LOG);
		} catch (Exception e) {
			LOG.error("WQPLayerBuildingService.parseInput() Exception: " + e.getMessage());
			String msg = "WQPLayerBuildingService.buildDynamicLayer() EXCEPTION : Parsing the input failed.  Throwing Exception...";
			LOG.error(msg, e);
			OGCProxyExceptionID id = OGCProxyExceptionID.DATAFILE_PARSING_ERROR;
			throw new OGCProxyException(id, "WQPLayerBuildingService", "buildDynamicLayer()", msg);
		}
		return featureList;
	}
	
	private boolean createShapeFile(String path, String filename, boolean createIndex, List<FeatureDAO> featureList, SimpleFeatureType featureType) {
		File newFile = new File(path + "/" + filename + ".shp");

		TimeProfiler.startTimer("WQPLayerBuildingService.createShapeFile() INFO: GeoTools - ShapefileDataStoreFactory Creation Time");
		ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();
		TimeProfiler.endTimer("WQPLayerBuildingService.createShapeFile() INFO: GeoTools - ShapefileDataStoreFactory Creation Time", LOG);

		Map<String, Serializable> params = new HashMap<>();
		try {
			params.put("url", newFile.toURI().toURL());
		} catch (MalformedURLException e) {
			LOG.error(e.getMessage());
			return false;
		}
		params.put("create spatial index", createIndex);

		TimeProfiler.startTimer("WQPLayerBuildingService.createShapeFile() INFO: GeoTools - ShapefileDataStore Creation Time");
		ShapefileDataStore newDataStore;
		try {
			newDataStore = (ShapefileDataStore) dataStoreFactory.createNewDataStore(params);
		} catch (IOException e) {
			LOG.error(e.getMessage(), e);
			return false;
		}
		TimeProfiler.endTimer("WQPLayerBuildingService.createShapeFile() INFO: GeoTools - ShapefileDataStore Creation Time", LOG);

		/*
		 * TYPE is used as a template to describe the file contents
		 */
		try {
			newDataStore.createSchema(featureType);
		} catch (IOException e) {
			LOG.error(e.getMessage(), e);
			return false;
		}
		
		/*
		 * Get our features
		 */
		TimeProfiler.startTimer("WQPLayerBuildingService.createShapeFile() INFO: GeoTools - SimpleFeature List Creation Time");
		List<SimpleFeature> features = new ArrayList<>();
		for (FeatureDAO feature : featureList) {
			features.add(feature.getSimpleFeature());
		}
		TimeProfiler.endTimer("WQPLayerBuildingService.createShapeFile() INFO: GeoTools - SimpleFeature List Creation Time", LOG);
		
		/*
		 * Write the features to the shapefile
		 */
		TimeProfiler.startTimer("WQPLayerBuildingService.createShapeFile() INFO: OVERALL GeoTools ShapeFile Creation Time");
		if (!ShapeFileUtils.writeToShapeFile(newDataStore, featureType, features, path, filename)) {
			String error = "Unable to write shape file";
			LOG.error(error);
			return false;
		}
		TimeProfiler.endTimer("WQPLayerBuildingService.createShapeFile() INFO: OVERALL GeoTools ShapeFile Creation Time", LOG);
		
		return true;
	}
	
	private void uploadShapefile(String layerName) throws OGCProxyException {
		String geoServerURI = String.join("/", geoserverBaseURI, geoserverRest, geoserverWorkspaces, wqpWorkspace, geoserverDataStores);
		/*
		 * Upload the zipped shapefile
		 *
		 * Build the REST URI for uploading shapefiles.  Looks like:
		 * 		http://HOST:PORT/CONTEXT/rest/workspaces/WORKSPACE_NAME/datastores/LAYER_NAME/file.shp
		 *
		 * Where the "file.shp" is a GeoServer syntax that is required but does not change per request.
		 * We also duplicate the LAYER_NAME in the datastore path so we can let GeoServer separate the
		 * shapefiles easily between directories (it has an issue w/ tons of shapefiles in a single directory
		 * like what we did w/ the site_map datastore).
		 */
		String layerZipFile = shapefileDirectory + File.separator + layerName + ".zip";
		String restPut = geoServerURI + "/" + layerName + "/file.shp";
		LOG.debug("WQPLayerBuildingService.buildDynamicLayer() INFO: o ----- Uploading Shapefile (" + layerZipFile + ") to GeoServer");
		File file = new File(layerZipFile);
	    if (file.exists()) {
	    	try (CloseableHttpClient httpClient2 = HttpClients.custom().setDefaultCredentialsProvider(getCredentialsProvider()).build()) {
		    	verifyWorkspaceExists(httpClient2);
		    	putShapefile(httpClient2, restPut, ShapeFileUtils.MEDIATYPE_APPLICATION_ZIP, file);
	    	} catch (IOException e) {
	    		LOG.error(e.getLocalizedMessage(), e);
		    	OGCProxyExceptionID id = OGCProxyExceptionID.UPLOAD_SHAPEFILE_ERROR;
		    	throw new OGCProxyException(id, "WQPLayerBuildingService", "uploadShapefile()", "CloseableHttpClient Close Exception: " + e.getLocalizedMessage());
			} finally {
				if (!new File(layerZipFile).delete()) {
					LOG.debug("troubles deleting " + layerZipFile);
				}
			}
	    } else {
	    	OGCProxyExceptionID id = OGCProxyExceptionID.UPLOAD_SHAPEFILE_ERROR;
	    	throw new OGCProxyException(id, "WQPLayerBuildingService", "uploadShapefile()", "Exception: File [" + layerZipFile + "] DOES NOT EXIST");
	    }
		LOG.debug("WQPLayerBuildingService.buildDynamicLayer() INFO: o ----- Uploading Shapefile Complete");
		
		/*
		 * Let GeoServer catch up with its dataset ingestion.
		 * 		GeoServer has a race condition from when
		 * 		it finished uploading a shapefile to when
		 * 		the layer and datasource for that shapefile
		 * 		are available.  This is a wait time before
		 * 		we mark the layer AVAILABLE.
		 */
		try {
			Thread.sleep(geoserverCatchupTime);
		} catch (InterruptedException e) {
			LOG.warn("WQPLayerBuildingService.buildDynamicLayer() caught InterruptedException when running the GeoServer Catchup Time sleep.  Continuing...", e);
		}
		
		/*
		 * TODO:
		 *
		 * The final step is to force GeoServer to "enable" the layer.  Sometimes
		 * we have seen GeoServer correctly upload and accept a ShapeFile along
		 * with building the datastore and layer but forget to "Enable" the layer
		 * in its memory.
		 */
		
	}

	protected void verifyWorkspaceExists(CloseableHttpClient httpClient) throws OGCProxyException {
		int statusCode = -1;
		String workspaceURI = String.join("/", geoserverBaseURI, geoserverRest, geoserverWorkspaces, wqpWorkspace) + ".json";
		try {
			statusCode = httpClient.execute(new HttpGet(workspaceURI)).getStatusLine().getStatusCode();
		} catch (Exception e){
			LOG.error(e.getLocalizedMessage(), e);
			throw new OGCProxyException(OGCProxyExceptionID.UPLOAD_SHAPEFILE_ERROR, "WQPLayerBuildingService", "verifyWorkspaceExists()", 
					"Exception: " + e.getLocalizedMessage());
		}
		if (HttpStatus.SC_NOT_FOUND == statusCode) {
			createWorkspace(httpClient);
		}
	}

	protected void putShapefile(CloseableHttpClient httpClient, String uri, String mediaType, File file) throws OGCProxyException {
		int statusCode = -1;
		try {
	        HttpPut httpPut = new HttpPut(uri);
	        HttpEntity fileEntity = new FileEntity(file, ContentType.create(mediaType));
	        httpPut.setEntity(fileEntity);
	
	        statusCode = httpClient.execute(httpPut).getStatusLine().getStatusCode();
			LOG.debug("WQPLayerBuildingService.buildDynamicLayer() INFO: \nGeoServer response for request [" + uri + "] is: \n[" + statusCode + "]");
		} catch (Exception e) {
			LOG.error(e.getLocalizedMessage(), e);
			throw new OGCProxyException(OGCProxyExceptionID.UPLOAD_SHAPEFILE_ERROR, "WQPLayerBuildingService", "putShapefile()", 
					"Exception: " + e.getLocalizedMessage());
		}
		if (HttpStatus.SC_CREATED != statusCode) {
			throw new OGCProxyException(OGCProxyExceptionID.UPLOAD_SHAPEFILE_ERROR, "WQPLayerBuildingService", "putShapefile()", 
					"Exception: Invalid status code from geoserver:" + statusCode);
		}
	}

	protected void createWorkspace(CloseableHttpClient httpClient) throws OGCProxyException {
		/*
		 * We will actually try to create a namespace (which will automatically create the workspace)
		 */
		String uri = String.join("/", geoserverBaseURI, geoserverRest, geoserverNamespaces);
		String mediaType = "text/xml";
		String object = "<namespace><prefix>" + wqpWorkspace
				+ "</prefix><uri>http://www.waterqualitydata.us/ogcservices</uri></namespace>";
		int statusCode = -1;
		try {
			HttpPost httpPost = new HttpPost(uri);
			HttpEntity httpEntity = new StringEntity(object);
			httpPost.setEntity(httpEntity);
			httpPost.addHeader("content-type", mediaType);
			statusCode = httpClient.execute(httpPost).getStatusLine().getStatusCode();
		} catch (Exception e) {
			LOG.error(e.getLocalizedMessage(), e);
			throw new OGCProxyException(OGCProxyExceptionID.UPLOAD_SHAPEFILE_ERROR, "WQPLayerBuildingService", "createWorkspace()", 
					"Exception: " + e.getLocalizedMessage());
		}
		if (HttpStatus.SC_CREATED != statusCode) {
			throw new OGCProxyException(OGCProxyExceptionID.UPLOAD_SHAPEFILE_ERROR, "WQPLayerBuildingService", "createWorkspace()", 
					"Exception: Invalid status code from geoserver:" + statusCode);
		}

	}


	protected CredentialsProvider getCredentialsProvider() {
    	//TODO refactor to either WQPDynamicLayerCachingService or WQPLayerBuildingService
    	CredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(
                new AuthScope(geoserverHost, Integer.parseInt(geoserverPort)),
                new UsernamePasswordCredentials(geoserverUser, geoserverPass));
        return credsProvider;
    	//TODO end
	}

}
