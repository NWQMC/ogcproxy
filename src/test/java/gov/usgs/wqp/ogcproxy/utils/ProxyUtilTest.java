package gov.usgs.wqp.ogcproxy.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.message.BasicHeader;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.DelegatingServletInputStream;
import org.springframework.mock.web.MockHttpServletResponse;

import gov.usgs.wqp.ogcproxy.model.OGCRequest;
import gov.usgs.wqp.ogcproxy.model.ogc.parameters.WFSParameters;
import gov.usgs.wqp.ogcproxy.model.ogc.parameters.WMSParameters;
import gov.usgs.wqp.ogcproxy.model.ogc.services.OGCServices;
import gov.usgs.wqp.ogcproxy.model.parameters.ProxyDataSourceParameter;
import gov.usgs.wqp.ogcproxy.model.parameters.WQPParameters;
import gov.usgs.wqp.ogcproxy.model.parser.OgcParserTest;

public class ProxyUtilTest {

	@Mock
	private HttpServletRequest clientRequest;
	private HttpServletResponse clientResponse;
	private HttpUriRequest serverRequest;
	@Mock
	private HttpResponse serverResponse;
	@Mock
	private Header header;

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
	}

	@Test
	public void generateClientResponseHeadersTest() {
		Header[] headers = {new BasicHeader("transfer-encoding", "tEncoding"), new BasicHeader(HttpHeaders.WARNING, "Yikes!")};
		when(serverResponse.getAllHeaders()).thenReturn(headers);
		clientResponse = new MockHttpServletResponse();
		ProxyUtil.generateClientResponseHeaders(clientResponse, serverResponse);
		assertTrue(clientResponse.getHeaderNames().contains(HttpHeaders.WARNING));
		assertEquals("Yikes!", clientResponse.getHeader(HttpHeaders.WARNING));
		assertFalse(clientResponse.getHeaderNames().contains("transfer-encoding"));
		assertFalse(clientResponse.getHeaderNames().contains(HttpHeaders.TRANSFER_ENCODING));
	}

	@Test
	public void generateServerRequestHeadersTest() {
		Hashtable<String, String> headers = new Hashtable<>();
		headers.put("content-length", "666");
		headers.put(HttpHeaders.WARNING, "Yikes!");
		when(clientRequest.getHeaderNames()).thenReturn(headers.keys());
		when(clientRequest.getHeaders(eq("content-length"))).thenReturn(headers.elements());
		when(clientRequest.getHeaders(eq(HttpHeaders.WARNING))).thenReturn(headers.elements());
		serverRequest = new HttpGet("http://this.org:80/wow/dude");
		ProxyUtil.generateServerRequestHeaders(clientRequest, serverRequest);
		assertTrue(serverRequest.containsHeader(HttpHeaders.WARNING));

		Header[] warnings = serverRequest.getHeaders(HttpHeaders.WARNING);
		assertEquals(2, warnings.length);
		assertEquals("Yikes!", warnings[0].getValue());
		assertEquals("666", warnings[1].getValue());

		assertFalse(serverRequest.containsHeader("content-length"));
		assertFalse(serverRequest.containsHeader(HttpHeaders.CONTENT_LENGTH));

		assertTrue(serverRequest.containsHeader(HttpHeaders.HOST));
		Header[] hosts = serverRequest.getHeaders(HttpHeaders.HOST);
		assertEquals(1, hosts.length);
		assertEquals("this.org:80", hosts[0].getValue());
	}

	@Test
	public void getCaseSensitiveParameterTest() {
		String parm = ProxyUtil.getCaseSensitiveParameter(null, null);
		assertNull(parm);

		parm = ProxyUtil.getCaseSensitiveParameter("orig", null);
		assertEquals("orig", parm);

		parm = ProxyUtil.getCaseSensitiveParameter(null, new HashSet<String>(Arrays.asList("wayWrong", "OrIg", "nope")));
		assertNull(parm);

		parm = ProxyUtil.getCaseSensitiveParameter("wow", new HashSet<String>(Arrays.asList("wayWrong", "OrIg", "nope")));
		assertEquals("wow", parm);

		parm = ProxyUtil.getCaseSensitiveParameter("orig", new HashSet<String>(Arrays.asList("wayWrong", "OrIg", "nope")));
		assertEquals("OrIg", parm);
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

	@Test
	public void getServerRequestURIAsStringTest() {
		when(clientRequest.getServletPath()).thenReturn("/goHere");

		Map<String, String> ogcParams = new HashMap<>();
		ogcParams.put(WMSParameters.layer.toString(), "j@ck:e");
		ogcParams.put("ReQuEsT", OGCRequest.GET_LEGEND_GRAPHIC);

		assertEquals("http://bbggr.org/proxy/goHere?ReQuEsT=GetLegendGraphic&layer=j%40ck%3Ae",
				ProxyUtil.getServerRequestURIAsString(clientRequest, ogcParams, "http://bbggr.org/proxy"));
	}

	@Test
	public void redirectContentToProxyTest() {
		assertEquals("", ProxyUtil.redirectContentToProxy(null, null, null, null, null, null, null, null, null));
		assertEquals("", ProxyUtil.redirectContentToProxy("abc", null, null, null, null, null, null, null, null));
		assertEquals("ywxz", ProxyUtil.redirectContentToProxy("abcd", "d", "z", "a", "y", "c", "x", "b", "w"));
		assertEquals("", ProxyUtil.redirectContentToProxy("abcd", null, "z", null, "y", null, "x", null, "w"));
		assertEquals("wxwywxwzwxwywxwawxwywxwzwxwywxwbwxwywxwzwxwywxwcwxwywxwzwxwywxwdwxwywxwzwxwywxw",
				ProxyUtil.redirectContentToProxy("abcd", "", "z", "", "y", "", "x", "", "w"));
		assertEquals("", ProxyUtil.redirectContentToProxy("abcd", "d", null, "a", null, "c", null, "b", null));
		assertEquals("", ProxyUtil.redirectContentToProxy("abcd", "d", "", "a", "", "c", "", "b", ""));
	}

	@Test
	public void separateParametersGetTest() {
		Map<String, String[]> requestParams = new HashMap<>();
		OGCRequest ogcRequest = ProxyUtil.separateParameters(clientRequest, OGCServices.WFS);
		assertEquals(OGCServices.WFS, ogcRequest.getOgcService());

		requestParams.put(WMSParameters.layer.toString(), new String[]{ProxyDataSourceParameter.WQP_SITES.toString()});
		requestParams.put("ReQuEsT", new String[]{OGCRequest.GET_LEGEND_GRAPHIC});
		requestParams.put("sErViCe", new String[]{OGCServices.WMS.toString()});
		when(clientRequest.getParameterMap()).thenReturn(requestParams);
		ogcRequest = ProxyUtil.separateParameters(clientRequest, OGCServices.WFS);
		assertEquals(OGCServices.WMS, ogcRequest.getOgcService());

		assertEquals(3, ogcRequest.getOgcParams().size());
		assertEquals(ProxyDataSourceParameter.WQP_SITES.toString(), ogcRequest.getOgcParams().get(WMSParameters.layer.toString()));
		assertEquals(OGCRequest.GET_LEGEND_GRAPHIC, ogcRequest.getOgcParams().get("ReQuEsT"));
		assertEquals(OGCServices.WMS.toString(), ogcRequest.getOgcParams().get("sErViCe"));

		assertTrue(ogcRequest.getSearchParams().isEmpty());
		assertTrue(ogcRequest.getRequestBody().isEmpty());


		requestParams.put(WQPParameters.searchParams.toString(), null);
		ogcRequest = ProxyUtil.separateParameters(clientRequest, OGCServices.WFS);
		assertEquals(OGCServices.WMS, ogcRequest.getOgcService());

		assertEquals(3, ogcRequest.getOgcParams().size());
		assertEquals(ProxyDataSourceParameter.WQP_SITES.toString(), ogcRequest.getOgcParams().get(WMSParameters.layer.toString()));
		assertEquals(OGCRequest.GET_LEGEND_GRAPHIC, ogcRequest.getOgcParams().get("ReQuEsT"));
		assertEquals(OGCServices.WMS.toString(), ogcRequest.getOgcParams().get("sErViCe"));

		assertTrue(ogcRequest.getSearchParams().isEmpty());
		assertTrue(ogcRequest.getRequestBody().isEmpty());


		requestParams.put(WQPParameters.searchParams.toString(), new String[]{"huc:06*|07*;sampleMedia:Water;characteristicType:Nutrient"});
		ogcRequest = ProxyUtil.separateParameters(clientRequest, OGCServices.WFS);
		assertEquals(OGCServices.WMS, ogcRequest.getOgcService());

		assertEquals(3, ogcRequest.getOgcParams().size());
		assertEquals(ProxyDataSourceParameter.WQP_SITES.toString(), ogcRequest.getOgcParams().get(WMSParameters.layer.toString()));
		assertEquals(OGCRequest.GET_LEGEND_GRAPHIC, ogcRequest.getOgcParams().get("ReQuEsT"));
		assertEquals(OGCServices.WMS.toString(), ogcRequest.getOgcParams().get("sErViCe"));

		assertEquals(3, ogcRequest.getSearchParams().size());
		assertEquals(Arrays.asList("06*", "07*"), ogcRequest.getSearchParams().get("huc"));
		assertEquals(Arrays.asList("Water"), ogcRequest.getSearchParams().get("sampleMedia"));
		assertEquals(Arrays.asList("Nutrient"), ogcRequest.getSearchParams().get("characteristicType"));

		assertTrue(ogcRequest.getRequestBody().isEmpty());
	}

	@Test
	public void separateParametersPostTest() throws IOException {
		DelegatingServletInputStream a = new DelegatingServletInputStream(
				new ByteArrayInputStream(OgcParserTest.wfs_vendorParams.getBytes()));
		Map<String, String[]> requestParams = new HashMap<>();
		when(clientRequest.getMethod()).thenReturn(HttpPost.METHOD_NAME);
		when(clientRequest.getParameterMap()).thenReturn(requestParams);
		when(clientRequest.getInputStream()).thenReturn(a);

		OGCRequest ogcRequest = ProxyUtil.separateParameters(clientRequest, OGCServices.WFS);
		assertEquals(OGCServices.WFS, ogcRequest.getOgcService());

		assertEquals(4, ogcRequest.getOgcParams().size());
		assertEquals(ProxyDataSourceParameter.WQP_SITES.toString(), ogcRequest.getOgcParams().get(WFSParameters.typeName.toString()));
		assertFalse(requestParams.containsKey(WFSParameters.typeNames.toString()));
		assertEquals("GetFeature", ogcRequest.getOgcParams().get("request"));
		assertEquals(OGCServices.WFS.toString(), ogcRequest.getOgcParams().get("service"));
		assertEquals("1.1.0", ogcRequest.getOgcParams().get("version"));

		assertEquals(3, ogcRequest.getSearchParams().size());
		assertEquals(Arrays.asList("US"), ogcRequest.getSearchParams().get("countrycode"));
		assertEquals(Arrays.asList("US:55", "US:28", "US:32"), ogcRequest.getSearchParams().get("statecode"));
		assertEquals(Arrays.asList("Atrazine"), ogcRequest.getSearchParams().get("characteristicName"));

		assertEquals(OgcParserTest.wfs_minus_vendorParams, ogcRequest.getRequestBody());
	}

}
