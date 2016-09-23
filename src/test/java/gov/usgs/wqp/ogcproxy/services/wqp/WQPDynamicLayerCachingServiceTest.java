package gov.usgs.wqp.ogcproxy.services.wqp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.powermock.reflect.Whitebox;
import org.springframework.core.env.Environment;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import gov.usgs.wqp.ogcproxy.exceptions.OGCProxyException;
import gov.usgs.wqp.ogcproxy.geo.JsonObjectResponseHandler;
import gov.usgs.wqp.ogcproxy.geo.JsonObjectResponseHandlerTest;
import gov.usgs.wqp.ogcproxy.model.OGCRequest;
import gov.usgs.wqp.ogcproxy.model.cache.DynamicLayerCache;
import gov.usgs.wqp.ogcproxy.model.ogc.services.OGCServices;
import gov.usgs.wqp.ogcproxy.model.parameters.SearchParameters;
import gov.usgs.wqp.ogcproxy.model.status.DynamicLayerStatus;
import gov.usgs.wqp.ogcproxy.utils.GeoServerUtils;

@DirtiesContext(classMode=ClassMode.AFTER_CLASS)
public class WQPDynamicLayerCachingServiceTest {

	@Mock
	private CloseableHttpClient httpClient;
	@Mock
	private HttpClientBuilder httpClientBuilder;
	@Mock
	private Environment environment;
	@Mock
	private GeoServerUtils geoServerUtils;
	@Mock
	private CloseableHttpResponse response;
	@Mock
	private StatusLine statusLine;
	@Spy
	WQPDynamicLayerCachingService service = WQPDynamicLayerCachingService.getInstance();

	public final static String WQP_WORKSPACE = "wqp_sites";
	public final static String ONE_QUAL_NAME = String.join(":", WQP_WORKSPACE, JsonObjectResponseHandlerTest.ONE_NAME);
	public final static String TWO_QUAL_NAME = String.join(":", WQP_WORKSPACE, JsonObjectResponseHandlerTest.TWO_NAME);
	public final static String THREE_QUAL_NAME = String.join(":", WQP_WORKSPACE, JsonObjectResponseHandlerTest.THREE_NAME);

	@Before
	public void beforeTest() {
		MockitoAnnotations.initMocks(this);
		when(httpClientBuilder.build()).thenReturn(httpClient);
		when(geoServerUtils.buildLocalContext()).thenReturn(new HttpClientContext());
		when(geoServerUtils.buildAuthorizedClient()).thenReturn(httpClient);
		when(geoServerUtils.buildDataStoreRestGet()).thenReturn("http://owi.usgs.gov/geoserver");
		when(geoServerUtils.buildWorkspaceRestDelete()).thenReturn("http://owi.usgs.gov/geoserver");

		service.geoServerUtils = geoServerUtils;

		service.httpClient = httpClient;
		service.geoserverWorkspace = WQP_WORKSPACE;
	}

	@Test
	public void clearCacheTest() {
		int cnt = service.clearCache();
		verify(service).clearGeoserverWorkspace();
		verify(service).clearInMemoryCache();

		//Do it a second time to verify it really cleared the cache.
		cnt = service.clearCache();
		verify(service, times(2)).clearGeoserverWorkspace();
		verify(service, times(2)).clearInMemoryCache();

		assertEquals(0, cnt);
	}

	@Test
	public void clearGeoserverWorkspaceTest() {
		try {
			when(httpClient.execute(any(HttpDelete.class), any(HttpClientContext.class))).thenThrow(new IOException("Hi")).thenReturn(response);

			service.clearGeoserverWorkspace();
			verify(httpClient, times(1)).execute(any(HttpDelete.class), any(HttpClientContext.class));

			service.clearGeoserverWorkspace();
			verify(httpClient, times(2)).execute(any(HttpDelete.class), any(HttpClientContext.class));
		} catch (Exception e) {
			e.printStackTrace();
			fail("shouldn't get here at this time");
		}
	}

	@Test
	public void clearInMemoryCachetest() {
		Map<String, DynamicLayerCache> cache = new ConcurrentHashMap<>();
		cache.put("123456", new DynamicLayerCache("dynamicSites_123456", "qw_portal_map"));
		SearchParameters<String, List<String>> searchParams = new SearchParameters<>();
		searchParams.put("key", Arrays.asList("a", "b"));
		OGCRequest ogcRequest = new OGCRequest(OGCServices.WFS, null, searchParams, null);
		DynamicLayerCache c2 = new DynamicLayerCache(ogcRequest, "qw_portal_map");
		c2.setCurrentStatus(DynamicLayerStatus.AVAILABLE);
		cache.put(c2.getKey(), c2);
		Whitebox.setInternalState(WQPDynamicLayerCachingService.class, "requestToLayerCache", cache);

		int cnt = service.clearInMemoryCache();
		assertEquals(2, cnt);
		Map<String, DynamicLayerCache> afterCache = Whitebox.getInternalState(WQPDynamicLayerCachingService.class, "requestToLayerCache");
		assertTrue(afterCache.isEmpty());
	}

	@Test
	public void getDynamicLayerTest() {
		//TODO
	}

	@Test
	public void getLayerCacheTest() {
		DynamicLayerCache existingCache = new DynamicLayerCache("dynamicSites_123456", "qw_portal_map");
		Map<String, DynamicLayerCache> cache = new ConcurrentHashMap<>();
		cache.put("123456", existingCache);
		Whitebox.setInternalState(WQPDynamicLayerCachingService.class, "requestToLayerCache", cache);
		DynamicLayerCache defaultCache = new DynamicLayerCache("12345678", "testWorkspace");
		try {
			assertEquals(defaultCache, service.getLayerCache(defaultCache));
		} catch (Exception e) {
			fail("Didn't expect to get exception: " + e.getLocalizedMessage());
		}

		defaultCache = new DynamicLayerCache("123456", "qw_portal_map");
		try {
			assertEquals(existingCache, service.getLayerCache(defaultCache));
			verify(service).waitForFinalStatus(any(DynamicLayerCache.class));
		} catch (Exception e) {
			fail("Didn't expect to get exception: " + e.getLocalizedMessage());
		}
	}

	@Test
	public void getResponseIteratorTest() {
		Iterator<JsonElement> i = service.getResponseIterator(buildCompleteTestDataSourceResponse());
		assertNotNull(i);
		assertTrue(i.hasNext());

		i = service.getResponseIterator(buildEmptyDataStoresTestDataStoreResponse());
		assertNotNull(i);
		assertFalse(i.hasNext());

		i = service.getResponseIterator(buildDataStoreNotArrayTestDataStoreResponse());
		assertNotNull(i);
		assertFalse(i.hasNext());

		i = service.getResponseIterator(buildDataStoresNotObjectTestDataStoreResponse());
		assertNotNull(i);
		assertFalse(i.hasNext());

		i = service.getResponseIterator(null);
		assertNotNull(i);
		assertFalse(i.hasNext());

		i = service.getResponseIterator(buildNoDataStoreTestDataStoreResponse());
		assertNotNull(i);
		assertFalse(i.hasNext());
	}
	
	@Test
	public void initializeTest() {
		when(environment.getProperty("proxy.thread.sleep")).thenReturn("", "456");

		service.environment = environment;
		service.initialize();

		//defaults since all are empty string
		assertEnvironmentDefaults();
		verify(environment, times(1)).getProperty("proxy.thread.sleep");

		//still defaults because already initialized...
		service.initialize();
		assertEnvironmentDefaults();
		verify(environment, times(1)).getProperty("proxy.thread.sleep");

		//start over and apply some.
		service.initialized = false;
		service.initialize();
		assertEquals(Long.valueOf("456"), Whitebox.getInternalState(WQPDynamicLayerCachingService.class, "threadSleep"));
		verify(environment, times(2)).getProperty("proxy.thread.sleep");
	}

	@Test
	public void populateCacheTest() throws UnsupportedOperationException, IOException, OGCProxyException {
		when(httpClient.execute(any(HttpGet.class), any(JsonObjectResponseHandler.class), any(HttpClientContext.class)))
				.thenThrow(new HttpResponseException(HttpStatus.SC_NOT_FOUND, "fake not found"))
				.thenReturn(null, new JsonObject(), buildEmptyDataStoresTestDataStoreResponse(), buildCompleteTestDataSourceResponse());

		//Workspace not found
		service.populateCache();
		assertTrue(service.getCacheStore().isEmpty());
		verify(httpClient).execute(any(HttpGet.class), any(JsonObjectResponseHandler.class), any(HttpClientContext.class));
		verify(geoServerUtils).createWorkspace(any(CloseableHttpClient.class), any(HttpClientContext.class));

		//Null response
		service.populateCache();
		assertTrue(service.getCacheStore().isEmpty());
		verify(httpClient, times(2)).execute(any(HttpGet.class), any(JsonObjectResponseHandler.class), any(HttpClientContext.class));

		//No Layers
		service.populateCache();
		assertTrue(service.getCacheStore().isEmpty());

		//Empty Layer List
		service.populateCache();
		assertTrue(service.getCacheStore().isEmpty());

		//Some Layers
		service.populateCache();
		Map<String, DynamicLayerCache> cache = service.getCacheStore();
		assertEquals(3, cache.size());

		assertTrue(cache.containsKey(JsonObjectResponseHandlerTest.ONE_KEY));
		DynamicLayerCache one = cache.get(JsonObjectResponseHandlerTest.ONE_KEY);
		assertEquals(JsonObjectResponseHandlerTest.ONE_NAME, one.getLayerName());
		assertEquals(ONE_QUAL_NAME, one.getQualifiedLayerName());
		assertEquals(DynamicLayerStatus.AVAILABLE, one.getCurrentStatus());

		assertTrue(cache.containsKey(JsonObjectResponseHandlerTest.TWO_KEY));
		DynamicLayerCache two = cache.get(JsonObjectResponseHandlerTest.TWO_KEY);
		assertEquals(JsonObjectResponseHandlerTest.TWO_NAME, two.getLayerName());
		assertEquals(TWO_QUAL_NAME, two.getQualifiedLayerName());
		assertEquals(DynamicLayerStatus.AVAILABLE, two.getCurrentStatus());

		assertTrue(cache.containsKey(JsonObjectResponseHandlerTest.THREE_KEY));
		DynamicLayerCache three = cache.get(JsonObjectResponseHandlerTest.THREE_KEY);
		assertEquals(JsonObjectResponseHandlerTest.THREE_NAME, three.getLayerName());
		assertEquals(THREE_QUAL_NAME, three.getQualifiedLayerName());
		assertEquals(DynamicLayerStatus.AVAILABLE, three.getCurrentStatus());
	}

	@Test
	public void waitForFinalStatusTest() {
		DynamicLayerCache currentCache = new DynamicLayerCache("12345678", "testWorkspace");
		currentCache.setCurrentStatus(DynamicLayerStatus.ERROR);
		try {
			service.waitForFinalStatus(currentCache);
			fail("Didn't get expected OGCProxyException");
		} catch (Exception e) {
			if (e instanceof OGCProxyException && e.getLocalizedMessage().contains("Its current status is [ERROR].")) {
				//Cool!
			} else {
				fail("Wrong exception thrown!");
			}
		}

		currentCache.setCurrentStatus(DynamicLayerStatus.AVAILABLE);
		try {
			assertEquals(currentCache, service.waitForFinalStatus(currentCache));
		} catch (Exception e) {
			fail("Didn't expect to get exception: " + e.getLocalizedMessage());
		}

		//TODO waiting tests
	}

	private void assertEnvironmentDefaults() {
		assertEquals(Long.valueOf("500"), Whitebox.getInternalState(WQPDynamicLayerCachingService.class, "threadSleep"));
	}

	public static JsonObject buildCompleteTestDataSourceResponse() {
		JsonObject rtn = new JsonObject();
		JsonObject layers = new JsonObject();
		rtn.add(WQPDynamicLayerCachingService.DATASTORES, layers);
		JsonArray layerList = new JsonArray();
		layers.add(WQPDynamicLayerCachingService.DATASTORE, layerList);
		JsonObject one = new JsonObject();
		one.add("name", new JsonPrimitive(JsonObjectResponseHandlerTest.ONE_NAME));
		one.add("href", new JsonPrimitive(JsonObjectResponseHandlerTest.ONE_HREF));
		layerList.add(one);
		JsonObject two = new JsonObject();
		two.add("name", new JsonPrimitive(JsonObjectResponseHandlerTest.TWO_NAME));
		two.add("href", new JsonPrimitive(JsonObjectResponseHandlerTest.TWO_HREF));
		layerList.add(two);
		JsonObject three = new JsonObject();
		three.add("name", new JsonPrimitive(JsonObjectResponseHandlerTest.THREE_NAME));
		three.add("href", new JsonPrimitive(JsonObjectResponseHandlerTest.THREE_HREF));
		layerList.add(three);
		return rtn;
	}

	public static JsonObject buildEmptyDataStoresTestDataStoreResponse() {
		JsonObject rtn = new JsonObject();
		JsonObject dataStores = new JsonObject();
		rtn.add(WQPDynamicLayerCachingService.DATASTORES, dataStores);
		JsonArray dataStoreList = new JsonArray();
		dataStores.add(WQPDynamicLayerCachingService.DATASTORE, dataStoreList);
		return rtn;
	}

	public static JsonObject buildDataStoreNotArrayTestDataStoreResponse() {
		JsonObject rtn = new JsonObject();
		JsonObject dataStores = new JsonObject();
		rtn.add(WQPDynamicLayerCachingService.DATASTORES, dataStores);
		JsonPrimitive dataStoreList = new JsonPrimitive("hello");
		dataStores.add(WQPDynamicLayerCachingService.DATASTORE, dataStoreList);
		return rtn;
	}

	public static JsonObject buildNoDataStoreTestDataStoreResponse() {
		JsonObject rtn = new JsonObject();
		JsonObject dataStores = new JsonObject();
		rtn.add(WQPDynamicLayerCachingService.DATASTORES, dataStores);
		JsonPrimitive notDataStoreList = new JsonPrimitive("hello");
		dataStores.add("world", notDataStoreList);
		return rtn;
	}

	public static JsonObject buildDataStoresNotObjectTestDataStoreResponse() {
		JsonObject rtn = new JsonObject();
		JsonPrimitive dataStores = new JsonPrimitive("hello");
		rtn.add(WQPDynamicLayerCachingService.DATASTORES, dataStores);
		return rtn;
	}

}
