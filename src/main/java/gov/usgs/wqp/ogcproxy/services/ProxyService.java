package gov.usgs.wqp.ogcproxy.services;

import static org.springframework.util.StringUtils.isEmpty;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpTrace;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.web.context.request.async.DeferredResult;

import gov.usgs.wqp.ogcproxy.exceptions.OGCProxyException;
import gov.usgs.wqp.ogcproxy.exceptions.OGCProxyExceptionID;
import gov.usgs.wqp.ogcproxy.model.OGCRequest;
import gov.usgs.wqp.ogcproxy.model.ogc.parameters.WFSParameters;
import gov.usgs.wqp.ogcproxy.model.ogc.services.OGCServices;
import gov.usgs.wqp.ogcproxy.model.parameters.ProxyDataSourceParameter;
import gov.usgs.wqp.ogcproxy.model.parser.xml.ogc.RequestWrapper;
import gov.usgs.wqp.ogcproxy.services.wqp.WQPLayerBuildingService;
import gov.usgs.wqp.ogcproxy.utils.ProxyServiceResult;
import gov.usgs.wqp.ogcproxy.utils.ProxyUtil;
import gov.usgs.wqp.ogcproxy.utils.SystemUtils;


public class ProxyService {
	@Autowired
	private Environment environment;

	@Autowired
	private WQPLayerBuildingService wqpLayerBuildingService;

	@Autowired
	private LayerCachingService layerCachingService;
	
	private static final Logger LOG = LoggerFactory.getLogger(ProxyService.class);
	
	private static boolean initialized;
	
	private static String geoserverProtocol  = "http";
	private static String geoserverHost      = "localhost";
	private static String geoserverPort      = "8080";
	private static String forwardUrl         = "http://localhost:8080";
	private static String geoserverContext   = "geoserver";

	// 15 minutes, default is infinite
	private static int connection_ttl            = 15 * 60 * 1000;
	private static int connections_max_total     = 256;
	private static int connections_max_route     = 32;
	// 5 minutes, default is infinite
	private static int client_socket_timeout     = 5 * 60 * 1000;
	// 15 seconds, default is infinite
	private static int client_connection_timeout = 15 * 1000;

	private static Set<String> ignoredClientRequestHeaderSet  = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
	private static Set<String> ignoredServerResponseHeaderSet = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
	
	private static final ProxyService INSTANCE = new ProxyService();

	private CloseableHttpClient serverClient;

	/**
	 * Private Constructor for Singleton Pattern
	 */
	private ProxyService() {
	}

	/**
	 * Singleton access
	 *
	 * @return ProxyService instance
	 */
	public static ProxyService getInstance() {
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
			
			String tmp = environment.getProperty("wqp.geoserver.proto");
			if (!isEmpty(tmp)) {
				geoserverProtocol = tmp;
			}
			tmp = environment.getProperty("wqp.geoserver.host");
			if (!isEmpty(tmp)) {
				geoserverHost = tmp;
			}
			tmp = environment.getProperty("wqp.geoserver.port");
			if (!isEmpty(tmp)) {
				geoserverPort= tmp;
			}

			forwardUrl = geoserverProtocol + "://" + geoserverHost + ":" + geoserverPort;

			tmp = environment.getProperty("wqp.geoserver.context");
			if (!isEmpty(tmp)) {
				geoserverContext = tmp;
			}

			// Initialize connection manager, this is thread-safe. if we
			// use this
			// with any HttpClient instance it becomes thread-safe.
			PoolingHttpClientConnectionManager clientConnectionManager = new PoolingHttpClientConnectionManager(
					connection_ttl, TimeUnit.MILLISECONDS);
			clientConnectionManager.setMaxTotal(connections_max_total);
			clientConnectionManager.setDefaultMaxPerRoute(connections_max_route);

			RequestConfig config = RequestConfig.custom().setConnectTimeout(client_connection_timeout)
					.setSocketTimeout(client_socket_timeout).build();

			//.disableContentCompression() keeps the response from geoserver in it's native form so the existing 
			//logic still works - otherwise it tries to decompress it causing issues in the scrubbing process.
			serverClient = HttpClients.custom().setConnectionManager(clientConnectionManager)
					.disableContentCompression().setDefaultRequestConfig(config).build();

			// Ignored headers relating to proxing requests
			// don't parameterize, need to switch host from proxy to server
			ignoredClientRequestHeaderSet.add("host");
			// don't parameterize, let proxy to server call do it's own handling
			ignoredClientRequestHeaderSet.add("connection");
			// parameterize (cookies passthru?)
			ignoredClientRequestHeaderSet.add("cookie");
			// parameterize (authorization passthru?)
			ignoredClientRequestHeaderSet.add("authorization");
			// ignore header in request, this is set in client call.
			ignoredClientRequestHeaderSet.add("content-length");

			// Ignored headers relating to proxing responses.
			// don't parameterize
			ignoredServerResponseHeaderSet.add("transfer-encoding");
			// don't parameterize
			ignoredServerResponseHeaderSet.add("keep-alive");
			// parameterize (cookies passthru?)
			ignoredServerResponseHeaderSet.add("set-cookie");
			// parameterize (authorization passthru?)
			ignoredServerResponseHeaderSet.add("authorization");
			// ignoredServerResponseHeaderSet.add("content-length");
			// allow for now, NOTE: are you doing response body content rewrite?
		}
	}

	/**
	 * This method provides WMS/WFS request proxying along with intercepting
	 * WMS/WFS requests and providing additional functionality based on the
	 * filters found in the request parameters.
	 *
	 * @param request
	 * @param response
	 * @param requestParams
	 * @param finalResult
	 */
	public DeferredResult<String> performRequest(HttpServletRequest request, HttpServletResponse response, 
			Map<String, String> requestParams, OGCServices ogcService) {
		
		DeferredResult<String> deferredResult = new DeferredResult<String>();
		
		//Interrogate the request, converting our vendor parameter values into the appropriate ogc values.
		//This may include building the dynamic layer associated with out "searchParams".
		OGCRequest ogcRequest = convertVendorParms(requestParams, ogcService);

		// We now need to perform the proxy call to the GeoServer and return the result to the client
		boolean proxySuccess = proxyRequest(request, response, ogcRequest);
		
		if (proxySuccess) {
			LOG.debug("ProxyService.performRequest() INFO: Proxy request is completed successfully.");
			deferredResult.setResult(ProxyServiceResult.SUCCESS.toString());
		} else {
			LOG.error("ProxyService.performRequest() Error:  Unable to proxy client request.");
			deferredResult.setResult(ProxyServiceResult.ERROR.toString());
		}
		return deferredResult;
	}

	protected OGCRequest convertVendorParms(Map<String, String> requestParams, OGCServices ogcService) {
		OGCRequest ogcRequest = ProxyUtil.separateParameters(ogcService, requestParams);
		
		if (ogcRequest.isValidRequest()) {
			// We can now proceed with the request. Depending on the value of
			// the layer parameter we will call the correct layer building service.
			LOG.debug("ProxyService.performRequest() Info: Kicking off search parameter logic for data source ["
					+ ogcRequest.getDataSource() + "]");
				// TODO asdf this must handle the POST
			String layerName = layerCachingService.getDynamicLayer(ogcRequest);
				
			ogcRequest.setLayerFromVendor(layerName);
		}

		return ogcRequest;
	}

	
	/**
	 * Logic and supporting methods stolen from
	 * gov.usgs.cida.proxy.AlternateProxyServlet
	 *
	 * @param clientRequest
	 * @param clientResponse
	 * @param requestParams
	 * @return
	 */
	private boolean proxyRequest(HttpServletRequest clientRequest, HttpServletResponse clientResponse,
			OGCRequest ogcRequest) {
		
		try {
			HttpUriRequest serverRequest = generateServerRequest(clientRequest, ogcRequest.getOgcParams());
			handleServerRequest(clientRequest, clientResponse, serverRequest, ogcRequest);
		} catch (OGCProxyException e) {
			LOG.error("ProxyService.proxyRequest() Error: proxying client request: " + e.getMessage(), e);
			return false;
		}

		return true;
	}

	private HttpUriRequest generateServerRequest( HttpServletRequest clientRequest, final Map<String, String> ogcParams) 
			throws OGCProxyException {
		
		HttpUriRequest serverRequest = null;
		
		try {
			// 1) Generate Server URI
			String serverRequestURIAsString = ProxyUtil.getServerRequestURIAsString(clientRequest, ogcParams,
							ProxyService.forwardUrl, ProxyService.geoserverContext);

			LOG.debug("ProxyService.generateServerRequest(): request to GeoServer is: [\n" + serverRequestURIAsString + "]");

			// instantiating to URL then calling toURI gives us some error
			// checking as URI(String) appears too forgiving.
			URI serverRequestURI = new URL(serverRequestURIAsString).toURI();

			// 2 ) Create request base on client request method
			switch (clientRequest.getMethod().toUpperCase()) {
            case "HEAD":
				serverRequest = new HttpHead(serverRequestURI);
                break;
            case "GET":
				serverRequest = new HttpGet(serverRequestURI);
                break;
            case "POST":
				serverRequest = new HttpPost(serverRequestURI);
                break;
            case "PUT":
				serverRequest = new HttpPut(serverRequestURI);
                break;
            case "DELETE":
				serverRequest = new HttpDelete(serverRequestURI);
                break;
            case "TRACE":
				serverRequest = new HttpTrace(serverRequestURI);
                break;
            case "OPTIONS":
				serverRequest = new HttpOptions(serverRequestURI);
                break;
			default:
				String msg = "ProxyService.generateServerRequest() Exception : Unsupported request method [" + serverRequest + "].";
				LOG.error(msg);
				OGCProxyExceptionID id = OGCProxyExceptionID.UNSUPPORTED_REQUEST_METHOD;
				throw new OGCProxyException(id, "ProxyService", "generateServerRequest()", msg);
			}

			// 3) Map client request headers to server request
			ProxyUtil.generateServerRequestHeaders(clientRequest, serverRequest, ProxyService.ignoredClientRequestHeaderSet);

			// 4) Copy client request body to server request
			int contentLength = clientRequest.getContentLength();
			String body = "";
			// revise the length to the length of the body without the searchParams
			if (clientRequest instanceof RequestWrapper) {
				String dynamicLayerName = ogcParams.get(WFSParameters.typeName.toString());
				body = ((RequestWrapper)clientRequest).getPostBodySansSearchParams();
				if ( ! isEmpty(dynamicLayerName) ) {
					body = ((RequestWrapper)clientRequest).getPostBodySansSearchParams().replaceAll(ProxyDataSourceParameter.WQP_SITES.toString(), dynamicLayerName);
				}
				// TODO asdf need to replace typeName with dynamic layer name here
				contentLength = body.length();
			}
			
			if (contentLength > 0) {
				// is this a POST (or PUT)
				if (serverRequest instanceof HttpEntityEnclosingRequest) {
					try {
						HttpEntityEnclosingRequest entityRequest = (HttpEntityEnclosingRequest) serverRequest;
						
						InputStreamEntity serverRequestEntity = new InputStreamEntity(clientRequest.getInputStream(), contentLength);
						serverRequestEntity.setContentType(clientRequest.getContentType());
						((HttpEntityEnclosingRequest) serverRequest).setEntity(serverRequestEntity);
												
				        HttpEntity entity = new ByteArrayEntity(body.getBytes("UTF-8"));
				        entityRequest.setEntity(entity);
				 
					} catch (IOException e) {
						String msg = "ProxyService.generateServerRequest() Exception : Error reading client request body [" + e.getMessage() + "].";
						LOG.error(msg, e);

						OGCProxyExceptionID id = OGCProxyExceptionID.ERROR_READING_CLIENT_REQUEST_BODY;
						throw new OGCProxyException(id, "ProxyService", "generateServerRequest()", msg);
					}
				} else {
					String msg = "ProxyService.generateServerRequest() Exception : Content in request body unsupported for client request method [" + serverRequest.getMethod() + "].";
					LOG.error(msg);

					OGCProxyExceptionID id = OGCProxyExceptionID.UNSUPPORTED_CONTENT_FOR_REQUEST_METHOD;
					throw new OGCProxyException(id, "ProxyService", "generateServerRequest()", msg);
				}
			}

		} catch (MalformedURLException e) {
			String msg = "ProxyService.generateServerRequest() Exception : Syntax error parsing server URL ["
					+ e.getMessage() + "].";
			LOG.error(msg, e);

			OGCProxyExceptionID id = OGCProxyExceptionID.URL_PARSING_EXCEPTION;
			throw new OGCProxyException(id, "ProxyService",
					"generateServerRequest()", msg);
		} catch (URISyntaxException e) {
			String msg = "ProxyService.generateServerRequest() Exception : Syntax error parsing server URL ["
					+ e.getMessage() + "].";
			LOG.error(msg, e);

			OGCProxyExceptionID id = OGCProxyExceptionID.URL_PARSING_EXCEPTION;
			throw new OGCProxyException(id, "ProxyService",
					"generateServerRequest()", msg);
		}

		return serverRequest;
	}

	private void handleServerRequest(HttpServletRequest clientRequest,
			HttpServletResponse clientResponse, HttpUriRequest serverRequest,
			OGCRequest ogcRequest) throws OGCProxyException {
		
		try {
			HttpContext localContext = new BasicHttpContext();
			HttpResponse methodReponse = serverClient.execute(serverRequest, localContext);
			handleServerResponse(clientRequest, clientResponse, methodReponse, ogcRequest);
		} catch (ClientProtocolException e) {
			String msg = "ProxyService.handleServerRequest() Exception : Client protocol error ["
					+ e.getMessage() + "]";
			LOG.error(msg, e);

			OGCProxyExceptionID id = OGCProxyExceptionID.CLIENT_PROTOCOL_ERROR;
			throw new OGCProxyException(id, "ProxyService",
					"handleServerRequest()", msg);
		} catch (IOException e) {
			String msg = "ProxyService.handleServerRequest() Exception : I/O error on server request ["
					+ e.getMessage() + "]";
			LOG.error(msg, e);

			OGCProxyExceptionID id = OGCProxyExceptionID.SERVER_REQUEST_IO_ERROR;
			throw new OGCProxyException(id, "ProxyService",
					"handleServerRequest()", msg);
		}
	}

	private void handleServerResponse(HttpServletRequest clientRequest,
			HttpServletResponse clientResponse, HttpResponse serverResponse,
			OGCRequest ogcRequest) throws OGCProxyException {
		
		String clientRequestURLAsString = ProxyUtil.getClientRequestURIAsString(clientRequest);
		String serverRequestURLAsString = ProxyUtil.getServerRequestURIAsString(clientRequest, ogcRequest.getOgcParams(),
						ProxyService.forwardUrl, ProxyService.geoserverContext);

		// 1) Map server response status to client response
		// NOTE: There's no clear way to map status message,
		// HttpServletResponse.sendError(int, String)
		// will display some custom html (we don't want that here).
		// HttpServletResponse.setStatus(int, String)
		// is deprecated and i'm not certain there is (will be) an functional implementation behind it.
		StatusLine serverStatusLine = serverResponse.getStatusLine();
		int statusCode = serverStatusLine.getStatusCode();
		clientResponse.setStatus(statusCode);
		LOG.debug("ProxyService.handleServerResponse() DEBUG: Mapped server status code "
				+ statusCode);

		// 2) Map server response headers to client response
		ProxyUtil.generateClientResponseHeaders(clientResponse, serverResponse, ProxyService.ignoredServerResponseHeaderSet);

		// 3) Copy server response body to client response
		HttpEntity methodEntity = serverResponse.getEntity();
		if (methodEntity != null) {

			InputStream  is = null;
			OutputStream os = null;

			boolean contentCompressed = false;

			if (methodEntity.getContentEncoding() != null) {
				String contentEncoding = methodEntity.getContentEncoding().getValue();
				if ( "gzip".equalsIgnoreCase(contentEncoding) ) {
					contentCompressed = true;
				}
			}

			long responseBytes = 0;

			try {
				// !!! Are you here to edit this to enable response body content rewrite?
				// You may want to remove or edit the "Content-Length" header !!!
				try {
					is = methodEntity.getContent();
				} catch (IOException e) {
					String msg = "ProxyService.handleServerResponse() Exception : Error obtaining input stream for server response ["
							+ e.getMessage() + "]";
					LOG.error(msg, e);

					OGCProxyExceptionID id = OGCProxyExceptionID.SERVER_RESPONSE_INPUT_STREAM_ERROR;
					throw new OGCProxyException(id, "ProxyService",
							"handleServerResponse()", msg);
				}

				try {
					os = clientResponse.getOutputStream();
				} catch (IOException e) {
					String msg = "ProxyService.handleServerResponse() Exception : Error obtaining output stream for client response ["
							+ e.getMessage() + "]";
					LOG.error(msg, e);

					OGCProxyExceptionID id = OGCProxyExceptionID.CLIENT_RESPONSE_OUTPUT_STREAM_ERROR;
					throw new OGCProxyException(id, "ProxyService",
							"handleServerResponse()", msg);
				}

				byte[] serverContent;
				try {
					serverContent = IOUtils.toByteArray(is);
				} catch (IOException e) {
					String msg = "ProxyService.handleServerResponse() Exception : Error copying server response to byteArray ["
							+ e.getMessage() + "]";
					LOG.error(msg, e);

					OGCProxyExceptionID id = OGCProxyExceptionID.SERVER_TO_CLIENT_RESPONSE_ERROR;
					throw new OGCProxyException(id, "ProxyService",
							"handleServerResponse()", msg);
				}
				
				// Now, if the server response is xml we need to inspect it and
				// modify depending on a couple logic steps.
				//
				// 		WMS uses "text/xml" as the contentType
				// 		WFS uses "application/xml" as the contentType.
				//
				// We will just search for "xml" in the contentType value to
				// cover both cases.  If we need more contentType support we'll
				// have to expand the following logic.
				byte[] inspectedBytes;
				String contentType = methodEntity.getContentType().getValue();
				if ((contentType != null) && (contentType.toLowerCase().contains("xml"))) {
					inspectedBytes = inspectServerContent(clientRequest, ogcRequest, serverContent, contentCompressed);
					
					// We must set the content-length here for the possible change
					// in content from the inspection
					clientResponse.setContentLength(inspectedBytes.length);
				} else {
					inspectedBytes = serverContent;
				}
	
				try {
					IOUtils.write(inspectedBytes, os);
				} catch (IOException e) {
					String msg = "ProxyService.handleServerResponse() Exception : Error copying server response to client ["
							+ e.getMessage() + "]";
					LOG.error(msg, e);

					OGCProxyExceptionID id = OGCProxyExceptionID.SERVER_TO_CLIENT_RESPONSE_ERROR;
					throw new OGCProxyException(id, "ProxyService",
							"handleServerResponse()", msg);
				}

			} finally {
				LOG.debug("ProxyService.handleServerResponse() DEBUG: Copied "
						+ responseBytes
						+ " bytes from server response for proxy from "
						+ clientRequestURLAsString + " to "
						+ serverRequestURLAsString);
				try {
					// This is important to guarantee connection release back
					// into
					// connection pool for future reuse!
					EntityUtils.consume(methodEntity);
				} catch (IOException e) {
					LOG.error("ProxyService.handleServerResponse() Error: consuming remaining bytes in server response entity for proxy reponse from "
							+ clientRequestURLAsString
							+ " to "
							+ serverRequestURLAsString, e);
				}
				IOUtils.closeQuietly(is);
				IOUtils.closeQuietly(os);
			}
		} else {
			LOG.warn("ProxyService.handleServerResponse() WARN: Server response was empty for proxy response from "
					+ clientRequestURLAsString
					+ " to "
					+ serverRequestURLAsString);
		}
	}
	
	private byte[] inspectServerContent(HttpServletRequest clientRequest, OGCRequest ogcRequest, byte[] serverContent, boolean contentCompressed) throws OGCProxyException {
		String stringContent = "";
		if (contentCompressed) {
			stringContent = SystemUtils.uncompressGzipAsString(serverContent);
		} else {
			stringContent = Arrays.toString(serverContent);
		}
		
		// We now need to do some inspection on the data.  If the original OGC
		// request is a GetCapabilities, we need to insert the service's specific
		// information into the response.
		//TODO - should this constant be in ProxyUtil?
		if (ProxyUtil.OGC_GET_CAPABILITIES.equalsIgnoreCase(ogcRequest.getRequestType())) {
			// This is a GetCapabilities call.  We need to include the service
			// specific GetCapabilities information in the result so we conform
			// to the OGC spec
			switch (ogcRequest.getDataSource()) {
				case WQP_SITES: 
					stringContent = wqpLayerBuildingService.addGetCapabilitiesInfo(ogcRequest.getOgcService(), stringContent);
					break;
				default:
					break;
			}
		}
		
		// We also need to scrub the response for any mention of the actual
		// GeoServer's location and replace it with this proxy's location.
		if (stringContent.contains(geoserverHost)) {
			LOG.debug("ProxyService.inspectServerContent() INFO : Server response contains references to its hostname.  Redirecting entries to the proxy...");

			String proxyServerName = clientRequest.getServerName();
			String proxyContextPath = clientRequest
					.getContextPath();
			String proxyPort = clientRequest.getLocalPort() + "";
			String proxyProtocol = clientRequest.getScheme();

			stringContent = ProxyUtil.redirectContentToProxy(
					stringContent, geoserverProtocol,
					proxyProtocol, geoserverHost, proxyServerName,
					geoserverPort, proxyPort, geoserverContext,
					proxyContextPath);
		} else {
			LOG.debug("\n\nProxyService.handleServerResponse() DEBUG : Server response does not contain references to its hostname.  Continuing...\n\n");
		}
		
		if (contentCompressed) {
			return SystemUtils.compressStringToGzip(stringContent);
		} else {
			return stringContent.getBytes();
		}
	}

}
