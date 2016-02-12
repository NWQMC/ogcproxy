package gov.usgs.wqp.ogcproxy.utils;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

public class WQPUtilsTest {
	String requestParamString ="filter1=value1a%3Avalue1b%3Bvalue2%3Bvalue3&filter2=value1a%3Avalue1b%3Bvalue2a%3Avalue2b&filter3=value1&filter3=value2&filter3=value3&mimeType=geojson";
	String searchParamString = "filter1:value1a:value1b|value2|value3;filter2:value1a:value1b|value2a:value2b;filter3:value1;filter3:value2;filter3:value3";
	Map<String, List<String>> searchParams;
	
	@Before
	public void init() throws Exception {
		searchParams = WQPUtils.parseSearchParams(searchParamString);
	}
	
	
	@Test
	public void testParseSearchParams_paramsSize() {
		assertEquals(3, searchParams.size());
	}
	@Test
	public void testParseSearchParams_paramSize() {
		List<String> filter1 = searchParams.get("filter1");
		assertEquals(3, filter1.size());
	}
	@Test
	public void testParseSearchParams_paramParse() {
		List<String> filter1 = searchParams.get("filter1");
		assertEquals("value1a:value1b", filter1.get(0));
		assertEquals("value2", filter1.get(1));
		assertEquals("value3", filter1.get(2));
	}
	@Test
	public void testParseSearchParams_anotherParamParse() {
		List<String> filter2 = searchParams.get("filter2");
		assertEquals(2, filter2.size());
		
		assertEquals("value1a:value1b", filter2.get(0));
		assertEquals("value2a:value2b", filter2.get(1));
	}
		
	// this test proves demonstrates that the providers param can be parsed
	// it is also part of the HTML query string spec to accept a list or multiple entries of the same name
	@Test
	public void testParseSearchParams_mutlipleEntryParamParseJoin() {
		List<String> filter3 = searchParams.get("filter3");
		assertEquals(3, filter3.size());
		
		assertEquals("value1", filter3.get(0));
		assertEquals("value2", filter3.get(1));
		assertEquals("value3", filter3.get(2));
	}
	
	@Test
	public void testGenerateSimpleStationRequest() throws Exception {
		List<String> multiparams = new ArrayList<String>();
		multiparams.add("filter3");
		String url = WQPUtils.generateSimpleStationRequest(searchParams, "http://prefix", multiparams).getURI().toString();
		assertEquals("http://prefix?"+requestParamString, url);
	}
	
}
