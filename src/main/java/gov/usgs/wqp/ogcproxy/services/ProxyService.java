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

import gov.usgs.wqp.ogcproxy.exceptions.OGCProxyException;
import gov.usgs.wqp.ogcproxy.exceptions.OGCProxyExceptionID;
import gov.usgs.wqp.ogcproxy.model.OGCRequest;
import gov.usgs.wqp.ogcproxy.model.ogc.services.OGCServices;
import gov.usgs.wqp.ogcproxy.model.parameters.ProxyDataSourceParameter;
import gov.usgs.wqp.ogcproxy.services.wqp.WQPDynamicLayerCachingService;
import gov.usgs.wqp.ogcproxy.utils.ProxyUtil;
import gov.usgs.wqp.ogcproxy.utils.SystemUtils;


public class ProxyService {
	@Autowired
	private Environment environment;

	@Autowired
	private WQPDynamicLayerCachingService layerCachingService;

	private static final Logger LOG = LoggerFactory.getLogger(ProxyService.class);
	private static final String CLASSNAME = ProxyService.class.getName();

	private static boolean initialized;

	private static String geoserverProtocol	= "http";
	private static String geoserverHost		= "localhost";
	private static String geoserverPort		= "8080";
	private static String forwardUrl		= "http://localhost:8080";
	private static String geoserverContext	= "geoserver";

	// 15 minutes, default is infinite
	private static int connection_ttl			= 15 * 60 * 1000;
	private static int connections_max_total	= 256;
	private static int connections_max_route	= 32;
	// 5 minutes, default is infinite
	private static int client_socket_timeout	= 5 * 60 * 1000;
	// 15 seconds, default is infinite
	private static int client_connection_timeout = 15 * 1000;

	public static final String WMS_GET_CAPABILITIES_CONTENT = "<Layer queryable=\"1\">" +
			"<Name>wqp_sites</Name>" +
			"<Title>wqp_sites</Title>" +
			"<Abstract />" +
			"<KeywordList>" +
			"<Keyword>features</Keyword>" +
			"<Keyword>wqp_sites</Keyword>" +
			"</KeywordList>" +
			"<CRS>EPSG:4326</CRS>" +
			"<EX_GeographicBoundingBox>" +
			"<westBoundLongitude>-179.144806</westBoundLongitude>" +
			"<eastBoundLongitude>179.76416</eastBoundLongitude>" +
			"<southBoundLatitude>18.913826</southBoundLatitude>" +
			"<northBoundLatitude>71.332649</northBoundLatitude>" +
			"</EX_GeographicBoundingBox>" +
			"<BoundingBox CRS=\"CRS:84\" minx=\"-179.144806\" miny=\"18.913826\"" +
			" maxx=\"179.76416\" maxy=\"71.332649\" />" +
			"<BoundingBox CRS=\"EPSG:4326\" minx=\"18.913826\" miny=\"-179.144806\"" +
			" maxx=\"71.332649\" maxy=\"179.76416\" />" +
			"<Style>" +
			"<Name>point</Name>" +
			"<Title>Default Point</Title>" +
			"<Abstract>A sample style that draws a point</Abstract>" +
			"</Style>" +
			"</Layer>";

	/**
	 * WFS GetFeature allows the use of the searchParams parameter.  Declare it in the GetCapabilities
	 * document with the ows:AnyValue indicator (http://schemas.opengis.net/ows/1.1.0/owsDomainType.xsd)
	 */
	public static final String WFS_GET_CAPABILITIES_CONTENT = "<ows:Parameter name=\"searchParams\">" +
			"<ows:AnyValue />" +
			"</ows:Parameter>" +
			"<ows:Parameter name=\"typeName\">" +
			"<ows:AllowedValues>" +
			"<ows:Value>wqp_sites</ows:Value>" +
			"</ows:AllowedValues>" +
			"</ows:Parameter>";

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
		}
	}

	/**
	 * This method provides WMS/WFS request proxying along with intercepting
	 * WMS/WFS requests and providing additional functionality based on the
	 * filters found in the request parameters.
	 *
	 * @param request from the client
	 * @param response to the client
	 * @param ogcService specified service, may be overridden in the actual request parameters
	 */
	public void performRequest(HttpServletRequest request, HttpServletResponse response, OGCServices ogcService) {
		//Interrogate the request, converting our vendor parameter values into the appropriate ogc values.
		//This may include building the dynamic layer associated with out "searchParams".
		OGCRequest ogcRequest = convertVendorParms(request, ogcService);

		// We now need to perform the proxy call to the GeoServer and return the result to the client
		boolean proxySuccess = proxyRequest(request, response, ogcRequest);

		if (proxySuccess) {
			LOG.trace("Proxy request is completed successfully.");
		} else {
			LOG.error("Unable to proxy client request.");
		}
	}

	protected OGCRequest convertVendorParms(HttpServletRequest request, OGCServices ogcService) {
		OGCRequest ogcRequest = ProxyUtil.separateParameters(request, ogcService);

		if (ogcRequest.isValidVendorRequest()) {
			// We can now proceed with the request. Depending on the value of
			// the layer parameter we will call the correct layer building service.
			LOG.trace("Kicking off search parameter logic for data source ["
					+ ogcRequest.getDataSource() + "]");
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
			HttpUriRequest serverRequest = generateServerRequest(clientRequest, ogcRequest);
			handleServerRequest(clientRequest, clientResponse, serverRequest, ogcRequest);
		} catch (OGCProxyException e) {
			LOG.error("Proxying client request: " + e.getMessage(), e);
			return false;
		}

		return true;
	}

	private HttpUriRequest generateServerRequest(HttpServletRequest clientRequest, final OGCRequest ogcRequest) 
			throws OGCProxyException {
		String methodName = "generateServerRequest()";
		HttpUriRequest serverRequest = null;

		try {
			// 1) Generate Server URI
			String serverRequestURIAsString = ProxyUtil.getServerRequestURIAsString(clientRequest, ogcRequest.getOgcParams(),
							ProxyService.forwardUrl, ProxyService.geoserverContext);

			LOG.trace("Request to GeoServer is: [\n" + serverRequestURIAsString + "]");

			// instantiating to URL then calling toURI gives us some error
			// checking as URI(String) appears too forgiving.
			URI serverRequestURI = new URL(serverRequestURIAsString).toURI();

			// 2 ) Create request base on client request method
			switch (clientRequest.getMethod().toUpperCase()) {
			case HttpHead.METHOD_NAME:
				serverRequest = new HttpHead(serverRequestURI);
				break;
			case HttpGet.METHOD_NAME:
				serverRequest = new HttpGet(serverRequestURI);
				break;
			case HttpPost.METHOD_NAME:
				serverRequest = new HttpPost(serverRequestURI);
				break;
			case HttpPut.METHOD_NAME:
				serverRequest = new HttpPut(serverRequestURI);
				break;
			case HttpDelete.METHOD_NAME:
				serverRequest = new HttpDelete(serverRequestURI);
				break;
			case HttpTrace.METHOD_NAME:
				serverRequest = new HttpTrace(serverRequestURI);
				break;
			case HttpOptions.METHOD_NAME:
				serverRequest = new HttpOptions(serverRequestURI);
				break;
			default:
				String msg = "Unsupported request method [" + serverRequest + "].";
				LOG.error(msg);
				OGCProxyExceptionID id = OGCProxyExceptionID.UNSUPPORTED_REQUEST_METHOD;
				throw new OGCProxyException(id, CLASSNAME, methodName, msg);
			}

			// 3) Map client request headers to server request
			ProxyUtil.generateServerRequestHeaders(clientRequest, serverRequest);

			// 4) Copy client request body to server request
			String body = ogcRequest.getRequestBody();
			int contentLength = body.length();

			if (contentLength > 0) {
				// is this a POST (or PUT)
				if (serverRequest instanceof HttpEntityEnclosingRequest) {
					try {
						HttpEntityEnclosingRequest entityRequest = (HttpEntityEnclosingRequest) serverRequest;
						HttpEntity entity = new ByteArrayEntity(body.getBytes("UTF-8"));
						entityRequest.setEntity(entity);
					} catch (IOException e) {
						String msg = "Error reading client request body [" + e.getMessage() + "].";
						LOG.error(msg, e);

						OGCProxyExceptionID id = OGCProxyExceptionID.ERROR_READING_CLIENT_REQUEST_BODY;
						throw new OGCProxyException(id, CLASSNAME, methodName, msg);
					}
				} else {
					String msg = "Content in request body unsupported for client request method [" + serverRequest.getMethod() + "].";
					LOG.error(msg);

					OGCProxyExceptionID id = OGCProxyExceptionID.UNSUPPORTED_CONTENT_FOR_REQUEST_METHOD;
					throw new OGCProxyException(id, CLASSNAME, methodName, msg);
				}
			}

		} catch (MalformedURLException e) {
			String msg = "Syntax error parsing server URL ["
					+ e.getMessage() + "].";
			LOG.error(msg, e);

			OGCProxyExceptionID id = OGCProxyExceptionID.URL_PARSING_EXCEPTION;
			throw new OGCProxyException(id, CLASSNAME, methodName, msg);
		} catch (URISyntaxException e) {
			String msg = "Syntax error parsing server URL [" + e.getMessage() + "].";
			LOG.error(msg, e);

			OGCProxyExceptionID id = OGCProxyExceptionID.URL_PARSING_EXCEPTION;
			throw new OGCProxyException(id, CLASSNAME, methodName, msg);
		}

		return serverRequest;
	}

	private void handleServerRequest(HttpServletRequest clientRequest, HttpServletResponse clientResponse,
			HttpUriRequest serverRequest, OGCRequest ogcRequest) throws OGCProxyException {
		
		try {
			HttpContext localContext = new BasicHttpContext();
			HttpResponse methodReponse = serverClient.execute(serverRequest, localContext);
			handleServerResponse(clientRequest, clientResponse, methodReponse, ogcRequest);
		} catch (ClientProtocolException e) {
			String msg = "Client protocol error ["
					+ e.getMessage() + "]";
			LOG.error(msg, e);

			OGCProxyExceptionID id = OGCProxyExceptionID.CLIENT_PROTOCOL_ERROR;
			throw new OGCProxyException(id, CLASSNAME, "handleServerRequest()", msg);
		} catch (IOException e) {
			String msg = "I/O error on server request [" + e.getMessage() + "]";
			LOG.error(msg, e);

			OGCProxyExceptionID id = OGCProxyExceptionID.SERVER_REQUEST_IO_ERROR;
			throw new OGCProxyException(id, CLASSNAME, "handleServerRequest()", msg);
		}
	}

	private void handleServerResponse(HttpServletRequest clientRequest,
			HttpServletResponse clientResponse, HttpResponse serverResponse,
			OGCRequest ogcRequest) throws OGCProxyException {
		String methodName = "handleServerResponse()";

		String clientRequestURLAsString = clientRequest.getRequestURL().toString();
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
		LOG.debug("Mapped server status code " + statusCode);

		// 2) Map server response headers to client response
		ProxyUtil.generateClientResponseHeaders(clientResponse, serverResponse);

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
					String msg = "Error obtaining input stream for server response [" + e.getMessage() + "]";
					LOG.error(msg, e);

					OGCProxyExceptionID id = OGCProxyExceptionID.SERVER_RESPONSE_INPUT_STREAM_ERROR;
					throw new OGCProxyException(id, CLASSNAME, methodName, msg);
				}

				try {
					os = clientResponse.getOutputStream();
				} catch (IOException e) {
					String msg = "Error obtaining output stream for client response [" + e.getMessage() + "]";
					LOG.error(msg, e);

					OGCProxyExceptionID id = OGCProxyExceptionID.CLIENT_RESPONSE_OUTPUT_STREAM_ERROR;
					throw new OGCProxyException(id, CLASSNAME, methodName, msg);
				}

				byte[] serverContent;
				try {
					serverContent = IOUtils.toByteArray(is);
				} catch (IOException e) {
					String msg = "Error copying server response to byteArray [" + e.getMessage() + "]";
					LOG.error(msg, e);

					OGCProxyExceptionID id = OGCProxyExceptionID.SERVER_TO_CLIENT_RESPONSE_ERROR;
					throw new OGCProxyException(id, CLASSNAME, methodName, msg);
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
					String msg = "Error copying server response to client [" + e.getMessage() + "]";
					LOG.error(msg, e);

					OGCProxyExceptionID id = OGCProxyExceptionID.SERVER_TO_CLIENT_RESPONSE_ERROR;
					throw new OGCProxyException(id, CLASSNAME, methodName, msg);
				}

			} finally {
				LOG.debug("Copied "
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
					LOG.error("Consuming remaining bytes in server response entity for proxy reponse from "
							+ clientRequestURLAsString
							+ " to "
							+ serverRequestURLAsString, e);
				}
				IOUtils.closeQuietly(is);
				IOUtils.closeQuietly(os);
			}
		} else {
			LOG.warn("Server response was empty for proxy response from "
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
		if (ProxyUtil.OGC_GET_CAPABILITIES.equalsIgnoreCase(ogcRequest.getRequestType())) {
			// This is a GetCapabilities call.  We need to include the service
			// specific GetCapabilities information in the result so we conform
			// to the OGC spec
			if (ProxyDataSourceParameter.WQP_SITES == ogcRequest.getDataSource()) {
				stringContent = addGetCapabilitiesInfo(ogcRequest.getOgcService(), stringContent);
			}
		}

		// We also need to scrub the response for any mention of the actual
		// GeoServer's location and replace it with this proxy's location.
		if (stringContent.contains(geoserverHost)) {
			LOG.trace("Server response contains references to its hostname.  Redirecting entries to the proxy...");

			String proxyServerName = clientRequest.getServerName();
			String proxyContextPath = clientRequest.getContextPath().replaceFirst("^/", "");
			String proxyPort = clientRequest.getLocalPort() + "";
			String proxyProtocol = clientRequest.getScheme();

			stringContent = ProxyUtil.redirectContentToProxy(
					stringContent, geoserverProtocol,
					proxyProtocol, geoserverHost, proxyServerName,
					geoserverPort, proxyPort, geoserverContext,
					proxyContextPath);
		} else {
			LOG.debug("Server response does not contain references to its hostname.  Continuing...");
		}

		if (contentCompressed) {
			return SystemUtils.compressStringToGzip(stringContent);
		} else {
			return stringContent.getBytes();
		}
	}

	public String addGetCapabilitiesInfo(OGCServices serviceType, String serverContent) {
		/*
		 * For now we are assuming all GetCapabilities responses are XML.
		 *
		 * We are going to take the easy way out.  Instead of creating an XML
		 * document and parsing it and figuring out where specific elements are
		 * blah blah blah, we are just going to insert our specific XML blob in
		 * the location it needs to be.
		 *
		 * The main reason I am doing this is because its fast and cheap.  A
		 * very large String of XML is smaller in memory than an entire XML DOM
		 * object of the same string.
		 */
		StringBuffer newContent = new StringBuffer();

		switch (serviceType) {
			case WMS:
				/*
				 * For WMS, our XLM blob just needs to live inside the parent
				 * <Layer> element.  Since it can live ANYWHERE in the parent
				 * <Layer></Layer> element we will just look for the LAST
				 * closing </Layer> tag and insert our stuff before it.
				 */
				int closingParentTag = serverContent.lastIndexOf("</Layer>");
				if (closingParentTag == -1) {
					LOG.warn("WMS GetCapabilities response from mapping service does not contain a closing </Layer> element.  Returning silently...");
					return serverContent;
				}

				newContent.append(serverContent.substring(0, closingParentTag));
				newContent.append(WMS_GET_CAPABILITIES_CONTENT);
				newContent.append(serverContent.substring(closingParentTag, serverContent.length()));
				break;

			case WFS:
				/*
				 * WFS GetCapabilities response is different than WMS.  Most of our
				 * WFS requests will center around GetFeature so we need to add
				 * the "searchParams" parameter definition to the GetFeature operation
				 * description.
				 *
				 * We will look for string token: "<ows:Operation name="GetFeature">"
				 * and then look for the closing "</ows:DCP>" tag (a required child
				 * element for an operation http://schemas.opengis.net/ows/1.1.0/owsOperationsMetadata.xsd).
				 *
				 * Once we found the closing </ows:DCP> tag of the GetFeature operation, we insert
				 * our XML after it and append the rest of the document below it.  If we dont find
				 * this tag we'll just insert it right before the closing </ows:Operation> tag.
				 */
				int getFeatureTag = serverContent.lastIndexOf("<ows:Operation name=\"GetFeature\">");
				if (getFeatureTag == -1) {
					LOG.warn("WFS GetCapabilities response from mapping service does not contain a <ows:Operation name=\"GetFeature\"> element.  Returning silently...");
					return serverContent;
				}

				int insertTag = serverContent.indexOf("</ows:DCP>", getFeatureTag);
				if (insertTag == -1) {
					LOG.warn("WFS GetCapabilities response from mapping service does not contain a closing </ows:DCP> element from the location of the <ows:Operation name=\"GetFeature\"> tag.  Looking for closing Operation tag.");

					insertTag = serverContent.indexOf("</ows:Operation>", getFeatureTag);
					if (insertTag == -1) {
						LOG.warn("WFS GetCapabilities response from mapping service does not contain a closing </ows:Operation> element from the location of the <ows:Operation name=\"GetFeature\"> tag.  Returning silently...");
						return serverContent;
					}
				} else {
					insertTag += "</ows:DCP>".length();
				}

				newContent.append(serverContent.substring(0, insertTag));
				newContent.append(WFS_GET_CAPABILITIES_CONTENT);
				newContent.append(serverContent.substring(insertTag, serverContent.length()));
				break;

			default:
				break;
		}

		return newContent.toString();
	}

	/**
	 * Really only meant to be used by automated tests - Spring will normally handle it with the @Autowired annotation.
	 * @param environment
	 */
	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}

	/**
	 * Really only meant to be used by automated tests - Spring will normally handle it with the @Autowired annotation.
	 * @param layerCachingService
	 */
	public void setWQPDynamicLayerCachingService(WQPDynamicLayerCachingService layerCachingService) {
		this.layerCachingService = layerCachingService;
	}

}
