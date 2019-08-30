package gov.usgs.wqp.ogcproxy.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.co.datumedge.hamcrest.json.SameJSONAs.sameJSONObjectAs;

import gov.usgs.wqp.ogcproxy.model.DynamicLayer;
import gov.usgs.wqp.ogcproxy.model.OGCRequest;
import gov.usgs.wqp.ogcproxy.model.ogc.services.OGCServices;
import gov.usgs.wqp.ogcproxy.services.ConfigurationService;
import gov.usgs.wqp.ogcproxy.services.ProxyService;
import gov.usgs.wqp.ogcproxy.services.RESTService;
import java.io.IOException;
import java.util.Date;
import org.json.JSONObject;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.FileCopyUtils;

public class OGCProxyControllerTest {

	private MockMvc mockMvc;

	@Mock
	private ProxyService proxyService;

	@Mock
	private RESTService restService;
	private long timeInMilli;

	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
		ConfigurationService configurationService = new ConfigurationService();
		Whitebox.setInternalState(configurationService, "readLockTimeout", Long.valueOf("10"));
		Whitebox.setInternalState(configurationService, "writeLockTimeout", Long.valueOf("10"));
		OGCProxyController mvcService = new OGCProxyController(proxyService, restService, configurationService);
		mockMvc = MockMvcBuilders.standaloneSetup(mvcService).build();
		timeInMilli = new Date().getTime();
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
	public void restCacheStatusTest() throws Exception {
		Map<String, DynamicLayer> cache = new HashMap<>();
		cache.put("abc", new DynamicLayer(new OGCRequest(OGCServices.WMS), "abcWorkspace"));
		when(restService.checkCacheStatus(anyString())).thenReturn(getBadCacheStatus(), getOkCacheStatus(cache));
		MvcResult rtn = mockMvc.perform(get("/rest/cachestatus/wqp_sites"))
				.andExpect(status().isOk())
				.andReturn();
		
		JSONObject actual = new JSONObject(rtn.getResponse().getContentAsString());
		JSONObject expected = new JSONObject(getCompareFile("invalid_site.json"));
		assertThat(actual, sameJSONObjectAs(expected));
		
		rtn = mockMvc.perform(get("/rest/cachestatus/wqp_sites"))
				.andExpect(status().isOk())
				.andReturn();
		
		String actualJsonString = new JSONObject(rtn.getResponse().getContentAsString()).toString();
		String dateString = actualJsonString.replaceAll("[^0-9]","").substring(0,8);
		String dateCorrectedActualJsonString = actualJsonString.replaceAll("\\d{13}", dateString);
		
		actual = new JSONObject(dateCorrectedActualJsonString);
		expected = new JSONObject(getCompareFile("wqp_cache_status.json"));
		assertThat(actual, sameJSONObjectAs(expected));
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
		Map<String, Object> mv = new HashMap<>();
		mv.put("cache", cache.values());
		return mv;
	}

	protected Map<String, Object> getBadCacheStatus() {
		Map<String, Object> mv = new HashMap<>();
		mv.put("site", "BadSite");
		return mv;
	}
	
	/**
	 * Grabs a file and turns it into a String.
	 * @param file
	 * @return A String representation of the input file
	 * @throws IOException 
	 */
	public String getCompareFile(String file) throws IOException {
		String fileForCompareAsString = new String(FileCopyUtils.copyToByteArray(new ClassPathResource("testResult/" + file).getInputStream()));
		fileForCompareAsString = adjustJsonDatesToTimeInMilli(fileForCompareAsString);
		return fileForCompareAsString;
	}
	
	/**
	 * Replaces a String value with a String representation of a time.
	 * @param fileString
	 * @return A String with the "[ACTUAL_TIME]" placeholder replaced with a substring of the current time in epoch milliseconds.
	 */
	public String adjustJsonDatesToTimeInMilli(String fileString) {
		String adjustedDateString = fileString.replace("\"[ACTUAL_TIME]\"", String.valueOf(timeInMilli).substring(0, 8));
		return adjustedDateString;
	}	
}