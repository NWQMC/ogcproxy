package gov.usgs.wqp.ogcproxy.services.wqp;

import static org.springframework.util.StringUtils.isEmpty;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.http.HttpStatus;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import gov.usgs.wqp.ogcproxy.exceptions.OGCProxyException;
import gov.usgs.wqp.ogcproxy.exceptions.OGCProxyExceptionID;
import gov.usgs.wqp.ogcproxy.geo.JsonObjectResponseHandler;
import gov.usgs.wqp.ogcproxy.model.OGCRequest;
import gov.usgs.wqp.ogcproxy.model.cache.DynamicLayerCache;
import gov.usgs.wqp.ogcproxy.model.status.DynamicLayerStatus;
import gov.usgs.wqp.ogcproxy.utils.GeoServerUtils;

public class WQPDynamicLayerCachingService {
	private static final Logger LOG = LoggerFactory.getLogger(WQPDynamicLayerCachingService.class);

	public static final String DATASTORES = "dataStores";
	public static final String DATASTORE = "dataStore";

	@Autowired
	private WQPLayerBuildingService wqpLayerBuildingService;
	@Autowired
	protected String geoserverWorkspace;
	@Autowired
	protected GeoServerUtils geoServerUtils;

	@Autowired
	protected Environment environment;
	protected volatile boolean initialized;

	private static Map<String, DynamicLayerCache> requestToLayerCache;

	private static long threadSleep = 500;

	protected CloseableHttpClient httpClient;

	private static final WQPDynamicLayerCachingService INSTANCE = new WQPDynamicLayerCachingService();

	/**
	 * Private Constructor for Singleton Pattern
	 */
	private WQPDynamicLayerCachingService() {
		requestToLayerCache = new ConcurrentHashMap<String, DynamicLayerCache>();
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

			try {
				threadSleep = Long.parseLong(environment.getProperty("proxy.thread.sleep"));
			} catch (Exception e) {
				LOG.error("Failed to parse property [proxy.thread.sleep] " +
						  "- Keeping thread sleep default to [" + threadSleep + "].\n" + e.getMessage() + "\n", e);
			}

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
		DynamicLayerCache layerCache = null;

		try {
			DynamicLayerCache defaultLayerCache = new DynamicLayerCache(ogcRequest, geoserverWorkspace);
			layerCache = getLayerCache(defaultLayerCache);

			/*
			 * We should be blocked above with the getLayerCache() call and should only
			 * get a value for layerCache when its finished performing an action.  The
			 * valid non-action status's are AVAILABLE (default), INITIAL, EMPTY and ERROR
			 */
			switch (layerCache.getCurrentStatus()) {
				case INITIATED:
					LOG.trace("Created new DynamicLayerCache for key [" +
							ogcRequest.getSearchParams().unsignedHashCode() +"].  Setting status to BUILDING and creating layer...");

					/*
					 * We just created a new cache object.  This means there is no
					 * layer currently for this request in our GeoServer.  We now
					 * need to make this layer through the WMSLayerService.
					 */
					layerCache.setCurrentStatus(DynamicLayerStatus.BUILDING);

					/*
					 * We now call the simplestation url with our search params
					 * (along with a mimeType=xml) in order to retrieve the data
					 * that creates the layer:
					 *
					 * 		http://www.waterqualitydata.us/simplestation/search?countycode=US%3A40%3A109&characteristicName=Atrazine&mimeType=xml
					 *
					 * Documentation is from http://waterqualitydata.us/webservices_documentation.jsp
					 * except we call "simplestation" instead of "Station"
					 */
					//TODO - rather than giving string, maybe just throw exception if problems in buildDynamicLayer
					if (isEmpty(wqpLayerBuildingService.buildDynamicLayer(ogcRequest.getSearchParams()))) {
						layerCache.setCurrentStatus(DynamicLayerStatus.EMPTY);

						LOG.error("Unable to create layer [" + layerCache.getLayerName() +
								"] for key ["+ ogcRequest.getSearchParams().unsignedHashCode() +
								"].  Its status is [" + layerCache.getCurrentStatus().toString() +
								"].  Since it is an empty request this means the search parameters did not " +
								"result in any matching criteria.");
					} else {

						//TODO Also check to see if the layer is enabled in GeoServer...

						layerCache.setCurrentStatus(DynamicLayerStatus.AVAILABLE);

						LOG.trace("Finished building layer for key ["+
								ogcRequest.getSearchParams().unsignedHashCode() +
								"].  Layer name is [" + layerCache.getLayerName() + "].  Setting status to " +
								"AVAILABLE and continuing on to GeoServer WMS request...");
					}
					break;

				case EMPTY:
					LOG.error("Retrieved layer name [" + layerCache.getLayerName() +
							"] for key ["+ ogcRequest.getSearchParams().unsignedHashCode() +
							"] and its status is [" + layerCache.getCurrentStatus().toString() +
							"].  Since it is an empty request this means the search parameters did not " +
							"result in any matching criteria.");
					break;

				case ERROR:
					LOG.error("Retrieved layer name [" + layerCache.getLayerName() +
							"] for key ["+ ogcRequest.getSearchParams().unsignedHashCode() +
							"] and its status is [" + layerCache.getCurrentStatus().toString() +
							"].  Cannot continue request.");
					break;

				default:
					LOG.trace("Retrieved layer name [" + layerCache.getLayerName() +
							"] for key ["+ ogcRequest.getSearchParams().unsignedHashCode() +
							"] and its status is [" + layerCache.getCurrentStatus().toString() +
							"].  Continuing on to GeoServer WMS request...");
					break;
			}
		} catch (Exception e) {
			if (layerCache != null) {
				layerCache.setCurrentStatus(DynamicLayerStatus.ERROR);
				removeLayerCache(layerCache.getKey());
			}

			//TODO Bubble this error up to the client!!
			LOG.error("Layer was not created for search parameters.", e);
		}

		return null == layerCache ? "" : layerCache.getQualifiedLayerName();
	}

	/**
	 * getLayerCache()
	 * @param defaultLayerCache
	 * @return DynamicLayerCache
	 * <br /><br />
	 * This method will return an existing DynamicLayerCache object if it exists
	 * in its internal data store.  If it does NOT contain an existing Cache
	 * object, it will then create one and return the newly created object.  This
	 * newly created Cache Object's internal state will equal:<br />
	 * 			DynamicLayerStatus.INITIATED
	 * <br /><br />
	 * To keep the DynamicLayerCache object's state from becoming corrupted by
	 * multiple threads requesting the object, this method will BLOCK on
	 * returning a DynamicLayerCache object if its internal state value does NOT
	 * equal:<br />
	 * 			DynamicLayerStatus.AVAILABLE
	 * <br /><br />
	 * @throws OGCProxyException
	 */
	public DynamicLayerCache getLayerCache(DynamicLayerCache defaultLayerCache) throws OGCProxyException {
		String key = defaultLayerCache.getKey();
		DynamicLayerCache currentCache = WQPDynamicLayerCachingService.requestToLayerCache.get(key);

		if (currentCache == null) {
			/*
			 * This next block is synchronized so that only one thread can enter
			 * it at a time.  We will then recheck to see if the Cache object is
			 * in the data store.  If it is, we continue on.  If it is NOT we
			 * create one, add it to the data store and return.
			 */
			synchronized (WQPDynamicLayerCachingService.class) {
				currentCache = WQPDynamicLayerCachingService.requestToLayerCache.get(key);

				if (currentCache == null) {
					currentCache = defaultLayerCache;
					WQPDynamicLayerCachingService.requestToLayerCache.put(key, currentCache);

					String msg = "DynamicLayerCache object does not exist for key " + key
							+  ". Creating new Cache Object and setting status to [" +
							currentCache.getCurrentStatus().toString() + "]";
					LOG.trace(msg);

					return currentCache;
				}
			}
		}

		return waitForFinalStatus(currentCache);
	}

	protected DynamicLayerCache waitForFinalStatus(DynamicLayerCache currentCache) throws OGCProxyException {
		/*
		 * Lets check to see if this cache's status is an error.  If so we need
		 * to throw.
		 */
		if (currentCache.getCurrentStatus() == DynamicLayerStatus.ERROR) {
			String msg = "Caught Interrupted Exception waiting for Cache object [" + currentCache.getKey()
				+ "] status to change.  Its current status is [" +
				currentCache.getCurrentStatus().toString() + "].  Throwing Exception...";
			LOG.error(msg);

			OGCProxyExceptionID id = OGCProxyExceptionID.LAYER_CREATION_FAILED;
			throw new OGCProxyException(id, "WQPDynamicLayerCachingService", "getLayerCache()", msg);
		}

		/*
		 * There is a legit Cache object in the internal data store.  Now we need to
		 * check its status to see if its being worked on or if its fully available
		 * to being used.  If it's NOT available, we will sleep until it does
		 * become available.
		 *
		 * The internal DynamicLayerStatus object will have its state managed by
		 * the initial thread that created it.  This thread will set the state to
		 * DynamicLayerStatus.AVAILABLE as soon as it becomes available.
		 *
		 * In the catching of the Interrupted Exception, we will double check that
		 * the status is available.  If its not we LOG an error.
		 */
		while ((currentCache.getCurrentStatus() == DynamicLayerStatus.BUILDING)
			|| (currentCache.getCurrentStatus() == DynamicLayerStatus.INITIATED)) {

			try {
				String msg = "DynamicLayerCache object exists for key [" +
						currentCache.getKey() + "] but its status is [" +
							 currentCache.getCurrentStatus().toString() +
							 "].  Waiting " + WQPDynamicLayerCachingService.threadSleep + "ms";
				LOG.trace(msg);

				Thread.sleep(WQPDynamicLayerCachingService.threadSleep);
			} catch (InterruptedException e) {
				if ((currentCache.getCurrentStatus() != DynamicLayerStatus.AVAILABLE) && (currentCache.getCurrentStatus() != DynamicLayerStatus.EMPTY)) {
					String msg = "Caught Interrupted Exception waiting for " +
							  "Cache object [" + currentCache.getKey() + "] status to change.  Its current status is [" +
							  currentCache.getCurrentStatus().toString() +
							  "].  Throwing Exception...";
					LOG.error(msg);

					OGCProxyExceptionID id = OGCProxyExceptionID.LAYER_CREATION_FAILED;
					throw new OGCProxyException(id, "WQPDynamicLayerCachingService", "getLayerCache()", msg);
				}
			}
		}

		String msg = "DynamicLayerCache object exists for key " + currentCache.getKey() +  ". Returning object with status [" +
				currentCache.getCurrentStatus().toString() + "]";
		LOG.trace(msg);

		return currentCache;
	}

	public void removeLayerCache(String searchParamKey) {
		synchronized (requestToLayerCache) {
			DynamicLayerCache currentCache = WQPDynamicLayerCachingService.requestToLayerCache.remove(searchParamKey);

			/*
			 * Set the status to ERROR so that anyone currently waiting to use the cache will
			 * see it as invalid
			 */
			if (currentCache != null) {
				String msg = "Removed Layer Cache for layer name [" +
							 currentCache.getLayerName() + "] for key [" + searchParamKey + "].  Invalidating " +
							 "cache for any current threads.";
				LOG.trace(msg);
				currentCache.setCurrentStatus(DynamicLayerStatus.ERROR);
			}
		}
	}

	public Map<String, DynamicLayerCache> getCacheStore() {
		return WQPDynamicLayerCachingService.requestToLayerCache;
	}

	public int clearCache() {
		/* 
		 * First, drop the workspace in geoserver to clear it.
		 */
		clearGeoserverWorkspace();
		/*
		 * Then clear in-memory cache.
		 */
		return clearInMemoryCache();
	}

	protected void clearGeoserverWorkspace() {
		String deleteUri = geoServerUtils.buildWorkspaceRestDelete();
		try {
			HttpClientContext localContext = geoServerUtils.buildLocalContext();
			HttpDelete httpDelete = new HttpDelete(deleteUri);
			httpClient.execute(httpDelete, localContext);
		} catch (Exception e) {
			//TODO - OK to just eat?
			LOG.error("Problems resetting workspace in geoserver: " + e.getLocalizedMessage(), e);
		}
	}

	protected int clearInMemoryCache() {
		/*
		 * We need to clear the cache in a thread-safe way.  What we are going to
		 * do is actually utilize the thread-safe methods we have already and
		 * clear each item one at a time.
		 */
		List<String> cacheKeys = new Vector<>(WQPDynamicLayerCachingService.requestToLayerCache.keySet());
		int originalCount = cacheKeys.size();

		List<String> uncleared = new Vector<>();

		for (String cacheKey : cacheKeys) {
			DynamicLayerCache cache = WQPDynamicLayerCachingService.requestToLayerCache.get(cacheKey);
			DynamicLayerCache threadSafeCache = null;

			try {
				/*
				 * We'll go through our getLayerCache() method so that if there
				 * is a layer currently being built we will wait until it is
				 * finished before removing it.
				 */
				threadSafeCache = getLayerCache(cache);
			} catch (OGCProxyException e) {
				LOG.error("Issue getting cache key: " + cacheKey, e);
			}

			if (threadSafeCache != null) {
				threadSafeCache.setCurrentStatus(DynamicLayerStatus.ERROR);
				removeLayerCache(threadSafeCache.getKey());
			} else {
				uncleared.add(cacheKey);
			}
		}

		int clearedCount = originalCount - uncleared.size();

		if (!uncleared.isEmpty()) {
			LOG.error("Removed cache count [" + clearedCount +
					  "] does not equal total cache count [" + originalCount + "].  Potentially introducing " +
					  "stale layers {" + uncleared + "}");
		}

		LOG.trace("Removed cache count [" + clearedCount + "] from cache.");

		return clearedCount;
	}

	protected void populateCache() {
		/*
		 * Run out to Geoserver for the list of layers it has and add them to our cache.
		 * We should be finished before this service responds to any requests, but we will use the thread safe getLayerCache()
		 * just in case...
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
				DynamicLayerCache cacheIt = new DynamicLayerCache(layer.get("name").getAsString(), geoserverWorkspace);
				try {
					getLayerCache(cacheIt);
				} catch (OGCProxyException e) {
					LOG.error("Problems populating cache", e);
				}
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
