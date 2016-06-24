package gov.usgs.wqp.ogcproxy.services;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anySetOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import gov.usgs.wqp.ogcproxy.exceptions.OGCProxyException;
import gov.usgs.wqp.ogcproxy.model.OGCRequest;
import gov.usgs.wqp.ogcproxy.model.ogc.parameters.WMSParameters;
import gov.usgs.wqp.ogcproxy.model.ogc.services.OGCServices;
import gov.usgs.wqp.ogcproxy.model.parameters.ProxyDataSourceParameter;
import gov.usgs.wqp.ogcproxy.model.parameters.SearchParameters;
import gov.usgs.wqp.ogcproxy.services.wqp.WQPDynamicLayerCachingService;
import gov.usgs.wqp.ogcproxy.utils.CloseableHttpClientFactory;
import gov.usgs.wqp.ogcproxy.utils.ProxyUtil;
import gov.usgs.wqp.ogcproxy.utils.SystemUtils;

@RunWith(PowerMockRunner.class)
@PrepareForTest(ProxyUtil.class)
@PowerMockIgnore("org.apache.http.conn.ssl.*")
public class ProxyServiceTest {

	@Mock
	private HttpServletRequest request;
	@Mock
	private HttpUriRequest serverRequest;
	@Mock
	private CloseableHttpResponse response;
	@Mock
	private StatusLine statusLine;
	@Mock
	WQPDynamicLayerCachingService cachingService;
	@Mock
	CloseableHttpClientFactory clientFactory;
	@Spy
	ProxyService service = ProxyService.getInstance();

	@Before
	public void beforeTest() {
		PowerMockito.mockStatic(ProxyUtil.class);
		MockitoAnnotations.initMocks(this);

		service.layerCachingService = cachingService;
		service.closeableHttpClientFactory = clientFactory;
	}

	@Test
	public void addGetCapabilitiesInfoTest() {
		//TODO
	}

	@Test
	public void convertVendorParmsTest() {
		when(ProxyUtil.getCaseSensitiveParameter(eq("request"), anySetOf(String.class))).thenReturn("ReQuEsT");
		when(ProxyUtil.getCaseSensitiveParameter(eq("layer"), anySetOf(String.class))).thenReturn(WMSParameters.layer.toString());
		Map<String, String> ogcParams = new HashMap<>();
		ogcParams.put(WMSParameters.layer.toString(), ProxyDataSourceParameter.WQP_SITES.toString());
		ogcParams.put("ReQuEsT", OGCRequest.GET_LEGEND_GRAPHIC);
		SearchParameters<String, List<String>> searchParams = new SearchParameters<>();
		searchParams.put("wqp", Arrays.asList("nope", "nada"));
		OGCRequest notVendor = new OGCRequest(null);
		OGCRequest vendor = new OGCRequest(OGCServices.WMS, ogcParams, searchParams, null);

		when(ProxyUtil.separateParameters(any(HttpServletRequest.class), any(OGCServices.class)))
				.thenReturn(notVendor, vendor);
		when(cachingService.getDynamicLayer(any(OGCRequest.class))).thenReturn("thisIsALayer");

		OGCRequest ogcRequest = service.convertVendorParms(request, OGCServices.WMS);
		assertEquals(notVendor, ogcRequest);
		verify(cachingService, never()).getDynamicLayer(any(OGCRequest.class));
		verifyStatic();
		ProxyUtil.separateParameters(any(HttpServletRequest.class), any(OGCServices.class));

		ogcRequest = service.convertVendorParms(request, OGCServices.WMS);
		assertEquals(vendor, ogcRequest);
		assertEquals("thisIsALayer", ogcRequest.getOgcParams().get(WMSParameters.layer.toString()));
		verify(cachingService).getDynamicLayer(any(OGCRequest.class));
		verifyStatic(times(2));
		ProxyUtil.separateParameters(any(HttpServletRequest.class), any(OGCServices.class));
	}

	@Test
	public void generateServerRequestTest() {
		//TODO
	}

	@Test
	public void handleServerRequestTest() {
		//TODO
	}

	@Test
	public void handleServerResponseTest() {
		//TODO
	}

	@Test
	public void inspectServerContentTest() throws OGCProxyException, URISyntaxException {
		when(serverRequest.getURI()).thenReturn(new URI("https://owi.usgs.gov:8443/wow"));
		when(request.getServerName()).thenReturn("gp.org");
		when(request.getContextPath()).thenReturn("ac");
		when(request.getLocalPort()).thenReturn(8080);
		when(request.getScheme()).thenReturn("http");
		when(ProxyUtil.redirectContentToProxy(anyString(), anyString(),anyString(),anyString(),anyString(),
				anyString(),anyString(),anyString(),anyString())).thenReturn("ReQuEsT");
		when(ProxyUtil.getCaseSensitiveParameter(anyString(), anySetOf(String.class)))
			.thenReturn("request", WMSParameters.layers.toString(), null);
		OGCRequest ogcRequest = new OGCRequest(OGCServices.WMS, new HashMap<>(), new SearchParameters<>(), null);
		byte[] serverContent = "https://owi.usgs.gov:8443/wow?cool=beans".getBytes();
		byte[] sillyServerContent = "silly response".getBytes();

		//verify that correct values are calculated to send to ProxyUtil.redirectContentToProxy
		byte[] abc = service.inspectServerContent(request, serverRequest, ogcRequest, serverContent, false);
		assertEquals("ReQuEsT", new String(abc));

		verifyStatic();
		ProxyUtil.redirectContentToProxy(eq("https://owi.usgs.gov:8443/wow?cool=beans"),
				eq("https"), eq("http"),
				eq("owi.usgs.gov"), eq("gp.org"),
				eq("8443"), eq("8080"),
				eq("wow"), eq("ac"));

		//verify that nothing is changed if the host does not appear in the content
		assertEquals("silly response", 
				new String(service.inspectServerContent(request, serverRequest, ogcRequest, sillyServerContent, false)));

		//verify that the message is able to be compressed/decompressed
		assertEquals("compressed String", 
				SystemUtils.uncompressGzipAsString(service.inspectServerContent(request, serverRequest, ogcRequest,
						SystemUtils.compressStringToGzip("compressed String"), true)));

		//verify that addGetCapabilitesInfo is called
		Map<String, String> ogcParams = new HashMap<>();
		ogcParams.put(WMSParameters.layers.toString(), ProxyDataSourceParameter.WQP_SITES.toString());
		ogcParams.put("request", ProxyUtil.OGC_GET_CAPABILITIES);
		ogcRequest = new OGCRequest(OGCServices.WMS, ogcParams, new SearchParameters<>(), null);
		abc = service.inspectServerContent(request, serverRequest, ogcRequest, "</Layer>".getBytes(), false);
		assertEquals(ProxyService.WMS_GET_CAPABILITIES_CONTENT + "</Layer>", new String(abc));
	}

	@Test
	public void performRequestTest() {
		//TODO
	}

	@Test
	public void proxyRequestTest() {
		//TODO
	}

}
