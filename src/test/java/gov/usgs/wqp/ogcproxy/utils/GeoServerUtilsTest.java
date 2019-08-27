package gov.usgs.wqp.ogcproxy.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;

import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.reflect.Whitebox;

import gov.usgs.wqp.ogcproxy.exceptions.OGCProxyException;
import gov.usgs.wqp.ogcproxy.services.ConfigurationService;

public class GeoServerUtilsTest {

	GeoServerUtils geoServerUtils;

	@Mock
	private CloseableHttpClient httpClient;
	@Mock
	private HttpClientContext localContext;
	@Mock
	private CloseableHttpResponse response;
	@Mock
	private StatusLine statusLine;
	@Mock
	CloseableHttpClientFactory factory;
	@Mock
	private File file;
	@Mock
	CredentialsProvider credentialsProvider;

	ConfigurationService configurationService;

	@Before
	public void beforeTest() {
		MockitoAnnotations.initMocks(this);
		configurationService = new ConfigurationService();
		geoServerUtils = new GeoServerUtils(factory, configurationService);
		Whitebox.setInternalState(configurationService, "geoserverHost", "https://owi.usgs.gov");
		Whitebox.setInternalState(configurationService, "geoserverContext", "geoserver");
		Whitebox.setInternalState(configurationService, "geoserverWorkspace", "wqp_sites");
		Whitebox.setInternalState(configurationService, "geoserverUser", "username");
		Whitebox.setInternalState(configurationService, "geoserverPass", "pwd");
	}

	@Test
	public void buildAuthorizedClientTest() {
		when(factory.getCredentialsProvider(anyString(), anyString(), anyString())).thenReturn(credentialsProvider);
		when(factory.getAuthorizedCloseableHttpClient(any(CredentialsProvider.class))).thenReturn(httpClient);
		CloseableHttpClient client = geoServerUtils.buildAuthorizedClient();
		assertNotNull(client);
		verify(factory).getCredentialsProvider("https://owi.usgs.gov",  "username", "pwd");
	}

	@Test
	public void buildLocalContextTest() {
		when(factory.getPreemptiveAuthContext(anyString())).thenReturn(localContext);
		HttpClientContext context = geoServerUtils.buildLocalContext();
		assertNotNull(context);
		verify(factory).getPreemptiveAuthContext("https://owi.usgs.gov");
	}

	@Test
	public void buildNamespacePostTest() {
		assertEquals("https://owi.usgs.gov/geoserver/rest/namespaces",
				geoServerUtils.buildNamespacePost());
	}

	@Test
	public void buildShapeFileRestPutTest() {
		assertEquals("https://owi.usgs.gov/geoserver/rest/workspaces/wqp_sites/datastores/testLayer/file.shp",
				geoServerUtils.buildShapeFileRestPut("testLayer"));
	}

	@Test
	public void buildWorkspaceRestDeleteTest() {
		assertEquals("https://owi.usgs.gov/geoserver/rest/workspaces/wqp_sites?recurse=true",
				geoServerUtils.buildWorkspaceRestDelete());
	}

	@Test
	public void buildWorkspacesRestGetTest() {
		assertEquals("https://owi.usgs.gov/geoserver/rest/workspaces/wqp_sites.json",
				geoServerUtils.buildWorkspacesRestGet());
	}

	@Test
	public void buildResourceRestDeleteTest() {
		assertEquals("https://owi.usgs.gov/geoserver/rest/resource/data/wqp_sites",
			geoServerUtils.buildResourceRestDelete());
	}

	@Test
	public void createWorkspaceTest() {
		try {
			when(httpClient.execute(any(HttpPost.class), any(HttpClientContext.class))).thenThrow(new IOException("Hi")).thenReturn(response);
			when(response.getStatusLine()).thenReturn(statusLine);
			when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_OK, HttpStatus.SC_CREATED);

			try {
				geoServerUtils.createWorkspace(httpClient, localContext);
				fail("didn't get the OGCProxyException we were expecting");
			} catch (Exception e) {
				if (e instanceof OGCProxyException && e.getMessage().contains("Hi")) {
					//nothing to see here - is expected behavior
				} else {
					fail("Wrong exception thrown: " + e.getLocalizedMessage());
				}
			}
			verify(httpClient, times(1)).execute(any(HttpPost.class), any(HttpClientContext.class));

			try {
				geoServerUtils.createWorkspace(httpClient, localContext);
				fail("didn't get the OGCProxyException we were expecting");
			} catch (Exception e) {
				if (e instanceof OGCProxyException && e.getMessage().contains("Invalid status code from geoserver:200")) {
					//nothing to see here - is expected behavior
				} else {
					fail("Wrong exception thrown: " + e.getLocalizedMessage());
				}
			}
			verify(httpClient, times(2)).execute(any(HttpPost.class), any(HttpClientContext.class));

			//This time golden
			geoServerUtils.createWorkspace(httpClient, localContext);
			verify(httpClient, times(3)).execute(any(HttpPost.class), any(HttpClientContext.class));
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getLocalizedMessage());
		}
	}

	@Test
	public void putShapefileTest() {
		try {
			when(httpClient.execute(any(HttpPut.class), any(HttpClientContext.class))).thenThrow(new IOException("Hi")).thenReturn(response);
			when(response.getStatusLine()).thenReturn(statusLine);
			when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_CREATED, HttpStatus.SC_OK);

			try {
				geoServerUtils.putShapefile(httpClient, localContext, "abc", "application/xml", new File("myFile"));
				fail("didn't get the OGCProxyException we were expecting");
			} catch (Exception e) {
				if (e instanceof OGCProxyException && e.getMessage().contains("Hi")) {
					//nothing to see here - is expected behavior
				} else {
					fail("Wrong exception thrown: " + e.getLocalizedMessage());
				}
			}
			verify(httpClient, times(1)).execute(any(HttpPut.class), any(HttpClientContext.class));

			geoServerUtils.putShapefile(httpClient, localContext, "abc", "application/xml", new File("myFile"));
			verify(httpClient, times(2)).execute(any(HttpPut.class), any(HttpClientContext.class));

			try {
				geoServerUtils.putShapefile(httpClient, localContext, "abc", "application/xml", new File("myFile"));
				fail("didn't get the OGCProxyException we were expecting");
			} catch (Exception e) {
				if (e instanceof OGCProxyException && e.getMessage().contains("Exception: Invalid status code from geoserver:200")) {
					//nothing to see here - is expected behavior
				} else {
					fail("Wrong exception thrown: " + e.getLocalizedMessage());
				}
			}
			verify(httpClient, times(3)).execute(any(HttpPut.class), any(HttpClientContext.class));
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getLocalizedMessage());
		}
	}

	@Test
	public void verifyWorkspaceExistsTest() {
		try {
			when(factory.getPreemptiveAuthContext(isNull())).thenReturn(localContext);
			when(httpClient.execute(any(HttpGet.class), any(HttpClientContext.class))).thenThrow(new IOException("Hi")).thenReturn(response);
			when(httpClient.execute(any(HttpPost.class), any(HttpClientContext.class))).thenReturn(response);
			when(response.getStatusLine()).thenReturn(statusLine);
			when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_OK, HttpStatus.SC_NOT_FOUND, HttpStatus.SC_OK);

			try {
				geoServerUtils.verifyWorkspaceExists(httpClient, localContext);
				fail("didn't get the OGCProxyException we were expecting");
			} catch (Exception e) {
				if (e instanceof OGCProxyException && e.getMessage().contains("Hi")) {
					//nothing to see here - is expected behavior
				} else {
					fail("Wrong exception thrown: " + e.getLocalizedMessage());
				}
			}
			verify(httpClient, times(1)).execute(any(HttpGet.class), any(HttpClientContext.class));

			geoServerUtils.verifyWorkspaceExists(httpClient, localContext);
			verify(httpClient, times(2)).execute(any(HttpGet.class), any(HttpClientContext.class));

			try {
				geoServerUtils.verifyWorkspaceExists(httpClient, localContext);
				fail("didn't get the OGCProxyException we were expecting");
			} catch (Exception e) {
				if (e instanceof OGCProxyException && e.getMessage().contains("Exception: Invalid status code from geoserver:200")) {
					//nothing to see here - is expected behavior
				} else {
					fail("Wrong exception thrown: " + e.getLocalizedMessage());
				}
			}
			verify(httpClient, times(3)).execute(any(HttpGet.class), any(HttpClientContext.class));
			verify(httpClient).execute(any(HttpPost.class), any(HttpClientContext.class));
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getLocalizedMessage());
		}
	}

}
