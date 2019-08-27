package gov.usgs.wqp.ogcproxy.utils;

import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.whenNew;

import java.io.File;
import java.io.IOException;

import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import gov.usgs.wqp.ogcproxy.exceptions.OGCProxyException;
import gov.usgs.wqp.ogcproxy.exceptions.OGCProxyExceptionID;
import gov.usgs.wqp.ogcproxy.services.ConfigurationService;

@RunWith(PowerMockRunner.class)
@PrepareForTest(GeoServerUtils.class)
public class GeoServerUtilsUploadShapefileTest {

	private ConfigurationService configurationService = new ConfigurationService();

	@Mock
	private CloseableHttpClient httpClient;

	@Mock
	private HttpClientContext localContext;

	@Mock
	private CloseableHttpResponse response;

	@Mock
	private StatusLine statusLine;

	@Mock
	private CloseableHttpClientFactory factory;

	@Spy
	private GeoServerUtils geoServerUtils = new GeoServerUtils(factory, configurationService);

	private File file;

	@Before
	public void beforeTest() {
		file = PowerMockito.mock(File.class);
		MockitoAnnotations.initMocks(this);
		geoServerUtils.closeableHttpClientFactory = factory;
	}

	@Test
	public void uploadShapefileHappyPathTest() {
		try {
			whenNew(File.class).withAnyArguments().thenReturn(file);
			when(factory.getPreemptiveAuthContext(isNull())).thenReturn(localContext);
			when(httpClient.execute(any(HttpGet.class), any(HttpClientContext.class))).thenReturn(response);
			when(httpClient.execute(any(HttpPut.class), any(HttpClientContext.class))).thenReturn(response);
			when(file.exists()).thenReturn(true);
			when(response.getStatusLine()).thenReturn(statusLine);
			when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_OK, HttpStatus.SC_CREATED);

			geoServerUtils.uploadShapefile(httpClient, "", "");
			verify(geoServerUtils).verifyWorkspaceExists(any(CloseableHttpClient.class), any(HttpClientContext.class));
			verify(geoServerUtils).putShapefile(any(CloseableHttpClient.class), any(HttpClientContext.class), anyString(), anyString(), any(File.class));
			verify(file).exists();
			verify(file).delete();
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getLocalizedMessage());
		}
	}

	@Test
	public void uploadShapefileNoFileTest() {
		try {
			whenNew(File.class).withAnyArguments().thenReturn(file);
			when(file.exists()).thenReturn(false);

			try {
				geoServerUtils.uploadShapefile(httpClient, "", "");
				fail("didn't get the OGCProxyException we were expecting");
			} catch (Exception e) {
				if (!(e instanceof OGCProxyException && ((OGCProxyException)e).getExceptionid() == OGCProxyExceptionID.UPLOAD_SHAPEFILE_ERROR)) {
					fail("Wrong exception thrown: " + e.getLocalizedMessage());
				}
			}
			verify(geoServerUtils, never()).verifyWorkspaceExists(any(CloseableHttpClient.class), any(HttpClientContext.class));
			verify(geoServerUtils, never()).putShapefile(any(CloseableHttpClient.class), any(HttpClientContext.class), anyString(), anyString(), any(File.class));
			verify(file).exists();
			verify(file, never()).delete();
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getLocalizedMessage());
		}
	}


	@Test
	public void uploadShapefileFileDeleteOnErrorTest() {
		try {
			whenNew(File.class).withAnyArguments().thenReturn(file);
			when(file.exists()).thenReturn(true);
			when(httpClient.execute(any(HttpGet.class), any(HttpClientContext.class))).thenThrow(new IOException("Hi"));

			try {
				geoServerUtils.uploadShapefile(httpClient, "", "");
				fail("didn't get the OGCProxyException we were expecting");
			} catch (Exception e) {
				if (!(e instanceof OGCProxyException && ((OGCProxyException)e).getExceptionid() == OGCProxyExceptionID.UPLOAD_SHAPEFILE_ERROR)) {
					fail("Wrong exception thrown: " + e.getLocalizedMessage());
				}
			}
			verify(geoServerUtils).verifyWorkspaceExists(any(CloseableHttpClient.class), isNull());
			verify(geoServerUtils, never()).putShapefile(any(CloseableHttpClient.class), isNull(), anyString(), anyString(), any(File.class));
			verify(file).exists();
			verify(file).delete();
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getLocalizedMessage());
		}
	}
}
