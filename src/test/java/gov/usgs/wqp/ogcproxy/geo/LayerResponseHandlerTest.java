package gov.usgs.wqp.ogcproxy.geo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
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
import org.junit.Test;
import org.mockito.Mockito;

import gov.usgs.wqp.ogcproxy.geo.LayerResponse.Layers.Layer;
import gov.usgs.wqp.ogcproxy.model.cache.DynamicLayerCache;

public class LayerResponseHandlerTest {

	public final static String ONE_KEY = "2943210710";
	public final static String ONE_NAME = DynamicLayerCache.DYNAMIC_LAYER_PREFIX + ONE_KEY;
	public final static String ONE_HREF = "http://cida-eros-wqpgsdev.er.usgs.gov:8080/geoserver/rest/layers/" + ONE_NAME + ".json";
	public final static String TWO_KEY = "3121669545";
	public final static String TWO_NAME = DynamicLayerCache.DYNAMIC_LAYER_PREFIX + TWO_KEY;
	public final static String TWO_HREF = "http://cida-eros-wqpgsdev.er.usgs.gov:8080/geoserver/rest/layers/" + TWO_NAME + ".json";
	public final static String THREE_KEY = "3671118748";
	public final static String THREE_NAME = DynamicLayerCache.DYNAMIC_LAYER_PREFIX + THREE_KEY;
	public final static String THREE_HREF = "http://cida-eros-wqpgsdev.er.usgs.gov:8080/geoserver/rest/layers/" + THREE_NAME + ".json";

	public final static String TEST_LAYER_RESPONSE = "{\"layers\": {\"layer\": [{\"name\": \""
			+ ONE_NAME + "\",\"href\": \"" + ONE_HREF + "\"}, {\"name\": \""
			+ TWO_NAME + "\",\"href\": \"" + TWO_HREF + "\"}, {\"name\": \""
			+ THREE_NAME + "\",\"href\": \"" + THREE_HREF + "\"}]}}";
	
	@Test
	public void handleResponseHappyTest() throws UnsupportedOperationException, IOException {
		HttpResponse response = Mockito.mock(HttpResponse.class);
		StatusLine statusLine = Mockito.mock(StatusLine.class);
		HttpEntity httpEntity = Mockito.mock(HttpEntity.class);
		LayerResponseHandler lrh = new LayerResponseHandler();
		when(response.getStatusLine()).thenReturn(statusLine);
		when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_OK);
		when(response.getEntity()).thenReturn(httpEntity);
		when(httpEntity.getContent()).thenReturn(new ByteArrayInputStream(TEST_LAYER_RESPONSE.getBytes()));
		
		try {
			LayerResponse lr = lrh.handleResponse(response);
		
			assertNotNull(lr);
			assertNotNull(lr.getLayers());
			assertEquals(3, lr.getLayers().getLayer().size());
			
			Layer one = lr.getLayers().getLayer().get(0);
			assertEquals(ONE_NAME, one.getName());
			assertEquals(ONE_HREF, one.getHref());
			
			Layer two = lr.getLayers().getLayer().get(1);
			assertEquals(TWO_NAME, two.getName());
			assertEquals(TWO_HREF, two.getHref());
			
			Layer three = lr.getLayers().getLayer().get(2);
			assertEquals(THREE_NAME, three.getName());
			assertEquals(THREE_HREF, three.getHref());
		} catch (Exception e) {
			fail(e.getLocalizedMessage());
		}

	}

	@Test
	public void handleResponseSadTest() throws UnsupportedOperationException, IOException {
		HttpResponse response = Mockito.mock(HttpResponse.class);
		StatusLine statusLine = Mockito.mock(StatusLine.class);
		LayerResponseHandler lrh = new LayerResponseHandler();
		when(response.getStatusLine()).thenReturn(statusLine);
		when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_BAD_REQUEST, HttpStatus.SC_OK);
		when(response.getEntity()).thenReturn(null);
		
		try {
			//Bad HttpStatus from "Geoserver"
			lrh.handleResponse(response);
		} catch (Exception e) {
			assertTrue(e instanceof HttpResponseException);
		}
		
		try {
			//Empty response from "Geoserver"
			assertNull(lrh.handleResponse(response));
		} catch (Exception e) {
			fail(e.getLocalizedMessage());
		}
		
	}

}
