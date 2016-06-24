package gov.usgs.wqp.ogcproxy.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;

import javax.annotation.Resource;

import org.apache.http.HttpHost;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.auth.AuthScheme;
import org.apache.http.client.AuthCache;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.web.WebAppConfiguration;

import gov.usgs.ogcproxy.springinit.TestSpringConfig;
import gov.usgs.wqp.ogcproxy.exceptions.OGCProxyException;
import gov.usgs.wqp.springinit.SpringConfig;

@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes={SpringConfig.class, TestSpringConfig.class})
@TestExecutionListeners({DependencyInjectionTestExecutionListener.class})
public class GeoServerUtilsTest {

	@Resource
	@Spy
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
	private File file;

	@Before
	public void beforeTest() {
		MockitoAnnotations.initMocks(this);
	}

	@Test
	public void buildAuthorizedClientTest() {
		//We can't really test anything other than that a CloseableHttpClient is returned.
		CloseableHttpClient client = geoServerUtils.buildAuthorizedClient();
		assertNotNull(client);
	}

	@Test
	public void buildLocalContextTest() {
		HttpClientContext context = geoServerUtils.buildLocalContext();
		assertNotNull(context);
		AuthCache authCache = context.getAuthCache();
		assertNotNull(authCache);
		assertTrue(authCache instanceof BasicAuthCache);
		HttpHost host = new HttpHost(CloseableHttpClientFactoryTest.TEST_HOST, Integer.parseInt(CloseableHttpClientFactoryTest.TEST_PORT),
				CloseableHttpClientFactoryTest.TEST_PROTOCOL);
		AuthScheme authScheme = authCache.get(host);
		assertNotNull(authScheme);
		assertEquals("basic", authScheme.getSchemeName());
	}

	@Test
	public void buildNamespacePostTest() {
		assertEquals("https://owi.usgs.gov:8444/geoserver/rest/namespaces",
				geoServerUtils.buildNamespacePost());
	}

	@Test
	public void buildShapeFileRestPutTest() {
		assertEquals("https://owi.usgs.gov:8444/geoserver/rest/workspaces/wqp_sites/datastores/testLayer/file.shp",
				geoServerUtils.buildShapeFileRestPut("testLayer"));
	}

	@Test
	public void buildWorkspaceRestDeleteTest() {
		assertEquals("https://owi.usgs.gov:8444/geoserver/rest/workspaces/wqp_sites?recurse=true",
				geoServerUtils.buildWorkspaceRestDelete());
	}

	@Test
	public void buildWorkspacesRestGetTest() {
		assertEquals("https://owi.usgs.gov:8444/geoserver/rest/workspaces/wqp_sites.json",
				geoServerUtils.buildWorkspacesRestGet());
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
			when(httpClient.execute(any(HttpGet.class), any(HttpClientContext.class))).thenThrow(new IOException("Hi")).thenReturn(response);
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
			verify(httpClient, times(4)).execute(any(HttpGet.class), any(HttpClientContext.class));
			verify(geoServerUtils).createWorkspace(any(CloseableHttpClient.class), any(HttpClientContext.class));
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getLocalizedMessage());
		}
	}

}
