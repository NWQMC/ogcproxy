package gov.usgs.wqp.ogcproxy.services.wqp;

import static org.springframework.util.StringUtils.isEmpty;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.http.impl.client.CloseableHttpClient;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.Transaction;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.opengis.feature.simple.SimpleFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import gov.usgs.wqp.ogcproxy.exceptions.OGCProxyException;
import gov.usgs.wqp.ogcproxy.exceptions.OGCProxyExceptionID;
import gov.usgs.wqp.ogcproxy.model.cache.DynamicLayerCache;
import gov.usgs.wqp.ogcproxy.model.features.SimplePointFeature;
import gov.usgs.wqp.ogcproxy.model.features.SimplePointFeatureType;
import gov.usgs.wqp.ogcproxy.model.parameters.SearchParameters;
import gov.usgs.wqp.ogcproxy.utils.CloseableHttpClientFactory;
import gov.usgs.wqp.ogcproxy.utils.GeoServerUtils;
import gov.usgs.wqp.ogcproxy.utils.SystemUtils;
import gov.usgs.wqp.ogcproxy.utils.WQPUtils;

public class WQPLayerBuildingService {
	private static final Logger LOG = LoggerFactory.getLogger(WQPLayerBuildingService.class);

	@Autowired
	private CloseableHttpClientFactory closeableHttpClientFactory;
	@Autowired
	private GeoServerUtils geoServerUtils;
	@Autowired
	@Qualifier("layerbuilderBaseURI")
	private String layerbuilderBaseURI;
	@Autowired
	@Qualifier("workingDirectory")
	private String workingDirectory;
	@Autowired
	@Qualifier("shapefileDirectory")
	private String shapefileDirectory;

	private volatile boolean initialized;

	protected CloseableHttpClient geoserverClient;
	protected CloseableHttpClient wqpClient;

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

			geoserverClient = geoServerUtils.buildAuthorizedClient();
			wqpClient = closeableHttpClientFactory.getUnauthorizedCloseableHttpClient(false);

		}
	}

	@PreDestroy
	public void tearDown() {
		try {
			geoserverClient.close();
			wqpClient.close();
			LOG.info("Closed goeserverClient and wqpClient");
		} catch (IOException e) {
			LOG.error("Issue trying to close goeserverClient and wqpClient:" + e.getLocalizedMessage(), e);
		}
	}

	public String buildDynamicLayer(SearchParameters<String, List<String>> searchParams) throws OGCProxyException {
		long startTime = System.nanoTime();
		/*
		 * Get GeoJson from source
		 */
		String layerName = DynamicLayerCache.DYNAMIC_LAYER_PREFIX + searchParams.unsignedHashCode();
		File dataFile = getGeoJsonData(searchParams, layerName);

		/*
		 * Parse the GeoJsonData
		 */
		List<SimpleFeature> features;
		try {
			LOG.debug("WQPLayerBuildingService.buildDynamicLayer() INFO: o ----- Parsing input (" + dataFile.getAbsolutePath() + ")");
			features = processInput(dataFile.getAbsolutePath());
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
		if ( ! createShapeFile(shapefileDirectory, layerName, true, features) ) {
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
		geoServerUtils.uploadShapefile(geoserverClient, shapefileDirectory, layerName);

		LOG.info(layerName + " created in " + ((System.nanoTime() - startTime) / 1000000000d) + " seconds");

		return layerName;
	}

	private File getGeoJsonData(SearchParameters<String, List<String>> searchParams, String layerName) throws OGCProxyException {

		String dataFilename = WQPUtils.retrieveSearchParamData(wqpClient, searchParams, layerbuilderBaseURI, workingDirectory, layerName);

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

	private List<SimpleFeature> processInput(String filename) throws OGCProxyException {
		List<SimpleFeature> features = new ArrayList<>();
		try {
			JsonParser parser = new JsonParser();
			Object obj = parser.parse(new FileReader(filename));
			if (obj instanceof JsonObject && ((JsonObject) obj).has("features") && ((JsonObject) obj).get("features").isJsonArray()) {
				features.addAll(processFeatures(((JsonObject) obj).getAsJsonArray("features").iterator())); 
			}
		} catch (Exception e) {
			LOG.error("WQPLayerBuildingService.parseInput() Exception: " + e.getMessage());
			String msg = "WQPLayerBuildingService.buildDynamicLayer() EXCEPTION : Parsing the input failed.  Throwing Exception...";
			LOG.error(msg, e);
			OGCProxyExceptionID id = OGCProxyExceptionID.DATAFILE_PARSING_ERROR;
			throw new OGCProxyException(id, "WQPLayerBuildingService", "buildDynamicLayer()", msg);
		}
		return features;
	}

	private List<SimpleFeature> processFeatures(Iterator<JsonElement> i) {
		SimpleFeatureBuilder featurebuilder = new SimpleFeatureBuilder(SimplePointFeatureType.FEATURETYPE);
		List<SimpleFeature> features = new ArrayList<>();
		while (i.hasNext()) {
			JsonObject jsonFeature = i.next().getAsJsonObject();
			features.add(new SimplePointFeature(jsonFeature).getSimpleFeature(featurebuilder));
		}
		return features;
	}

	private boolean createShapeFile(String path, String filename, boolean createIndex, List<SimpleFeature> features) {
		File newFile = new File(path + "/" + filename + ".shp");
		ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();

		Map<String, Serializable> params = new HashMap<>();
		try {
			params.put("url", newFile.toURI().toURL());
		} catch (MalformedURLException e) {
			LOG.error(e.getMessage());
			return false;
		}
		params.put("create spatial index", createIndex);

		ShapefileDataStore newDataStore;
		try {
			newDataStore = (ShapefileDataStore) dataStoreFactory.createNewDataStore(params);
		} catch (IOException e) {
			LOG.error(e.getMessage(), e);
			return false;
		}

		/*
		 * TYPE is used as a template to describe the file contents
		 */
		try {
			newDataStore.createSchema(SimplePointFeatureType.FEATURETYPE);
		} catch (IOException e) {
			LOG.error(e.getMessage(), e);
			return false;
		}

		/*
		 * Write the features to the shapefile
		 */
		if (!writeToShapeFile(newDataStore, features, path, filename)) {
			String error = "Unable to write shape file";
			LOG.error(error);
			return false;
		}

		return true;
	}


	protected boolean writeToShapeFile(ShapefileDataStore newDataStore, List<SimpleFeature> features, String path, String filename) {
		/*
		 * Write the features to the shapefile
		 */
		try (Transaction transaction = new DefaultTransaction("create")) {
		
			String typeName = newDataStore.getTypeNames()[0];
			SimpleFeatureSource featureSource = newDataStore.getFeatureSource(typeName);

			if (featureSource instanceof SimpleFeatureStore) {
				SimpleFeatureStore featureStore = (SimpleFeatureStore) featureSource;
				/*
				 * SimpleFeatureStore has a method to add features from a
				 * SimpleFeatureCollection object, so we use the ListFeatureCollection
				 * class to wrap our list of features.
				 */
				SimpleFeatureCollection collection = new ListFeatureCollection(SimplePointFeatureType.FEATURETYPE, features);
				
				featureStore.setTransaction(transaction);
				try {
					featureStore.addFeatures(collection);
				} catch (IOException e) {
					transaction.rollback();
					throw e;
				}
				transaction.commit();

				/*
				 * Lets zip up all files created that make up "the shape file"
				 */
				SystemUtils.createZipFromFilematch(path, filename);
			} else {
				String msg = typeName + " does not support read/write access";
				LOG.error(msg);
				return false;
			}

		} catch (IOException e) {
			LOG.error(e.getMessage(), e);
			return false;
		}

		return true;
	}

}
