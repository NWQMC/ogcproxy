package gov.usgs.wqp.ogcproxy.controllers;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.ModelAndView;

import gov.usgs.wqp.ogcproxy.model.OGCRequest;
import gov.usgs.wqp.ogcproxy.model.cache.DynamicLayerCache;
import gov.usgs.wqp.ogcproxy.model.ogc.services.OGCServices;
import gov.usgs.wqp.ogcproxy.services.ProxyService;
import gov.usgs.wqp.ogcproxy.services.RESTService;

public class OGCProxyControllerTest {

	private MockMvc mockMvc;

	@Mock
	private ProxyService proxyService;
	
	@Mock
	private RESTService restService;
	
	private OGCProxyController mvcService;

    @Before
    public void setup() {
    	MockitoAnnotations.initMocks(this);
    	mvcService = new OGCProxyController(proxyService, restService);
    	mockMvc = MockMvcBuilders.standaloneSetup(mvcService).build();
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
		assertEquals("Application Version:  Error Encountered", rtn.getModelAndView().getModelMap().get("version"));
	}

	@Test
	public void restCacheStatusTest() throws Exception {
		Map<String, DynamicLayerCache> cache = new HashMap<>();
		cache.put("abc", new DynamicLayerCache(new OGCRequest(OGCServices.WMS), "abcWorkspace"));
		when(restService.checkCacheStatus(anyString())).thenReturn(getBadCacheStatus(), getOkCacheStatus(cache));

		MvcResult mvcResult = mockMvc.perform(get("/rest/cachestatus/wqp_sites"))
				.andExpect(status().isOk())
				.andExpect(request().asyncStarted())
				.andExpect(request().asyncResult(instanceOf(ModelAndView.class)))
				.andReturn();

		this.mockMvc.perform(asyncDispatch(mvcResult))
				.andExpect(status().isOk())
				.andExpect(forwardedUrl("invalid_site.jsp"))
				.andExpect(model().attributeExists("site"))
				.andExpect(model().attribute("site", "BadSite"));

		

		mvcResult = mockMvc.perform(get("/rest/cachestatus/wqp_sites"))
				.andExpect(status().isOk())
				.andExpect(request().asyncStarted())
				.andExpect(request().asyncResult(instanceOf(ModelAndView.class)))
				.andReturn();

		this.mockMvc.perform(asyncDispatch(mvcResult))
				.andExpect(status().isOk())
				.andExpect(forwardedUrl("wqp_cache_status.jsp"))
				.andExpect(model().attributeExists("site"))
				.andExpect(model().attribute("site", "WQP Layer Building Service"))
				.andExpect(model().attributeExists("cache"))
				.andExpect(model().attribute("cache", cache.values()));
	}

	@Test
	public void restClearCacheTest() throws Exception {
		Map<String, DynamicLayerCache> cache = new HashMap<>();
		cache.put("abc", new DynamicLayerCache(new OGCRequest(OGCServices.WMS), "abcWorkspace"));
		when(restService.clearCacheBySite(anyString())).thenReturn(getBadClear(), getOkClear());

		MvcResult mvcResult = mockMvc.perform(delete("/rest/clearcache/no_sites_here"))
				.andExpect(status().isOk())
				.andExpect(request().asyncStarted())
				.andExpect(request().asyncResult(instanceOf(ModelAndView.class)))
				.andReturn();

		this.mockMvc.perform(asyncDispatch(mvcResult))
				.andExpect(status().isOk())
				.andExpect(forwardedUrl("invalid_site.jsp"))
				.andExpect(model().attributeExists("site"))
				.andExpect(model().attribute("site", "no_sites_here"));

		

		mvcResult = mockMvc.perform(delete("/rest/clearcache/wqp_sites"))
				.andExpect(status().isOk())
				.andExpect(request().asyncStarted())
				.andExpect(request().asyncResult(instanceOf(ModelAndView.class)))
				.andReturn();

		this.mockMvc.perform(asyncDispatch(mvcResult))
				.andExpect(status().isOk())
				.andExpect(forwardedUrl("wqp_cache_cleared.jsp"))
				.andExpect(model().attributeExists("site"))
				.andExpect(model().attribute("site", "WQP Layer Building Service"))
				.andExpect(model().attributeExists("count"))
				.andExpect(model().attribute("count", 5));
	}

	protected ModelAndView getOkCacheStatus(Map<String, DynamicLayerCache> cache) {
		ModelAndView mv = new ModelAndView("wqp_cache_status.jsp");
		mv.addObject("site", "WQP Layer Building Service");
		mv.addObject("cache", cache.values());
		return mv;
	}

	protected ModelAndView getBadCacheStatus() {
		ModelAndView mv = new ModelAndView("invalid_site.jsp");
		mv.addObject("site", "BadSite");
		return mv;
	}

	protected ModelAndView getOkClear() {
		ModelAndView mv = new ModelAndView("wqp_cache_cleared.jsp");
		mv.addObject("site", "WQP Layer Building Service");
		mv.addObject("count", 5);
		return mv;
	}

	protected ModelAndView getBadClear() {
		ModelAndView mv = new ModelAndView("invalid_site.jsp");
		mv.addObject("site", "no_sites_here");
		return mv;
	}
}
