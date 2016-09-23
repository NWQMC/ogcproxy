package gov.usgs.wqp.ogcproxy.utils;

import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

import gov.usgs.wqp.ogcproxy.services.ProxyService;

public class CloseableHttpClientFactory {

	private volatile boolean initialized;

	private static final CloseableHttpClientFactory INSTANCE = new CloseableHttpClientFactory();

	protected PoolingHttpClientConnectionManager clientConnectionManager;
	protected RequestConfig config;

	/**
	 * Private Constructor for Singleton Pattern
	 */
	private CloseableHttpClientFactory() {
	}

	// 15 minutes, default is infinite
	public static int CONNECTION_TTL = 15 * 60 * 1000;
	public static int CONNECTIONS_MAX_TOTAL = 256;
	public static int CONNECTIONS_MAX_ROUTE = 32;
	// 5 minutes, default is infinite
	public static int CLIENT_SOCKET_TIMEOUT = 5 * 60 * 1000;
	// 15 seconds, default is infinite
	public static int CLIENT_CONNECTION_TIMEOUT = 15 * 1000;

	/**
	 * Singleton access
	 *
	 * @return CloseableHttpClientFactory instance
	 */
	public static CloseableHttpClientFactory getInstance() {
		return INSTANCE;
	}

	/**
	 * Since we are using Spring DI we cannot access the environment bean in
	 * the constructor. We'll just use a locked initialized variable to
	 * check initialization after instantiation and set the env properties here.
	 */
	@PostConstruct
	public void initialize() {
		if ( initialized ) {
			return;
		}
		synchronized (ProxyService.class) {
			if ( initialized ) {
				return;
			}
			initialized = true;

			// Initialize connection manager, this is thread-safe. if we use this
			// with any HttpClient instance it becomes thread-safe.
			clientConnectionManager = new PoolingHttpClientConnectionManager(CONNECTION_TTL, TimeUnit.MILLISECONDS);
			clientConnectionManager.setMaxTotal(CONNECTIONS_MAX_TOTAL);
			clientConnectionManager.setDefaultMaxPerRoute(CONNECTIONS_MAX_ROUTE);

			config = RequestConfig.custom().setConnectTimeout(CLIENT_CONNECTION_TIMEOUT)
					.setSocketTimeout(CLIENT_SOCKET_TIMEOUT).build();
		}
	}

	public HttpClientBuilder getCommonClientBuilder() {
		return HttpClients.custom().setConnectionManager(clientConnectionManager)
				.setDefaultRequestConfig(config).disableAutomaticRetries();
	}

	public CloseableHttpClient getUnauthorizedCloseableHttpClient(boolean disableContentCompression) {
		HttpClientBuilder builder = getCommonClientBuilder();

		if (disableContentCompression) {
			//.disableContentCompression() keeps the response from geoserver in it's native form so the existing 
			//logic still works - otherwise it tries to decompress it causing issues in the scrubbing process.
			builder = builder.disableContentCompression();
		}

		return builder.build();
	}

	public CloseableHttpClient getAuthorizedCloseableHttpClient(CredentialsProvider credentialsProvider) {
		return getCommonClientBuilder().setDefaultCredentialsProvider(credentialsProvider).build();
	}

	public CredentialsProvider getCredentialsProvider(String host, String port, String username, String password) {
		CredentialsProvider credsProvider = new BasicCredentialsProvider();
		credsProvider.setCredentials(
				new AuthScope(host, Integer.parseInt(port)),
				new UsernamePasswordCredentials(username, password));
		return credsProvider;
	}

	public HttpClientContext getPreemptiveAuthContext(String host, String port, String protocol) {
		HttpHost target = new HttpHost(host, Integer.parseInt(port), protocol);

		BasicScheme basicAuth = new BasicScheme();

		AuthCache authCache = new BasicAuthCache();
		authCache.put(target, basicAuth);

		HttpClientContext localContext = HttpClientContext.create();
		localContext.setAuthCache(authCache);

		return localContext;
	}

}
