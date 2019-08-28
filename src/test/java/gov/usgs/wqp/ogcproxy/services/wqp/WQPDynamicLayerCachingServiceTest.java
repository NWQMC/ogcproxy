package gov.usgs.wqp.ogcproxy.services.wqp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.apache.http.HttpStatus;
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
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import gov.usgs.wqp.ogcproxy.exceptions.OGCProxyException;
import gov.usgs.wqp.ogcproxy.geo.JsonObjectResponseHandler;
import gov.usgs.wqp.ogcproxy.geo.JsonObjectResponseHandlerTest;
import gov.usgs.wqp.ogcproxy.model.DynamicLayer;
import gov.usgs.wqp.ogcproxy.model.OGCRequest;
import gov.usgs.wqp.ogcproxy.model.ogc.services.OGCServices;
import gov.usgs.wqp.ogcproxy.model.parameters.SearchParameters;
import gov.usgs.wqp.ogcproxy.services.ConfigurationService;
import gov.usgs.wqp.ogcproxy.utils.GeoServerUtils;

@DirtiesContext(classMode=ClassMode.AFTER_CLASS)
public class WQPDynamicLayerCachingServiceTest {

	@Mock
	private CloseableHttpClient httpClient;

	@Mock
	private HttpClientBuilder httpClientBuilder;

	@Mock
	private ConfigurationService configurationService;

	@Mock
	private GeoServerUtils geoServerUtils;

	@Mock
	private CloseableHttpResponse response;

	@Mock
	private WQPLayerBuildingService wqpLayerBuildingService;

	@Spy
	private WQPDynamicLayerCachingService service = WQPDynamicLayerCachingService.getInstance();

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
		when(geoServerUtils.buildResourceRestDelete()).thenReturn("http://owi.usgs.gov/geoserver");
		when(configurationService.getGeoserverWorkspace()).thenReturn(WQP_WORKSPACE);

		service.geoServerUtils = geoServerUtils;
		service.wqpLayerBuildingService = wqpLayerBuildingService;

		service.httpClient = httpClient;
		service.configurationService = configurationService;
	}

	@Test
	public void clearCacheTest() {
		int cnt = service.clearCache();
		verify(service).deleteGeoserverResources();
		verify(service).clearGeoserverWorkspace();
		verify(service).clearInMemoryCache();

		//Do it a second time to verify it really cleared the cache.
		cnt = service.clearCache();
		verify(service, times(2)).deleteGeoserverResources();
		verify(service, times(2)).clearGeoserverWorkspace();
		verify(service, times(2)).clearInMemoryCache();

		assertEquals(0, cnt);
	}

	@Test
	public void deleteGeoserverResourcesTest() {
		try {
			when(httpClient.execute(any(HttpDelete.class), any(HttpClientContext.class))).thenThrow(new IOException("Hi")).thenReturn(response);

			service.deleteGeoserverResources();
			verify(httpClient, times(1)).execute(any(HttpDelete.class), any(HttpClientContext.class));

			service.deleteGeoserverResources();
			verify(httpClient, times(2)).execute(any(HttpDelete.class), any(HttpClientContext.class));
		} catch (Exception e) {
			e.printStackTrace();
			fail("shouldn't get here at this time");
		}
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
	public void clearInMemoryCachetest() throws OGCProxyException, InterruptedException, ExecutionException {
		DynamicLayer dla = new DynamicLayer("dynamicSites_123456", "qw_portal_map");
		SearchParameters<String, List<String>> searchParams = new SearchParameters<>();
		searchParams.put("key", Arrays.asList("a", "b"));
		OGCRequest ogcRequest = new OGCRequest(OGCServices.WFS, null, searchParams, null);
		DynamicLayer dlb = new DynamicLayer(ogcRequest, "qw_portal_map");
		when(wqpLayerBuildingService.buildDynamicLayer(any(DynamicLayer.class))).thenReturn(dla, dlb);
		try {
			//Not really a unit test, but easiest way to get data into the cache...
			//Make sure it is clear to start
			service.clearInMemoryCache();
			service.getLayer(dla);
			service.getLayer(dlb);
		} catch (Exception e) {
			e.printStackTrace();
			fail("Didn't expect to get exception: " + e.getLocalizedMessage());
		}

		int cnt = service.clearInMemoryCache();
		assertEquals(2, cnt);
		assertTrue(service.getCacheValues().isEmpty());
	}

	@Test
	public void getLayerCacheTest() throws OGCProxyException {
		DynamicLayer dla = new DynamicLayer("dynamicSites_123456", "qw_portal_map");
		DynamicLayer dlb = new DynamicLayer("12345678", "testWorkspace");
		when(wqpLayerBuildingService.buildDynamicLayer(any(DynamicLayer.class))).thenReturn(dla, dlb);

		try {
			//New Layer
			assertEquals(dla, service.getLayer(dla));
			verify(wqpLayerBuildingService).buildDynamicLayer(any(DynamicLayer.class));
		} catch (Exception e) {
			e.printStackTrace();
			fail("Didn't expect to get exception: " + e.getLocalizedMessage());
		}

		try {
			//New Layer
			assertEquals(dlb, service.getLayer(dlb));
			verify(wqpLayerBuildingService, times(2)).buildDynamicLayer(any(DynamicLayer.class));
		} catch (Exception e) {
			e.printStackTrace();
			fail("Didn't expect to get exception: " + e.getLocalizedMessage());
		}

		try {
			//Existing Layer
			assertEquals(dla, service.getLayer(dla));
			verify(wqpLayerBuildingService, times(2)).buildDynamicLayer(any(DynamicLayer.class));
		} catch (Exception e) {
			e.printStackTrace();
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
		when(configurationService.getThreadSleep()).thenReturn(Long.valueOf("500"), Long.valueOf("456"));

		service.configurationService = configurationService;
		service.initialize();

		//should be 500
		assertEnvironmentDefaults();
		verify(configurationService, times(1)).getThreadSleep();

		//still 500 because already initialized...
		service.initialize();
		assertEnvironmentDefaults();
		verify(configurationService, times(1)).getThreadSleep();

		//start over and make 456.
		service.initialized = false;
		service.initialize();
		assertEquals(456, service.threadSleep);
		verify(configurationService, times(2)).getThreadSleep();
	}

	@Test
	public void populateCacheTest() throws UnsupportedOperationException, IOException, OGCProxyException, InterruptedException, ExecutionException {
		when(httpClient.execute(any(HttpGet.class), any(JsonObjectResponseHandler.class), any(HttpClientContext.class)))
				.thenThrow(new HttpResponseException(HttpStatus.SC_NOT_FOUND, "fake not found"))
				.thenReturn(null, new JsonObject(), buildEmptyDataStoresTestDataStoreResponse(), buildCompleteTestDataSourceResponse());

		//Workspace not found
		service.populateCache();
		assertTrue(service.getCacheValues().isEmpty());
		verify(httpClient).execute(any(HttpGet.class), any(JsonObjectResponseHandler.class), any(HttpClientContext.class));
		verify(geoServerUtils).createWorkspace(any(CloseableHttpClient.class), any(HttpClientContext.class));

		//Null response
		service.populateCache();
		assertTrue(service.getCacheValues().isEmpty());
		verify(httpClient, times(2)).execute(any(HttpGet.class), any(JsonObjectResponseHandler.class), any(HttpClientContext.class));

		//No Layers
		service.populateCache();
		assertTrue(service.getCacheValues().isEmpty());

		//Empty Layer List
		service.populateCache();
		assertTrue(service.getCacheValues().isEmpty());

		//Some Layers
		service.populateCache();
		Collection<DynamicLayer> cache = service.getCacheValues();
		assertEquals(3, cache.size());

		Map<String, DynamicLayer> test = new HashMap<>();
		Iterator<DynamicLayer> i = cache.iterator();
		while (i.hasNext()) {
			DynamicLayer dl = i.next();
			test.put(dl.getLayerName(), dl);
		}
		assertTrue(test.containsKey(JsonObjectResponseHandlerTest.ONE_NAME));
		DynamicLayer one = test.get(JsonObjectResponseHandlerTest.ONE_NAME);
		assertEquals(JsonObjectResponseHandlerTest.ONE_NAME, one.getLayerName());
		assertEquals(ONE_QUAL_NAME, one.getQualifiedLayerName());

		assertTrue(test.containsKey(JsonObjectResponseHandlerTest.TWO_NAME));
		DynamicLayer two = test.get(JsonObjectResponseHandlerTest.TWO_NAME);
		assertEquals(JsonObjectResponseHandlerTest.TWO_NAME, two.getLayerName());
		assertEquals(TWO_QUAL_NAME, two.getQualifiedLayerName());

		assertTrue(test.containsKey(JsonObjectResponseHandlerTest.THREE_NAME));
		DynamicLayer three = test.get(JsonObjectResponseHandlerTest.THREE_NAME);
		assertEquals(JsonObjectResponseHandlerTest.THREE_NAME, three.getLayerName());
		assertEquals(THREE_QUAL_NAME, three.getQualifiedLayerName());
	}

	private void assertEnvironmentDefaults() {
		assertEquals(500, service.threadSleep);
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

	public static Collection<DynamicLayer> getCompleteTestData() {
		Collection<DynamicLayer> rtn = new ArrayList<>();
		rtn.add(new DynamicLayer(JsonObjectResponseHandlerTest.ONE_NAME, ""));
		rtn.add(new DynamicLayer(JsonObjectResponseHandlerTest.TWO_NAME, ""));
		rtn.add(new DynamicLayer(JsonObjectResponseHandlerTest.THREE_NAME, ""));
		return rtn;
	}
}
