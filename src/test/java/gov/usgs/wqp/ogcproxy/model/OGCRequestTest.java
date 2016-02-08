package gov.usgs.wqp.ogcproxy.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import gov.usgs.wqp.ogcproxy.model.ogc.parameters.WFSParameters;
import gov.usgs.wqp.ogcproxy.model.ogc.parameters.WMSParameters;
import gov.usgs.wqp.ogcproxy.model.ogc.services.OGCServices;
import gov.usgs.wqp.ogcproxy.model.parameters.ProxyDataSourceParameter;
import gov.usgs.wqp.ogcproxy.model.parameters.SearchParameters;

public class OGCRequestTest {

	@Test
	public void oneArgConstructorTest() {
		OGCRequest ogcRequest = new OGCRequest(null);
		assertNull(ogcRequest.getOgcService());
		assertTrue(ogcRequest.getOgcParams().isEmpty());
		assertTrue(ogcRequest.getSearchParams().isEmpty());
		assertEquals("", ogcRequest.getRequestType());
		assertTrue(ogcRequest.getReplaceableLayers().isEmpty());
		assertEquals(ProxyDataSourceParameter.UNKNOWN, ogcRequest.getDataSource());

		ogcRequest = new OGCRequest(OGCServices.WFS);
		assertEquals(OGCServices.WFS, ogcRequest.getOgcService());
		assertTrue(ogcRequest.getOgcParams().isEmpty());
		assertTrue(ogcRequest.getSearchParams().isEmpty());
		assertEquals("", ogcRequest.getRequestType());
		assertTrue(ogcRequest.getReplaceableLayers().isEmpty());
		assertEquals(ProxyDataSourceParameter.UNKNOWN, ogcRequest.getDataSource());
	}

	@Test
	public void threeArgConstructorTest() {
		Map<String, String> ogcParams = new HashMap<>();
		ogcParams.put(WMSParameters.layer.toString(), ProxyDataSourceParameter.WQP_SITES.toString());
		ogcParams.put("ReQuEsT", OGCRequest.GET_LEGEND_GRAPHIC);
		SearchParameters<String, List<String>> searchParams = new SearchParameters<>();
		searchParams.put("wqp", Arrays.asList("nope", "nada"));

		OGCRequest ogcRequest = new OGCRequest(null, null, null);
		assertNull(ogcRequest.getOgcService());
		assertTrue(ogcRequest.getOgcParams().isEmpty());
		assertTrue(ogcRequest.getSearchParams().isEmpty());
		assertEquals("", ogcRequest.getRequestType());
		assertTrue(ogcRequest.getReplaceableLayers().isEmpty());
		assertEquals(ProxyDataSourceParameter.UNKNOWN, ogcRequest.getDataSource());

		ogcRequest = new OGCRequest(OGCServices.WMS, ogcParams, searchParams);
		assertEquals(OGCServices.WMS, ogcRequest.getOgcService());
		assertTrue(ogcRequest.getOgcParams().equals(ogcParams));
		assertTrue(ogcRequest.getSearchParams().equals(searchParams));
		assertEquals(OGCRequest.GET_LEGEND_GRAPHIC, ogcRequest.getRequestType());
		assertEquals(1, ogcRequest.getReplaceableLayers().size());
		assertTrue(ogcRequest.getReplaceableLayers().contains(WMSParameters.layer.toString()));
		assertEquals(ProxyDataSourceParameter.WQP_SITES, ogcRequest.getDataSource());
	}

	@Test
	public void setRequestTypeTest() {
		OGCRequest ogcRequest = new OGCRequest(null);
		Map<String, String> ogcParams = new HashMap<>();
		
		ogcRequest.setRequestType(null);
		assertEquals("", ogcRequest.getRequestType());
		
		ogcRequest.setRequestType(ogcParams);
		assertEquals("", ogcRequest.getRequestType());

		ogcParams.put("ReQuEsT", null);
		ogcRequest.setRequestType(ogcParams);
		assertEquals("", ogcRequest.getRequestType());

		ogcParams.put("ReQuEsT", "frack");
		ogcRequest.setRequestType(ogcParams);
		assertEquals("frack", ogcRequest.getRequestType());
	}

	@Test
	public void setReplaceableLayersAndDataSourceTest() {
		OGCRequest ogcRequest = new OGCRequest(null);
		Map<String, String> ogcParams = new HashMap<>();
		
		ogcRequest.setReplaceableLayersAndDataSource(null, null, null);
		assertTrue(ogcRequest.getReplaceableLayers().isEmpty());
		assertEquals(ProxyDataSourceParameter.UNKNOWN, ogcRequest.getDataSource());
		
		ogcRequest.setReplaceableLayersAndDataSource(OGCServices.WMS, null, null);
		assertTrue(ogcRequest.getReplaceableLayers().isEmpty());
		assertEquals(ProxyDataSourceParameter.UNKNOWN, ogcRequest.getDataSource());
		
		ogcRequest.setReplaceableLayersAndDataSource(OGCServices.WMS, ogcParams, null);
		assertTrue(ogcRequest.getReplaceableLayers().isEmpty());
		assertEquals(ProxyDataSourceParameter.UNKNOWN, ogcRequest.getDataSource());

		ogcParams.put(WMSParameters.layer.toString(), ProxyDataSourceParameter.WQP_SITES.toString());
		ogcParams.put(WMSParameters.layers.toString(), ProxyDataSourceParameter.WQP_SITES.toString());
		ogcParams.put(WMSParameters.query_layers.toString(), ProxyDataSourceParameter.WQP_SITES.toString());
		
		ogcRequest.setReplaceableLayersAndDataSource(OGCServices.WMS, ogcParams, OGCRequest.GET_LEGEND_GRAPHIC);
		assertEquals(1, ogcRequest.getReplaceableLayers().size());
		assertTrue(ogcRequest.getReplaceableLayers().contains(WMSParameters.layer.toString()));
		assertEquals(ProxyDataSourceParameter.WQP_SITES, ogcRequest.getDataSource());
		
		ogcRequest.setReplaceableLayersAndDataSource(OGCServices.WMS, ogcParams, null);
		assertEquals(2, ogcRequest.getReplaceableLayers().size());
		assertTrue(ogcRequest.getReplaceableLayers().contains(WMSParameters.layers.toString()));
		assertTrue(ogcRequest.getReplaceableLayers().contains(WMSParameters.query_layers.toString()));
		assertEquals(ProxyDataSourceParameter.WQP_SITES, ogcRequest.getDataSource());
		
		ogcParams.clear();
		ogcRequest.setReplaceableLayersAndDataSource(OGCServices.WFS, null, null);
		assertTrue(ogcRequest.getReplaceableLayers().isEmpty());
		assertEquals(ProxyDataSourceParameter.UNKNOWN, ogcRequest.getDataSource());
		
		ogcRequest.setReplaceableLayersAndDataSource(OGCServices.WFS, ogcParams, null);
		assertTrue(ogcRequest.getReplaceableLayers().isEmpty());
		assertEquals(ProxyDataSourceParameter.UNKNOWN, ogcRequest.getDataSource());
		
		ogcParams.put(WFSParameters.typeName.toString(), ProxyDataSourceParameter.WQP_SITES.toString());
		ogcRequest.setReplaceableLayersAndDataSource(OGCServices.WFS, ogcParams, null);
		assertEquals(1, ogcRequest.getReplaceableLayers().size());
		assertTrue(ogcRequest.getReplaceableLayers().contains(WFSParameters.typeName.toString()));
		assertEquals(ProxyDataSourceParameter.WQP_SITES, ogcRequest.getDataSource());
		
		ogcParams.clear();
		ogcParams.put(WFSParameters.typeNames.toString(), ProxyDataSourceParameter.WQP_SITES.toString());
		ogcRequest.setReplaceableLayersAndDataSource(OGCServices.WFS, ogcParams, null);
		assertEquals(1, ogcRequest.getReplaceableLayers().size());
		assertTrue(ogcRequest.getReplaceableLayers().contains(WFSParameters.typeNames.toString()));
		assertEquals(ProxyDataSourceParameter.WQP_SITES, ogcRequest.getDataSource());
	}

	@Test
	public void checkIfApplicableTest() {
		OGCRequest ogcRequest = new OGCRequest(null);
		Map<String, String> ogcParams = new HashMap<>();

		assertEquals("", ogcRequest.checkIfApplicable(null, null));
		assertEquals("", ogcRequest.checkIfApplicable(ogcParams, null));
		assertEquals("", ogcRequest.checkIfApplicable(null, "abc"));
		assertEquals("", ogcRequest.checkIfApplicable(ogcParams, "abc"));
		
		ogcParams.put("XyZ", null);
		assertEquals("", ogcRequest.checkIfApplicable(ogcParams, "abc"));

		assertEquals("", ogcRequest.checkIfApplicable(ogcParams, "xyz"));
		ogcParams.put("XyZ", "");
		assertEquals("", ogcRequest.checkIfApplicable(ogcParams, "xyz"));
		ogcParams.put("XyZ", ProxyDataSourceParameter.WQP_SITES.toString());
		assertEquals("XyZ", ogcRequest.checkIfApplicable(ogcParams, "xyz"));
		ogcParams.put("XyZ", "abc," + ProxyDataSourceParameter.WQP_SITES.toString());
		assertEquals("XyZ", ogcRequest.checkIfApplicable(ogcParams, "xyz"));
	}

	@Test
	public void isValidRequestTest() {
		Map<String, String> ogcParams = new HashMap<>();
		ogcParams.put(WMSParameters.layer.toString(), ProxyDataSourceParameter.WQP_SITES.toString());
		ogcParams.put("ReQuEsT", OGCRequest.GET_LEGEND_GRAPHIC);
		SearchParameters<String, List<String>> searchParams = new SearchParameters<>();
		searchParams.put("wqp", Arrays.asList("nope", "nada"));

		OGCRequest ogcRequest = new OGCRequest(OGCServices.WMS, null, null);
		assertFalse(ogcRequest.isValidRequest());

		ogcRequest = new OGCRequest(OGCServices.WMS, null, searchParams);
		assertFalse(ogcRequest.isValidRequest());

		ogcRequest = new OGCRequest(OGCServices.WMS, ogcParams, null);
		assertFalse(ogcRequest.isValidRequest());

		ogcRequest = new OGCRequest(OGCServices.WMS, ogcParams, searchParams);
		assertTrue(ogcRequest.isValidRequest());
	}

	@Test
	public void setLayerFromVendorTest() {
		Map<String, String> ogcParams = new HashMap<>();
		ogcParams.put(WMSParameters.layers.toString(), ProxyDataSourceParameter.WQP_SITES.toString());
		ogcParams.put(WMSParameters.query_layers.toString(), ProxyDataSourceParameter.WQP_SITES.toString());
		SearchParameters<String, List<String>> searchParams = new SearchParameters<>();
		searchParams.put("wqp", Arrays.asList("nope", "nada"));

		OGCRequest ogcRequest = new OGCRequest(OGCServices.WMS, ogcParams, searchParams);
		ogcRequest.setLayerFromVendor("hah");
		assertEquals(ogcRequest.getOgcParams().get(WMSParameters.layers.toString()), "hah");
		assertEquals(ogcRequest.getOgcParams().get(WMSParameters.query_layers.toString()), "hah");

		ogcParams.put(WMSParameters.layers.toString(), "abc," + ProxyDataSourceParameter.WQP_SITES.toString());
		ogcParams.put(WMSParameters.query_layers.toString(), ProxyDataSourceParameter.WQP_SITES.toString()+ ",abc");
		ogcRequest = new OGCRequest(OGCServices.WMS, ogcParams, searchParams);
		ogcRequest.setLayerFromVendor("hah");
		assertEquals(ogcRequest.getOgcParams().get(WMSParameters.layers.toString()), "abc,hah");
		assertEquals(ogcRequest.getOgcParams().get(WMSParameters.query_layers.toString()), "hah,abc");
	}

}
