package gov.usgs.wqp.ogcproxy.services.wqp;

import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;

import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.powermock.api.mockito.PowerMockito;

import gov.usgs.wqp.ogcproxy.exceptions.OGCProxyException;

//@RunWith(PowerMockRunner.class)
//@PrepareForTest(HttpClients.class)
public class WQPLayerBuildingServiceTest {

	@Mock
	private CloseableHttpClient httpClient;
//	@Mock
//	private HttpClientBuilder httpClientBuilder;
	@Mock
	private CloseableHttpResponse response;
	@Mock
	private StatusLine statusLine;
	@Spy
	private WQPLayerBuildingService service = WQPLayerBuildingService.getInstance();

	@Before
	public void beforeTest() {
		PowerMockito.mockStatic(HttpClients.class);
		MockitoAnnotations.initMocks(this);
//		when(HttpClients.custom()).thenReturn(httpClientBuilder);
//		when(httpClientBuilder.build()).thenReturn(httpClient);
	}

	@Test
	public void buildDynamicLayerTest() {
		//TODO
	}

	@Test
	public void createShapeFileTest() {
		//TODO
	}

	@Test
	public void createWorkspaceTest() {
		try {
			when(httpClient.execute(any(HttpPost.class))).thenThrow(new IOException("Hi")).thenReturn(response);
			when(response.getStatusLine()).thenReturn(statusLine);
			when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_OK, HttpStatus.SC_CREATED);
			
			try {
				service.createWorkspace(httpClient);
				fail("didn't get the OGCProxyException we were expecting");
			} catch (Exception e) {
				if (e instanceof OGCProxyException && e.getMessage().contains("Hi")) {
					//nothing to see here - is expected behavior
				} else {
					fail("Wrong exception thrown: " + e.getLocalizedMessage());
				}
			}
			verify(httpClient, times(1)).execute(any(HttpPost.class));
			
			try {
				service.createWorkspace(httpClient);
				fail("didn't get the OGCProxyException we were expecting");
			} catch (Exception e) {
				if (e instanceof OGCProxyException && e.getMessage().contains("Invalid status code from geoserver:200")) {
					//nothing to see here - is expected behavior
				} else {
					fail("Wrong exception thrown: " + e.getLocalizedMessage());
				}
			}
			verify(httpClient, times(2)).execute(any(HttpPost.class));
			
			//This time golden
			service.createWorkspace(httpClient);
			verify(httpClient, times(3)).execute(any(HttpPost.class));
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getLocalizedMessage());
		}
	}

	@Test
	public void getCredentialsProviderTest() {
		//TODO (maybe)
	}

	@Test
	public void getGeoJsonDataTest() {
		//TODO
	}

	@Test
	public void initializeTest() {
		//TODO
	}

	@Test
	public void processFeaturesTest() {
		//TODO
	}

	@Test
	public void processInputTest() {
		//TODO
	}

	@Test
	public void putShapefileTest() {
		try {
			when(httpClient.execute(any(HttpPut.class))).thenThrow(new IOException("Hi")).thenReturn(response);
			when(response.getStatusLine()).thenReturn(statusLine);
			when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_CREATED, HttpStatus.SC_OK);
			
			try {
				service.putShapefile(httpClient, "abc", "application/xml", new File("myFile"));
				fail("didn't get the OGCProxyException we were expecting");
			} catch (Exception e) {
				if (e instanceof OGCProxyException && e.getMessage().contains("Hi")) {
					//nothing to see here - is expected behavior
				} else {
					fail("Wrong exception thrown: " + e.getLocalizedMessage());
				}
			}
			verify(httpClient, times(1)).execute(any(HttpPut.class));

			service.putShapefile(httpClient, "abc", "application/xml", new File("myFile"));
			verify(httpClient, times(2)).execute(any(HttpPut.class));
			
			try {
				service.putShapefile(httpClient, "abc", "application/xml", new File("myFile"));
				fail("didn't get the OGCProxyException we were expecting");
			} catch (Exception e) {
				if (e instanceof OGCProxyException && e.getMessage().contains("Exception: Invalid status code from geoserver:200")) {
					//nothing to see here - is expected behavior
				} else {
					fail("Wrong exception thrown: " + e.getLocalizedMessage());
				}
			}
			verify(httpClient, times(3)).execute(any(HttpPut.class));
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getLocalizedMessage());
		}
	}
	
	@Test
	public void uploadShapefileTest() {
		//TODO
//		private void uploadShapefile(String layerName, String geoServerURI, String geoServerUser, String geoServerPass) throws OGCProxyException {
	}

	@Test
	public void verifyWorkspaceExistsTest() {
		try {
			when(httpClient.execute(any(HttpGet.class))).thenThrow(new IOException("Hi")).thenReturn(response);
			when(response.getStatusLine()).thenReturn(statusLine);
			when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_OK, HttpStatus.SC_NOT_FOUND, HttpStatus.SC_OK);
			
			try {
				service.verifyWorkspaceExists(httpClient);
				fail("didn't get the OGCProxyException we were expecting");
			} catch (Exception e) {
				if (e instanceof OGCProxyException && e.getMessage().contains("Hi")) {
					//nothing to see here - is expected behavior
				} else {
					fail("Wrong exception thrown: " + e.getLocalizedMessage());
				}
			}
			verify(httpClient, times(1)).execute(any(HttpGet.class));

			service.verifyWorkspaceExists(httpClient);
			verify(httpClient, times(2)).execute(any(HttpGet.class));
			
			try {
				service.verifyWorkspaceExists(httpClient);
				fail("didn't get the OGCProxyException we were expecting");
			} catch (Exception e) {
				if (e instanceof OGCProxyException && e.getMessage().contains("Exception: Invalid status code from geoserver:200")) {
					//nothing to see here - is expected behavior
				} else {
					fail("Wrong exception thrown: " + e.getLocalizedMessage());
				}
			}
			verify(httpClient, times(4)).execute(any(HttpGet.class));
			verify(service).createWorkspace(any(CloseableHttpClient.class));
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getLocalizedMessage());
		}		
	}
	
	@Test
	public void writeToShapeFileTest() {
		//TODO
	}

}
