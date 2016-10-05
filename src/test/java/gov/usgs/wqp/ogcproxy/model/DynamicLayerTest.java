package gov.usgs.wqp.ogcproxy.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import gov.usgs.wqp.ogcproxy.model.ogc.services.OGCServices;
import gov.usgs.wqp.ogcproxy.model.parameters.SearchParameters;

public class DynamicLayerTest {

	private static final String LAYER_SUFFIX = "3187351746";
	private static final String LAYER_NAME = DynamicLayer.DYNAMIC_LAYER_PREFIX + LAYER_SUFFIX;

	@Test
	public void twoArgConstructorTest() {
		DynamicLayer cache = new DynamicLayer("test", "workspace");
		assertEquals("test", cache.getLayerName());
	}

	@Test
	public void singleArgConstructorTest() {
		OGCRequest ogcRequest = getOGCRequest();
		DynamicLayer cache = new DynamicLayer(getOGCRequest(), "qw_portal_map");
		assertEquals(LAYER_NAME, cache.getLayerName());
		assertEquals(ogcRequest.getSearchParams(), cache.getSearchParameters());
		assertNotNull(cache.getDateCreated());
		assertEquals(OGCServices.WFS, cache.getOriginatingService());
	}

	@Test
	public void getQualifiedLayerNameTest() {
		DynamicLayer cache = new DynamicLayer("test", "workspace");
		assertEquals("workspace:test", cache.getQualifiedLayerName());
	}

	@Test
	public void buildLayerNameTest() {
		assertEquals(LAYER_NAME, DynamicLayer.buildLayerName(getOGCRequest()));
	}

	private OGCRequest getOGCRequest() {
		SearchParameters<String, List<String>> searchParams = new SearchParameters<>();
		searchParams.put("hello", Arrays.asList("world"));
		return new OGCRequest(OGCServices.WFS, null, searchParams, null);
	}
}
