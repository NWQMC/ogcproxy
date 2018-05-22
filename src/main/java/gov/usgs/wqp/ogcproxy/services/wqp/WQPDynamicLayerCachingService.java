package gov.usgs.wqp.ogcproxy.services.wqp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.http.HttpStatus;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import gov.usgs.wqp.ogcproxy.exceptions.OGCProxyException;
import gov.usgs.wqp.ogcproxy.geo.JsonObjectResponseHandler;
import gov.usgs.wqp.ogcproxy.model.DynamicLayer;
import gov.usgs.wqp.ogcproxy.model.OGCRequest;
import gov.usgs.wqp.ogcproxy.model.cache.GenericCache;
import gov.usgs.wqp.ogcproxy.services.ConfigurationService;
import gov.usgs.wqp.ogcproxy.utils.GeoServerUtils;

public class WQPDynamicLayerCachingService {
	private static final Logger LOG = LoggerFactory.getLogger(WQPDynamicLayerCachingService.class);

	public static final String DATASTORES = "dataStores";
	public static final String DATASTORE = "dataStore";

	@Autowired
	protected WQPLayerBuildingService wqpLayerBuildingService;
	@Autowired
	protected GeoServerUtils geoServerUtils;
	@Autowired
	protected ConfigurationService configurationService;

	protected volatile boolean initialized;

	private final GenericCache<String, DynamicLayer> requestToLayerCache = new GenericCache<>();

	protected long threadSleep = 500;

	protected CloseableHttpClient httpClient;

	private static final WQPDynamicLayerCachingService INSTANCE = new WQPDynamicLayerCachingService();

	/**
	 * Private Constructor for Singleton Pattern
	 */
	private WQPDynamicLayerCachingService() {
		initialized = false;
	}

	/**
	 * Singleton accessor
	 *
	 * @return WQPDynamicLayerCachingService instance
	 */
	public static WQPDynamicLayerCachingService getInstance() {
		return INSTANCE;
	}

	@PostConstruct
	public void initialize() {
		LOG.trace("WQPDynamicLayerCachingService.initialize() called: " + initialized);

		/*
		 * Since we are using Spring DI we cannot access the environment bean
		 * in the constructor.  We'll just use a locked initialized variable
		 * to check initialization after instantiation and set the env
		 * properties here.
		 */
		if (initialized) {
			return;
		}
		synchronized(WQPDynamicLayerCachingService.class) {
			if (initialized) {
				return;
			}
			initialized = true;

			threadSleep = configurationService.getThreadSleep();

			httpClient = geoServerUtils.buildAuthorizedClient();

			populateCache();
		}
	}

	@PreDestroy
	public void tearDown() {
		try {
			httpClient.close();
			LOG.info("Closed httpClient");
		} catch (IOException e) {
			LOG.error("Issue trying to close httpClient:" + e.getLocalizedMessage(), e);
		}
	}

	/** 
	 * Entry point for our vendor-specific searchParams layer building. If all goes well, there will be a layer created based 
	 * on the searchParams and uploaded to geoserver for use in completing this call and others with the same base parameters. 
	 * @param ogcRequest The parsed request and background information from the requesting service.
	 * @return the layer name if successful, otherwise and empty string.
	 */
	public String getDynamicLayer(OGCRequest ogcRequest) {
		String layerName = "";
		try {
			layerName = getLayer(new DynamicLayer(ogcRequest, configurationService.getGeoserverWorkspace())).getQualifiedLayerName();
		} catch (Exception e) {
			//TODO Bubble this error up to the client!!
			LOG.error("Layer was not created for search parameters.", e);
		}
		return layerName;
	}

	public DynamicLayer getLayer(DynamicLayer dynamicLayer) throws InterruptedException, ExecutionException {
		return requestToLayerCache.getValue(dynamicLayer.getLayerName(), new Callable<DynamicLayer>() {
			@Override
			public DynamicLayer call() throws Exception {
				return wqpLayerBuildingService.buildDynamicLayer(dynamicLayer);
			}
		});
	}

	public Collection<DynamicLayer> getCacheValues() throws InterruptedException, ExecutionException {
		List<DynamicLayer> rtn = new ArrayList<>();
		Iterator<Future<DynamicLayer>> i = requestToLayerCache.getValues().iterator();
		while (i.hasNext()) {
			rtn.add((DynamicLayer) i.next().get());
		}
		return rtn;
	}

	public int clearCache() {
		/* 
		 * First, delete the files on the geoserver server in case the workspace drop would fail.
		 */
		deleteGeoserverResources();
		/* 
		 * Then drop the workspace in geoserver to clear it.
		 */
		clearGeoserverWorkspace();
		/*
		 * Finally, clear in-memory cache.
		 */
		return clearInMemoryCache();
	}

	protected void deleteGeoserverResources() {
		String deleteUri = geoServerUtils.buildResourceRestDelete();
		CloseableHttpResponse response = null;
		try {
			HttpClientContext localContext = geoServerUtils.buildLocalContext();
			HttpDelete httpDelete = new HttpDelete(deleteUri);
			response = httpClient.execute(httpDelete, localContext);
		} catch (Exception e) {
			LOG.error("Problems deleting resources in geoserver: " + e.getLocalizedMessage(), e);
		} finally {
			if (null != response) {
				try {
					response.close();
				} catch (IOException e) {
					LOG.error("Problems closing response for deleting resources in geoserver: " + e.getLocalizedMessage(), e);
				}
			}
		}
	}

	protected void clearGeoserverWorkspace() {
		String deleteUri = geoServerUtils.buildWorkspaceRestDelete();
		CloseableHttpResponse response = null;
		try {
			HttpClientContext localContext = geoServerUtils.buildLocalContext();
			HttpDelete httpDelete = new HttpDelete(deleteUri);
			response = httpClient.execute(httpDelete, localContext);
		} catch (Exception e) {
			LOG.error("Problems resetting workspace in geoserver: " + e.getLocalizedMessage(), e);
		} finally {
			if (null != response) {
				try {
					response.close();
				} catch (IOException e) {
					LOG.error("Problems closing response for resetting workspace in geoserver: " + e.getLocalizedMessage(), e);
				}
			}
		}
	}

	protected int clearInMemoryCache() {
		int clearedCount = requestToLayerCache.size();
		requestToLayerCache.clear();
		return clearedCount;
	}

	protected void populateCache() {
		/*
		 * Run out to Geoserver for the list of layers it has and add them to our cache.
		 */
		LOG.trace("START");
		HttpClientContext localContext = geoServerUtils.buildLocalContext();
		try {
			JsonObject jsonObject = httpClient.execute(new HttpGet(geoServerUtils.buildDataStoreRestGet()),
					new JsonObjectResponseHandler(), localContext);
			Iterator<JsonElement> i = getResponseIterator(jsonObject);
			while (i.hasNext()) {
				JsonObject layer = i.next().getAsJsonObject();
				LOG.trace("With [" + layer.get("name").getAsString() + "]");
				DynamicLayer dynamiclayer = new DynamicLayer(layer.get("name").getAsString(), configurationService.getGeoserverWorkspace());
				requestToLayerCache.setValueIfAbsent(dynamiclayer.getLayerName(), dynamiclayer);
			}
		} catch (Exception e) {
			if (e instanceof HttpResponseException && ((HttpResponseException)e).getStatusCode() == HttpStatus.SC_NOT_FOUND) {
				try {
					geoServerUtils.createWorkspace(httpClient, localContext);
				} catch (OGCProxyException e1) {
					LOG.error("Problems creating workspace while loading cache: " + e.getLocalizedMessage(), e);
				}
			} else {
				LOG.error("Problems loading cache from geoserver: " + e.getLocalizedMessage(), e);
			}
		}
		LOG.trace("FINISH");
	}

	protected Iterator<JsonElement> getResponseIterator(JsonObject jsonObject) {
		Iterator<JsonElement> rtn = new JsonArray().iterator();
		//We are expecting {"dataStores": {"dataStore: [{....}, {....}, ...]}}
		if (null != jsonObject 
				&& jsonObject.has(DATASTORES) && jsonObject.get(DATASTORES).isJsonObject()
				&& jsonObject.getAsJsonObject(DATASTORES).has(DATASTORE)
				&& jsonObject.getAsJsonObject(DATASTORES).get(DATASTORE).isJsonArray()) {
			rtn = jsonObject.getAsJsonObject(DATASTORES).getAsJsonArray(DATASTORE).iterator();
		}
		return rtn;
	}

}
