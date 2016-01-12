package gov.usgs.wqp.ogcproxy.services.wqp;

import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import gov.usgs.wqp.ogcproxy.exceptions.OGCProxyException;
import gov.usgs.wqp.ogcproxy.exceptions.OGCProxyExceptionID;
import gov.usgs.wqp.ogcproxy.model.cache.DynamicLayerCache;
import gov.usgs.wqp.ogcproxy.model.ogc.services.OGCServices;
import gov.usgs.wqp.ogcproxy.model.parameters.SearchParameters;
import gov.usgs.wqp.ogcproxy.model.status.DynamicLayerStatus;
import gov.usgs.wqp.ogcproxy.utils.SystemUtils;

public class WQPDynamicLayerCachingService {
	private static Logger log = SystemUtils.getLogger(WQPDynamicLayerCachingService.class);
	
	/*
	 * Static Local		===========================================================
	 * ========================================================================
	 */
	/* ====================================================================== */
	@Autowired
	private Environment environment;
	private static boolean initialized;
	
	private static Map<String, DynamicLayerCache> requestToLayerCache;	// Map<LayerHash, LayerCache>
	
	private static long cacheTimeout = 604800000;			// 1000 * 60 * 60 * 24 * 7 (1 week)
	private static long threadSleep  = 500;
	/* ====================================================================== */
		
	/*
	 * INSTANCE		===========================================================
	 * ========================================================================
	 */
	/* ====================================================================== */
	private static final WQPDynamicLayerCachingService INSTANCE = new WQPDynamicLayerCachingService();
	/* ====================================================================== */
	
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
	 * @return WMSService instance
	 */
	public static WQPDynamicLayerCachingService getInstance() {
		return INSTANCE;
	}
	
	@PostConstruct
	public void initialize() {
		log.info("WQPDynamicLayerCachingService.initialize() called");
		
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
				cacheTimeout = Long.parseLong(environment.getProperty("wmscache.layercache.period"));
			} catch (Exception e) {
				log.error("WQPDynamicLayerCachingService() Constructor Exception: Failed to parse property [wmscache.layercache.period] " +
						  "- Keeping cache timeout period default to [" + cacheTimeout + "].\n" + e.getMessage() + "\n");
			}
			
			try {
				threadSleep = Long.parseLong(environment.getProperty("wmscache.layercache.sleep"));
			} catch (Exception e) {
				log.error("WQPDynamicLayerCachingService() Constructor Exception: Failed to parse property [wmscache.layercache.sleep] " +
						  "- Keeping thread sleep default to [" + threadSleep + "].\n" + e.getMessage() + "\n");
			}
			
		}
	}
	
	/**
	 * getLayerCache()
	 * @param key
	 * @param searchParams
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
	public DynamicLayerCache getLayerCache(SearchParameters<String, List<String>> searchParams, OGCServices originatingService) throws OGCProxyException {
		String key = searchParams.unsignedHashCode() + "";
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
					currentCache = new DynamicLayerCache(searchParams, originatingService);
					WQPDynamicLayerCachingService.requestToLayerCache.put(key, currentCache);
					
					String msg = "WQPDynamicLayerCachingService.getLayerCache() INFO : DynamicLayerCache object does not " +
							  "exist for key " + key +  ". Creating new Cache Object and setting status to [" +
							  DynamicLayerStatus.getStringFromType(currentCache.getCurrentStatus()) + "]";
					log.info(msg);
					
					return currentCache;
				}
			}
		}
		
		/*
		 * Lets check to see if this cache's status is an error.  If so we need
		 * to throw.
		 */
		if (currentCache.getCurrentStatus() == DynamicLayerStatus.ERROR) {
			String msg = "WQPDynamicLayerCachingService.getLayerCache() INFO : Caught Interrupted Exception waiting for " +
					  "Cache object [" + key + "] status to change.  Its current status is [" +
					  DynamicLayerStatus.getStringFromType(currentCache.getCurrentStatus()) +
					  "].  Throwing Exception...";
			log.error(msg);
			
			OGCProxyExceptionID id = OGCProxyExceptionID.WMS_LAYER_CREATION_FAILED;
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
		 * the status is available.  If its not we log an error.
		 */
		while ((currentCache.getCurrentStatus() == DynamicLayerStatus.BUILDING)
			|| (currentCache.getCurrentStatus() == DynamicLayerStatus.INITIATED)) {
			
			try {
				String msg = "WQPDynamicLayerCachingService.getLayerCache() INFO : DynamicLayerCache object exists for key [" +
							 key + "] but its status is [" +
							 DynamicLayerStatus.getStringFromType(currentCache.getCurrentStatus()) +
							 "].  Waiting " + WQPDynamicLayerCachingService.threadSleep + "ms";
				log.info(msg);
				
				Thread.sleep(WQPDynamicLayerCachingService.threadSleep);
			} catch (InterruptedException e) {
				if ((currentCache.getCurrentStatus() != DynamicLayerStatus.AVAILABLE) && (currentCache.getCurrentStatus() != DynamicLayerStatus.EMPTY)) {
					String msg = "WQPDynamicLayerCachingService.getLayerCache() INFO : Caught Interrupted Exception waiting for " +
							  "Cache object [" + key + "] status to change.  Its current status is [" +
							  DynamicLayerStatus.getStringFromType(currentCache.getCurrentStatus()) +
							  "].  Throwing Exception...";
					log.error(msg);
					
					OGCProxyExceptionID id = OGCProxyExceptionID.WMS_LAYER_CREATION_FAILED;
					throw new OGCProxyException(id, "WQPDynamicLayerCachingService", "getLayerCache()", msg);
				}
			}
		}
		
		String msg = "WQPDynamicLayerCachingService.getLayerCache() INFO : DynamicLayerCache object " +
				  "exists for key " + key +  ". Returning object with status [" +
				  DynamicLayerStatus.getStringFromType(currentCache.getCurrentStatus()) + "]";
		log.info(msg);
		
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
				String msg = "WQPDynamicLayerCachingService.getLayerCache() INFO : Removed Layer Cache for layer name [" +
							 currentCache.getLayerName() + "] for key [" + searchParamKey + "].  Invalidating " +
							 "cache for any current threads.";
				log.info(msg);
				currentCache.setCurrentStatus(DynamicLayerStatus.ERROR);
			}
		}
	}
	
	public Map<String, DynamicLayerCache> getCacheStore() {
		return WQPDynamicLayerCachingService.requestToLayerCache;
	}
	
	public int clearCache() {
		/*
		 * We need to clear the cache in a thread-safe way.  What we are going to
		 * do is actually utilize the thread-safe methods we have already and
		 * clear each item one at a time.
		 */
		List<String> cacheKeys = new Vector<String>(WQPDynamicLayerCachingService.requestToLayerCache.keySet());
		int originalCount = cacheKeys.size();
		
		List<String> uncleared = new Vector<String>();
		
		for (String cacheKey : cacheKeys) {
			DynamicLayerCache cache = WQPDynamicLayerCachingService.requestToLayerCache.get(cacheKey);
			DynamicLayerCache threadSafeCache = null;
			
			try {
				/*
				 * We'll go through our getLayerCache() method so that if there
				 * is a layer currently being built we will wait until it is
				 * finished before removing it.
				 */
				threadSafeCache = getLayerCache(cache.getSearchParameters(), OGCServices.UNKNOWN);
			} catch (OGCProxyException e) {
				log.error(e.traceBack());
			}
			
			if (threadSafeCache != null) {
				threadSafeCache.setCurrentStatus(DynamicLayerStatus.ERROR);
				removeLayerCache(threadSafeCache.getSearchParameters().unsignedHashCode() + "");
			} else {
				uncleared.add(cacheKey);
			}
		}
		
		int clearedCount = originalCount - uncleared.size();
		
		if (uncleared.size() != 0) {
			log.error("WQPDynamicLayerCachingService.clearCache() ERROR: Removed cache count [" + clearedCount +
					  "] does not equal total cache count [" + originalCount + "].  Potentially introducing " +
					  "stale layers {" + uncleared + "}");
		}
		
		log.info("WQPDynamicLayerCachingService.clearCache() INFO: Removed cache count [" + clearedCount +
				  "] from cache.");
		
		return clearedCount;
	}
	
}
