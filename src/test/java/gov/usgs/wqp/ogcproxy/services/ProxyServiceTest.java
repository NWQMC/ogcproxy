package gov.usgs.wqp.ogcproxy.services;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anySetOf;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
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
import org.powermock.reflect.Whitebox;
import org.springframework.core.env.Environment;

import gov.usgs.wqp.ogcproxy.model.OGCRequest;
import gov.usgs.wqp.ogcproxy.model.ogc.parameters.WMSParameters;
import gov.usgs.wqp.ogcproxy.model.ogc.services.OGCServices;
import gov.usgs.wqp.ogcproxy.model.parameters.ProxyDataSourceParameter;
import gov.usgs.wqp.ogcproxy.model.parameters.SearchParameters;
import gov.usgs.wqp.ogcproxy.services.wqp.WQPDynamicLayerCachingService;
import gov.usgs.wqp.ogcproxy.utils.ProxyUtil;

@RunWith(PowerMockRunner.class)
@PrepareForTest(ProxyUtil.class)
@PowerMockIgnore("org.apache.http.conn.ssl.*")
public class ProxyServiceTest {
	
//	@Mock
//	private CloseableHttpClient httpClient;
//	@Mock
//	private HttpClientBuilder httpClientBuilder;
	@Mock
	private HttpServletRequest request;
	@Mock
	private Environment environment;
	@Mock
	private CloseableHttpResponse response;
	@Mock
	private StatusLine statusLine;
	@Mock
	WQPDynamicLayerCachingService cachingService;
	@Spy
	ProxyService service = ProxyService.getInstance();

	@Before
	public void beforeTest() {
		PowerMockito.mockStatic(ProxyUtil.class);
		MockitoAnnotations.initMocks(this);
//		when(HttpClients.custom()).thenReturn(httpClientBuilder);
//		when(httpClientBuilder.build()).thenReturn(httpClient);
		
		//reset those potentially mucked up in any tests - (fun with statics - not)
		Whitebox.setInternalState(ProxyService.class, "geoserverProtocol", "http");
		Whitebox.setInternalState(ProxyService.class, "geoserverHost", "localhost");
		Whitebox.setInternalState(ProxyService.class, "geoserverPort", "8080");
		Whitebox.setInternalState(ProxyService.class, "geoserverContext", "geoserver");
		Whitebox.setInternalState(ProxyService.class, "forwardUrl", "");
		
		service.setWQPDynamicLayerCachingService(cachingService);
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
	public void initializeTest() {
		when(environment.getProperty("wqp.geoserver.proto")).thenReturn("","https");
		when(environment.getProperty("wqp.geoserver.host")).thenReturn("","somewhere");
		when(environment.getProperty("wqp.geoserver.port")).thenReturn("","8443");
		when(environment.getProperty("wqp.geoserver.context")).thenReturn("","mycontext");

		service.setEnvironment(environment);
		service.initialize();
		
		//defaults since all are empty string
		assertEnvironmentDefaults();
		
		//still defaults because already initialized...
		service.initialize();
		assertEnvironmentDefaults();
		
		//start over and apply some.
		Whitebox.setInternalState(ProxyService.class, "initialized", false);
		service.initialize();
		assertEquals("https", Whitebox.getInternalState(ProxyService.class, "geoserverProtocol"));
		assertEquals("somewhere", Whitebox.getInternalState(ProxyService.class, "geoserverHost"));
		assertEquals("8443", Whitebox.getInternalState(ProxyService.class, "geoserverPort"));
		assertEquals("mycontext", Whitebox.getInternalState(ProxyService.class, "geoserverContext"));
		assertEquals("https://somewhere:8443", Whitebox.getInternalState(ProxyService.class, "forwardUrl"));
	}

	@Test
	public void inspectServerContentTest() {
		//TODO
	}

	@Test
	public void performRequestTest() {
		//TODO
	}

	@Test
	public void proxyRequestTest() {
		//TODO
	}

	private void assertEnvironmentDefaults() {
		assertEquals("http", Whitebox.getInternalState(ProxyService.class, "geoserverProtocol"));
		assertEquals("localhost", Whitebox.getInternalState(ProxyService.class, "geoserverHost"));
		assertEquals("8080", Whitebox.getInternalState(ProxyService.class, "geoserverPort"));
		assertEquals("geoserver", Whitebox.getInternalState(ProxyService.class, "geoserverContext"));
		assertEquals("http://localhost:8080", Whitebox.getInternalState(ProxyService.class, "forwardUrl"));
	}

}
