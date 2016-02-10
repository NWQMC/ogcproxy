package gov.usgs.wqp.ogcproxy.model.cache;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import gov.usgs.wqp.ogcproxy.model.OGCRequest;
import gov.usgs.wqp.ogcproxy.model.ogc.services.OGCServices;
import gov.usgs.wqp.ogcproxy.model.parameters.SearchParameters;
import gov.usgs.wqp.ogcproxy.model.status.DynamicLayerStatus;

public class DynamicLayerCacheTest {

	@Test
	public void twoArgConstructorTest() {
		DynamicLayerCache cache = new DynamicLayerCache("test", "workspace");
		assertEquals("test", cache.getLayerName());
		assertEquals(DynamicLayerStatus.AVAILABLE, cache.getCurrentStatus());
	}

	@Test
	public void singleArgConstructorTest() {
		SearchParameters<String, List<String>> searchParams = new SearchParameters<>();
		searchParams.put("hello", Arrays.asList("world"));
		OGCRequest ogcRequest = new OGCRequest(OGCServices.WFS, null, searchParams, null);
		DynamicLayerCache cache = new DynamicLayerCache(ogcRequest, "qw_portal_map");
		assertEquals(DynamicLayerCache.DYNAMIC_LAYER_PREFIX + "3187351746", cache.getLayerName());
		assertEquals("3187351746", cache.getKey());
		assertEquals(searchParams, cache.getSearchParameters());
		assertEquals(DynamicLayerStatus.INITIATED, cache.getCurrentStatus());
		assertNotNull(cache.getDateCreated());
		assertEquals(OGCServices.WFS, cache.getOriginatingService());

	}

	@Test
	public void getQualifiedLayerNameTest() {
		DynamicLayerCache cache = new DynamicLayerCache("test", "workspace");
		assertEquals("workspace:test", cache.getQualifiedLayerName());
	}

}
