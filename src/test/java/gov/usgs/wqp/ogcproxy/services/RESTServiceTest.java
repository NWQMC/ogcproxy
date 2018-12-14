package gov.usgs.wqp.ogcproxy.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ExecutionException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.servlet.ModelAndView;

import gov.usgs.wqp.ogcproxy.model.DynamicLayer;
import gov.usgs.wqp.ogcproxy.model.OGCRequest;
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
	public void clearCacheBySiteOkTest() {
		when(layerCachingService.clearCache()).thenReturn(5);

		assertTrue(service.clearCacheBySite(ProxyDataSourceParameter.WQP_SITES.toString()));
	}

	@Test
	public void clearCacheBySiteBadTest() {
		assertFalse(service.clearCacheBySite("no_sites_here"));
	}

}
