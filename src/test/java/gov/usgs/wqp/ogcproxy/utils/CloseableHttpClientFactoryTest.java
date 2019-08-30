package gov.usgs.wqp.ogcproxy.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScheme;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Test;

public class CloseableHttpClientFactoryTest {

	public static final String TEST_PROTOCOL = "https";
	public static final String TEST_HOST = "owi.usgs.gov";
	public static final String TEST_PORT = "8444";
	public static final String TEST_USERNAME = "username";
	public static final String TEST_PASSWORD = "password";
	
	CloseableHttpClientFactory factory = CloseableHttpClientFactory.getInstance();

	@Test
	public void getAuthorizedCloseableHttpClientTest() {
		//We can't really test anything other than that a CloseableHttpClient is returned.
		CloseableHttpClient client = factory.getAuthorizedCloseableHttpClient(new BasicCredentialsProvider());
		assertNotNull(client);
		client = factory.getAuthorizedCloseableHttpClient(null);
		assertNotNull(client);
	}

	@Test
	public void getCommonClientBuilderTest() {
		//We can't really test anything other than that a CloseableHttpClient can be build from the returned object.
		CloseableHttpClient client = factory.getCommonClientBuilder().build();
		assertNotNull(client);
	}

	@Test
	public void getCredentialsProviderTest() {
		CredentialsProvider credentialsProvider = factory.getCredentialsProvider(TEST_HOST, TEST_USERNAME, TEST_PASSWORD);
		assertNotNull(credentialsProvider);
		HttpHost target = HttpHost.create(TEST_HOST);
		AuthScope authScope = new AuthScope(target);
		Credentials credentials = credentialsProvider.getCredentials(authScope);
		assertNotNull(credentials);
		assertEquals(TEST_PASSWORD, credentials.getPassword());
		assertEquals(TEST_USERNAME, credentials.getUserPrincipal().getName());
	}

	@Test
	public void getPreemptiveAuthContextTest() {
		HttpClientContext context = factory.getPreemptiveAuthContext(TEST_HOST);
		assertNotNull(context);
		AuthCache authCache = context.getAuthCache();
		assertNotNull(authCache);
		assertTrue(authCache instanceof BasicAuthCache);
		HttpHost host = new HttpHost(TEST_HOST);
		AuthScheme authScheme = authCache.get(host);
		assertNotNull(authScheme);
		assertEquals("basic", authScheme.getSchemeName());
	}

	@Test
	public void getUnauthorizedCloseableHttpClientTest() {
		//We can't really test anything other than that a CloseableHttpClient is returned.
		CloseableHttpClient client = factory.getUnauthorizedCloseableHttpClient(false);
		assertNotNull(client);
		client = factory.getUnauthorizedCloseableHttpClient(true);
		assertNotNull(client);
	}

	@Test
	public void initializeTest() throws InterruptedException {
		factory.initialize();
		assertNotNull(factory.clientConnectionManager);
		assertEquals(CloseableHttpClientFactory.CONNECTIONS_MAX_TOTAL, factory.clientConnectionManager.getMaxTotal());
		assertEquals(CloseableHttpClientFactory.CONNECTIONS_MAX_ROUTE, factory.clientConnectionManager.getDefaultMaxPerRoute());

		assertNotNull(factory.config);
		assertEquals(CloseableHttpClientFactory.CLIENT_SOCKET_TIMEOUT, factory.config.getSocketTimeout());
		assertEquals(CloseableHttpClientFactory.CLIENT_CONNECTION_TIMEOUT, factory.config.getConnectTimeout());
	}

}
