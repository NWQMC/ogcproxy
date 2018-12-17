package gov.usgs.wqp.ogcproxy.controllers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.reflect.Whitebox;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.ModelAndView;

import gov.usgs.wqp.ogcproxy.model.DynamicLayer;
import gov.usgs.wqp.ogcproxy.model.OGCRequest;
import gov.usgs.wqp.ogcproxy.model.ogc.services.OGCServices;
import gov.usgs.wqp.ogcproxy.services.ConfigurationService;
import gov.usgs.wqp.ogcproxy.services.ProxyService;
import gov.usgs.wqp.ogcproxy.services.RESTService;

public class OGCProxyControllerTest {

	private MockMvc mockMvc;

	@Mock
	private ProxyService proxyService;

	@Mock
	private RESTService restService;

	private ConfigurationService configurationService;
	private OGCProxyController mvcService;

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
		configurationService = new ConfigurationService();
		Whitebox.setInternalState(configurationService, "readLockTimeout", Long.valueOf("10"));
		Whitebox.setInternalState(configurationService, "writeLockTimeout", Long.valueOf("10"));
		mvcService = new OGCProxyController(proxyService, restService, configurationService);
		mockMvc = MockMvcBuilders.standaloneSetup(mvcService).build();
	}

	@Test
	public void schemasGetTest() throws Exception {
		mockMvc.perform(get("/schemas")).andExpect(status().isOk());

		verify(proxyService).performRequest(any(HttpServletRequest.class), any(HttpServletResponse.class), eq(OGCServices.WMS));
	}

	@Test
	public void owsProxyGetTest() throws Exception {
		mockMvc.perform(get("/ows")).andExpect(status().isOk());

		verify(proxyService).performRequest(any(HttpServletRequest.class), any(HttpServletResponse.class), eq(OGCServices.WMS));
	}

	@Test
	public void wmsProxyGetTest() throws Exception {
		mockMvc.perform(get("/wms")).andExpect(status().isOk());

		verify(proxyService).performRequest(any(HttpServletRequest.class), any(HttpServletResponse.class), eq(OGCServices.WMS));
	}

	@Test
	public void wfsProxyGetTest() throws Exception {
		mockMvc.perform(get("/wfs")).andExpect(status().isOk());

		verify(proxyService).performRequest(any(HttpServletRequest.class), any(HttpServletResponse.class), eq(OGCServices.WFS));
	}

	@Test
	public void wmsProxyPostTest() throws Exception {
		mockMvc.perform(post("/wms")).andExpect(status().isOk());

		verify(proxyService).performRequest(any(HttpServletRequest.class), any(HttpServletResponse.class), eq(OGCServices.WMS));
	}

	@Test
	public void wfsProxyPostTest() throws Exception {
		mockMvc.perform(post("/wfs")).andExpect(status().isOk());

		verify(proxyService).performRequest(any(HttpServletRequest.class), any(HttpServletResponse.class), eq(OGCServices.WFS));
	}

	@Test
	public void entryTest() throws Exception {
		MvcResult rtn = mockMvc.perform(get("/"))
				.andExpect(status().isOk())
				.andReturn();

		assertEquals("index.jsp", rtn.getModelAndView().getViewName());
		assertEquals(1, rtn.getModelAndView().getModelMap().size());
		assertTrue(rtn.getModelAndView().getModelMap().containsKey("version"));
		//We get this error because the test does not run in a Spring context.
		assertEquals("v Error Encountered", rtn.getModelAndView().getModelMap().get("version"));
	}

	@Test
	public void restCacheStatusTest() throws Exception {
		Map<String, DynamicLayer> cache = new HashMap<>();
		cache.put("abc", new DynamicLayer(new OGCRequest(OGCServices.WMS), "abcWorkspace"));
		when(restService.checkCacheStatus(anyString())).thenReturn(getBadCacheStatus(), getOkCacheStatus(cache));

		// just get and compare the json response - don't think about the jsps
		// use https://github.com/NWQMC/qw_portal_services/blob/master/src/test/java/gov/usgs/cida/qw/codes/webservices/BaseCodesRestControllerTest.java as a sample
		mockMvc.perform(get("/rest/cachestatus/wqp_sites"))
				.andExpect(status().isOk())
				//TODO .andExpect(forwardedUrl("invalid_site.jsp"))
				.andExpect(model().attributeExists("site"))
				.andExpect(model().attribute("site", "BadSite"));
		
		// just get and compare the json response - don't think about the jsps
		// use https://github.com/NWQMC/qw_portal_services/blob/master/src/test/java/gov/usgs/cida/qw/codes/webservices/BaseCodesRestControllerTest.java as a sample
		mockMvc.perform(get("/rest/cachestatus/wqp_sites"))
				.andExpect(status().isOk())
				//TODO .andExpect(forwardedUrl("wqp_cache_status.jsp"))
				.andExpect(model().attributeExists("site"))
				.andExpect(model().attribute("site", "WQP Layer Building Service"))
				.andExpect(model().attributeExists("cache"))
				.andExpect(model().attribute("cache", cache.values()));
	}

	@Test
	public void restClearCacheTest() throws Exception {
		Map<String, DynamicLayer> cache = new HashMap<>();
		cache.put("abc", new DynamicLayer(new OGCRequest(OGCServices.WMS), "abcWorkspace"));
		when(restService.clearCacheBySite(anyString())).thenReturn(false, true);

		mockMvc.perform(delete("/rest/clearcache/no_sites_here"))
				.andExpect(status().isBadRequest());

		mockMvc.perform(delete("/rest/clearcache/wqp_sites"))
				.andExpect(status().isOk());
	}

	protected Map<String, Object> getOkCacheStatus(Map<String, DynamicLayer> cache) {
		Map<String, Object> mv = new HashMap<>();//TODO ("wqp_cache_status.jsp");
		mv.put("site", "WQP Layer Building Service");
		mv.put("cache", cache.values());
		return mv;
	}

	protected Map<String, Object> getBadCacheStatus() {
		Map<String, Object> mv = new HashMap<>();//TODO ("invalid_site.jsp");
		mv.put("site", "BadSite");
		return mv;
	}

}
