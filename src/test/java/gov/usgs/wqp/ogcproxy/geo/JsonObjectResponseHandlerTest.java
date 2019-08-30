package gov.usgs.wqp.ogcproxy.geo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpResponseException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.google.gson.JsonObject;

import gov.usgs.wqp.ogcproxy.model.DynamicLayer;

public class JsonObjectResponseHandlerTest {

	public final static String ONE_KEY = "2943210710";
	public final static String ONE_NAME = DynamicLayer.DYNAMIC_LAYER_PREFIX + ONE_KEY;
	public final static String ONE_HREF = "http://cida-eros-wqpgsdev.er.usgs.gov:8080/geoserver/rest/layers/" + ONE_NAME + ".json";
	public final static String TWO_KEY = "3121669545";
	public final static String TWO_NAME = DynamicLayer.DYNAMIC_LAYER_PREFIX + TWO_KEY;
	public final static String TWO_HREF = "http://cida-eros-wqpgsdev.er.usgs.gov:8080/geoserver/rest/layers/" + TWO_NAME + ".json";
	public final static String THREE_KEY = "3671118748";
	public final static String THREE_NAME = DynamicLayer.DYNAMIC_LAYER_PREFIX + THREE_KEY;
	public final static String THREE_HREF = "http://cida-eros-wqpgsdev.er.usgs.gov:8080/geoserver/rest/layers/" + THREE_NAME + ".json";

	public final static String TEST_LAYER_RESPONSE = "{\"layers\": {\"layer\": [{\"name\": \""
			+ ONE_NAME + "\",\"href\": \"" + ONE_HREF + "\"}, {\"name\": \""
			+ TWO_NAME + "\",\"href\": \"" + TWO_HREF + "\"}, {\"name\": \""
			+ THREE_NAME + "\",\"href\": \"" + THREE_HREF + "\"}]}}";

	@Mock
	private HttpResponse response;
	@Mock
	private StatusLine statusLine;
	@Mock
	private HttpEntity httpEntity;

	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
	}

	@Test
	public void handleResponseHappyTest() throws UnsupportedOperationException, IOException {
		JsonObjectResponseHandler responseHandler = new JsonObjectResponseHandler();
		when(response.getStatusLine()).thenReturn(statusLine);
		when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_OK);
		when(response.getEntity()).thenReturn(httpEntity);
		when(httpEntity.getContent()).thenReturn(new ByteArrayInputStream(TEST_LAYER_RESPONSE.getBytes()));
		
		try {
			JsonObject jsonObject = responseHandler.handleResponse(response);
		
			assertNotNull(jsonObject);
			assertNotNull(jsonObject.getAsJsonObject("layers"));
			assertEquals(3, jsonObject.getAsJsonObject("layers").getAsJsonArray("layer").size());
			
			JsonObject one = jsonObject.getAsJsonObject("layers").getAsJsonArray("layer").get(0).getAsJsonObject();
			assertEquals(ONE_NAME, one.get("name").getAsString());
			assertEquals(ONE_HREF, one.get("href").getAsString());
			
			JsonObject two = jsonObject.getAsJsonObject("layers").getAsJsonArray("layer").get(1).getAsJsonObject();
			assertEquals(TWO_NAME, two.get("name").getAsString());
			assertEquals(TWO_HREF, two.get("href").getAsString());
			
			JsonObject three = jsonObject.getAsJsonObject("layers").getAsJsonArray("layer").get(2).getAsJsonObject();
			assertEquals(THREE_NAME, three.get("name").getAsString());
			assertEquals(THREE_HREF, three.get("href").getAsString());
		} catch (Exception e) {
			fail(e.getLocalizedMessage());
		}

	}

	@Test
	public void handleResponseSadTest() throws UnsupportedOperationException, IOException {
		JsonObjectResponseHandler responseHandler = new JsonObjectResponseHandler();
		when(response.getStatusLine()).thenReturn(statusLine);
		when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_BAD_REQUEST, HttpStatus.SC_OK);
		when(response.getEntity()).thenReturn(null);
		
		try {
			//Bad HttpStatus from "Geoserver"
			responseHandler.handleResponse(response);
		} catch (Exception e) {
			assertTrue(e instanceof HttpResponseException);
		}
		
		try {
			//Empty response from "Geoserver"
			responseHandler.handleResponse(response);
		} catch (Exception e) {
			assertTrue(e instanceof HttpResponseException);
		}
		
	}

}
