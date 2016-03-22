package gov.usgs.wqp.ogcproxy.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.servlet.ModelAndView;

import gov.usgs.wqp.ogcproxy.model.OGCRequest;
import gov.usgs.wqp.ogcproxy.model.cache.DynamicLayerCache;
import gov.usgs.wqp.ogcproxy.model.ogc.services.OGCServices;
import gov.usgs.wqp.ogcproxy.model.parameters.ProxyDataSourceParameter;
import gov.usgs.wqp.ogcproxy.services.wqp.WQPDynamicLayerCachingService;

public class RESTServiceTest {

	@Mock
	private WQPDynamicLayerCachingService layerCachingService;
	
	private RESTService service;

    @Before
    public void setup() {
    	MockitoAnnotations.initMocks(this);
    	service = new RESTService(layerCachingService);
    }

    @Test
	public void checkCacheStatusOkTest() {
		Map<String, DynamicLayerCache> cache = new HashMap<>();
		cache.put("abc", new DynamicLayerCache(new OGCRequest(OGCServices.WMS), "abcWorkspace"));
		when(layerCachingService.getCacheStore()).thenReturn(cache);

		ModelAndView mv = service.checkCacheStatus(ProxyDataSourceParameter.WQP_SITES.toString());
		assertEquals("wqp_cache_status.jsp", mv.getViewName());
		assertTrue(mv.getModelMap().containsKey("site"));
		assertEquals("WQP Layer Building Service", mv.getModelMap().get("site"));
		assertTrue(mv.getModelMap().containsKey("cache"));
		assertEquals(cache.values(), mv.getModelMap().get("cache"));
    }

    @Test
	public void checkCacheStatusBadTest() {
		ModelAndView mv = service.checkCacheStatus("no_sites_here");
		assertEquals("invalid_site.jsp", mv.getViewName());
		assertTrue(mv.getModelMap().containsKey("site"));
		assertEquals("no_sites_here", mv.getModelMap().get("site"));
    }
	
    @Test
    public void clearCacheBySiteOkTest() {
		when(layerCachingService.clearCache()).thenReturn(5);

		assertTrue(service.clearCacheBySite(ProxyDataSourceParameter.WQP_SITES.toString()));
    }
	
    @Test
    public void clearCacheBySiteBadTest() {
		assertFalse(service.clearCacheBySite("no_sites_here"));
    }

}
