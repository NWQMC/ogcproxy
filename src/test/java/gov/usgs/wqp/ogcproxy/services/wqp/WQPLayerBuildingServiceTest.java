package gov.usgs.wqp.ogcproxy.services.wqp;

import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
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
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
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

@RunWith(PowerMockRunner.class)
@PrepareForTest(HttpClients.class)
public class WQPLayerBuildingServiceTest {

	@Mock
	private CloseableHttpClient httpClient;
	@Mock
	private HttpClientBuilder httpClientBuilder;
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
	public void getGeoJsonDataTest() {
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
	public void writeToShapeFileTest() {
		//TODO
	}

}
