package gov.usgs.wqp.ogcproxy.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import gov.usgs.wqp.ogcproxy.model.OGCRequest;
import gov.usgs.wqp.ogcproxy.model.ogc.parameters.WMSParameters;
import gov.usgs.wqp.ogcproxy.model.ogc.services.OGCServices;
import gov.usgs.wqp.ogcproxy.model.parameters.ProxyDataSourceParameter;

public class ProxyUtilTest {

	@Mock
	private HttpServletRequest clientrequest;

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
	}

	@Test
	public void getRequestedServiceTest() {
		Map<String, String[]> wfs = new HashMap<>();
		wfs.put(ProxyUtil.OGC_SERVICE_PARAMETER, new String[] {"qwerty"});
		assertEquals(OGCServices.WFS, ProxyUtil.getRequestedService(OGCServices.WFS, wfs));
		
		wfs.put(ProxyUtil.OGC_SERVICE_PARAMETER, new String[] {"WFS"});
		Map<String, String[]> wms = new HashMap<>();
		wms.put(ProxyUtil.OGC_SERVICE_PARAMETER, new String[] {"WMS"});
		assertEquals(OGCServices.WFS, ProxyUtil.getRequestedService(OGCServices.WFS, null));
		assertEquals(OGCServices.WFS, ProxyUtil.getRequestedService(OGCServices.WFS, wfs));
		assertEquals(OGCServices.WMS, ProxyUtil.getRequestedService(OGCServices.WFS, wms));
		assertEquals(OGCServices.WMS, ProxyUtil.getRequestedService(OGCServices.WMS, null));
		assertEquals(OGCServices.WFS, ProxyUtil.getRequestedService(OGCServices.WMS, wfs));
		assertEquals(OGCServices.WMS, ProxyUtil.getRequestedService(OGCServices.WMS, wms));

		//Try with mixed case
		wfs.put(ProxyUtil.OGC_SERVICE_PARAMETER, new String[] {"WfS"});
		wms.put(ProxyUtil.OGC_SERVICE_PARAMETER, new String[] {"WmS"});
		assertEquals(OGCServices.WFS, ProxyUtil.getRequestedService(OGCServices.WFS, wfs));
		assertEquals(OGCServices.WMS, ProxyUtil.getRequestedService(OGCServices.WFS, wms));
		assertEquals(OGCServices.WFS, ProxyUtil.getRequestedService(OGCServices.WMS, wfs));
		assertEquals(OGCServices.WMS, ProxyUtil.getRequestedService(OGCServices.WMS, wms));

		//Try with lower case
		wfs.put(ProxyUtil.OGC_SERVICE_PARAMETER, new String[] {"wfs"});
		wms.put(ProxyUtil.OGC_SERVICE_PARAMETER, new String[] {"wms"});
		assertEquals(OGCServices.WFS, ProxyUtil.getRequestedService(OGCServices.WFS, wfs));
		assertEquals(OGCServices.WMS, ProxyUtil.getRequestedService(OGCServices.WFS, wms));
		assertEquals(OGCServices.WFS, ProxyUtil.getRequestedService(OGCServices.WMS, wfs));
		assertEquals(OGCServices.WMS, ProxyUtil.getRequestedService(OGCServices.WMS, wms));
		
		//Try with wonky service
		wfs.put("sErViCe", new String[] {"wfs"});
		wms.put("sErViCe", new String[] {"wms"});
		assertEquals(OGCServices.WFS, ProxyUtil.getRequestedService(OGCServices.WFS, wfs));
		assertEquals(OGCServices.WMS, ProxyUtil.getRequestedService(OGCServices.WFS, wms));
		assertEquals(OGCServices.WFS, ProxyUtil.getRequestedService(OGCServices.WMS, wfs));
		assertEquals(OGCServices.WMS, ProxyUtil.getRequestedService(OGCServices.WMS, wms));
		
	}

//	@Test
//	public void separateParametersTest() {
//		OGCRequest ogcRequest = ProxyUtil.separateParameters(null, null);
//		assertNotNull(ogcRequest);
//		
//		
//		ogcRequest = ProxyUtil.separateParameters(OGCServices.WFS, null);
//		assertEquals(OGCServices.WFS, ogcRequest.getOgcService());
//		
//		
//		Map<String, String> requestParams = new HashMap<>();
//		ogcRequest = ProxyUtil.separateParameters(OGCServices.WFS, requestParams);
//		assertEquals(OGCServices.WFS, ogcRequest.getOgcService());
//
//		
//		requestParams.put(WMSParameters.layer.toString(), ProxyDataSourceParameter.WQP_SITES.toString());
//		requestParams.put("ReQuEsT", OGCRequest.GET_LEGEND_GRAPHIC);
//		requestParams.put("sErViCe", OGCServices.WMS.toString());
//		ogcRequest = ProxyUtil.separateParameters(OGCServices.WFS, requestParams);
//		assertEquals(OGCServices.WMS, ogcRequest.getOgcService());
//		
//		assertEquals(3, ogcRequest.getOgcParams().size());
//		assertEquals(ProxyDataSourceParameter.WQP_SITES.toString(), ogcRequest.getOgcParams().get(WMSParameters.layer.toString()));
//		assertEquals(OGCRequest.GET_LEGEND_GRAPHIC, ogcRequest.getOgcParams().get("ReQuEsT"));
//		assertEquals(OGCServices.WMS.toString(), ogcRequest.getOgcParams().get("sErViCe"));
//
//		assertTrue(ogcRequest.getSearchParams().isEmpty());
//
//		
//		requestParams.put("searchParams", null);
//		ogcRequest = ProxyUtil.separateParameters(OGCServices.WFS, requestParams);
//		assertEquals(OGCServices.WMS, ogcRequest.getOgcService());
//		
//		assertEquals(3, ogcRequest.getOgcParams().size());
//		assertEquals(ProxyDataSourceParameter.WQP_SITES.toString(), ogcRequest.getOgcParams().get(WMSParameters.layer.toString()));
//		assertEquals(OGCRequest.GET_LEGEND_GRAPHIC, ogcRequest.getOgcParams().get("ReQuEsT"));
//		assertEquals(OGCServices.WMS.toString(), ogcRequest.getOgcParams().get("sErViCe"));
//
//		assertTrue(ogcRequest.getSearchParams().isEmpty());
//		
//
//		requestParams.put("searchParams", "huc:06*|07*;sampleMedia:Water;characteristicType:Nutrient");
//		ogcRequest = ProxyUtil.separateParameters(OGCServices.WFS, requestParams);
//		assertEquals(OGCServices.WMS, ogcRequest.getOgcService());
//		
//		assertEquals(3, ogcRequest.getOgcParams().size());
//		assertEquals(ProxyDataSourceParameter.WQP_SITES.toString(), ogcRequest.getOgcParams().get(WMSParameters.layer.toString()));
//		assertEquals(OGCRequest.GET_LEGEND_GRAPHIC, ogcRequest.getOgcParams().get("ReQuEsT"));
//		assertEquals(OGCServices.WMS.toString(), ogcRequest.getOgcParams().get("sErViCe"));
//
//		assertEquals(3, ogcRequest.getSearchParams().size());
//		assertEquals(Arrays.asList("06*", "07*"), ogcRequest.getSearchParams().get("huc"));
//		assertEquals(Arrays.asList("Water"), ogcRequest.getSearchParams().get("sampleMedia"));
//		assertEquals(Arrays.asList("Nutrient"), ogcRequest.getSearchParams().get("characteristicType"));
//	}

	@Test
	public void getServerRequestURIAsStringTest() {
		when(clientrequest.getServletPath()).thenReturn("/goHere");

		Map<String, String> ogcParams = new HashMap<>();
		ogcParams.put(WMSParameters.layer.toString(), "j@ck:e");
		ogcParams.put("ReQuEsT", OGCRequest.GET_LEGEND_GRAPHIC);

		assertEquals("http://bbggr.org/proxy/goHere?ReQuEsT=GetLegendGraphic&layer=j%40ck%3Ae", ProxyUtil.getServerRequestURIAsString(clientrequest, ogcParams, "http://bbggr.org", "proxy"));
	}

}
