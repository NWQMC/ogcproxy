package gov.usgs.wqp.ogcproxy.utils;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class WQPUtilsTest {
	@Before
	public void init() throws Exception {
	}
	
	@After
	public void destroy() throws Exception {
		
	}
	
	@Test
	public void testParseSearchParams() {
		String searchParamString = "filter1:value1a:value1b|value2|value3;filter2:value1a:value1b|value2a:value2b|value3;filter3:value1a:value1b|value2a:value2b|value3a:value3b";
		
		Map<String, List<String>> searchParams = new HashMap<String, List<String>>();
		WQPUtils.parseSearchParams(searchParamString, searchParams);
		
		assertEquals(3, searchParams.size(), 0);
		
		List<String> filter1 = searchParams.get("filter1");
		assertEquals(3, filter1.size(), 0);
		
		assertEquals("value1a:value1b", filter1.get(0));
		assertEquals("value2", filter1.get(1));
		assertEquals("value3", filter1.get(2));
		
		List<String> filter2 = searchParams.get("filter2");
		assertEquals(3, filter2.size(), 0);
		
		assertEquals("value1a:value1b", filter2.get(0));
		assertEquals("value2a:value2b", filter2.get(1));
		assertEquals("value3", filter2.get(2));
		
		List<String> filter3 = searchParams.get("filter3");
		assertEquals(3, filter3.size(), 0);
		
		assertEquals("value1a:value1b", filter3.get(0));
		assertEquals("value2a:value2b", filter3.get(1));
		assertEquals("value3a:value3b", filter3.get(2));
	}
}
