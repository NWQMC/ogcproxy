package gov.usgs.wqp.ogcproxy.services;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
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
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.FileCopyUtils;

import gov.usgs.wqp.ogcproxy.exceptions.OGCProxyException;
import gov.usgs.wqp.ogcproxy.exceptions.OGCProxyExceptionID;
import gov.usgs.wqp.ogcproxy.model.DynamicLayer;
import gov.usgs.wqp.ogcproxy.model.OGCRequest;
import gov.usgs.wqp.ogcproxy.model.ogc.parameters.WMSParameters;
import gov.usgs.wqp.ogcproxy.model.ogc.services.OGCServices;
import gov.usgs.wqp.ogcproxy.model.parameters.ProxyDataSourceParameter;
import gov.usgs.wqp.ogcproxy.services.wqp.WQPDynamicLayerCachingService;
import gov.usgs.wqp.ogcproxy.utils.CloseableHttpClientFactory;
import gov.usgs.wqp.ogcproxy.utils.ProxyUtil;
import gov.usgs.wqp.ogcproxy.utils.SystemUtils;

public class ProxyService {

	@Autowired
	protected CloseableHttpClientFactory closeableHttpClientFactory;

	@Autowired
	protected WQPDynamicLayerCachingService layerCachingService;

	@Autowired
	protected ConfigurationService configurationService;

	private CloseableHttpClient serverClient;

	private static final Logger LOG = LoggerFactory.getLogger(ProxyService.class);
	private static final String CLASSNAME = ProxyService.class.getName();

	private volatile boolean initialized; 
	
	public static String WMS_GET_CAPABILITIES_1_3_0_CONTENT;
	
	public static String WMS_GET_CAPABILITIES_1_1_1_CONTENT;
	
	public static String WFS_GET_CAPABILITIES_1_0_0_CONTENT;
	
	public static String WFS_GET_CAPABILITIES_2_0_0_CONTENT;

	private static final ProxyService INSTANCE = new ProxyService();

	/**
	 * Private Constructor for Singleton Pattern
	 */
	private ProxyService() {
		try {
			WMS_GET_CAPABILITIES_1_3_0_CONTENT = new String(FileCopyUtils.copyToByteArray(new ClassPathResource("schemas/wms/capabilities/GetCapabilities.1.3.0.xml").getInputStream()));
			WMS_GET_CAPABILITIES_1_1_1_CONTENT = new String(FileCopyUtils.copyToByteArray(new ClassPathResource("schemas/wms/capabilities/GetCapabilities.1.1.1.xml").getInputStream()));
			WFS_GET_CAPABILITIES_1_0_0_CONTENT = new String(FileCopyUtils.copyToByteArray(new ClassPathResource("schemas/wfs/capabilities/GetCapabilities.1.0.0.xml").getInputStream()));
			WFS_GET_CAPABILITIES_2_0_0_CONTENT = new String(FileCopyUtils.copyToByteArray(new ClassPathResource("schemas/wfs/capabilities/GetCapabilities.2.0.0.xml").getInputStream()));
		} 
		catch (IOException e) {
			LOG.error("Unexpected exception reading file: " + e.getLocalizedMessage());
		}
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

			serverClient = closeableHttpClientFactory.getUnauthorizedCloseableHttpClient(true);
		}
	}

	@PreDestroy
	public void tearDown() {
		try {
			serverClient.close();
			LOG.info("Closed serverClient");
		} catch (IOException e) {
			LOG.error("Issue trying to close serverClient:" + e.getLocalizedMessage(), e);
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
		//TODO - only attempt to contact GeoServer if we have a valid feature type!!!
		boolean proxySuccess = proxyRequest(request, response, ogcRequest);

		if (proxySuccess) {
			LOG.trace("Proxy request is completed successfully.");
		} else {
			LOG.error("Unable to proxy client request.");
		}
	}

	protected OGCRequest convertVendorParms(HttpServletRequest request, OGCServices ogcService) {
		OGCRequest ogcRequest = ProxyUtil.separateParameters(request, ogcService);
		LOG.info("Parameters: " + ogcRequest.getSearchParams() + " :layer key:" + DynamicLayer.buildLayerName(ogcRequest));

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
					configurationService.getGeoserverBaseURI());
			String clientRequestMethod = clientRequest.getMethod().toUpperCase();

			LOG.trace("Request to GeoServer is: [\n" + serverRequestURIAsString + "]");

			// instantiating to URL then calling toURI gives us some error
			// checking as URI(String) appears too forgiving.
			URI serverRequestURI = new URL(serverRequestURIAsString).toURI();

			// 2 ) Create request base on client request method
			switch (clientRequestMethod) {
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
				String msg = "Unsupported request method [" + clientRequestMethod + "].";
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

		} catch (MalformedURLException | URISyntaxException e) {
			String msg = "Syntax error parsing server URL ["
					+ e.getMessage() + "].";
			LOG.error(msg, e);

			OGCProxyExceptionID id = OGCProxyExceptionID.URL_PARSING_EXCEPTION;
			throw new OGCProxyException(id, CLASSNAME, methodName, msg);
		} 

		return serverRequest;
	}

	private void handleServerRequest(HttpServletRequest clientRequest, HttpServletResponse clientResponse,
			HttpUriRequest serverRequest, OGCRequest ogcRequest) throws OGCProxyException {
		HttpContext localContext = new BasicHttpContext();
		CloseableHttpResponse methodResponse = null;
		try {
			methodResponse = serverClient.execute(serverRequest, localContext);
			handleServerResponse(clientRequest, clientResponse, serverRequest, methodResponse, ogcRequest);
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
		} finally {
			try {
				if (null != methodResponse) {
					methodResponse.close();
				}
			} catch (IOException e1) {
				LOG.error("Trouble closing geoserver response", e1);
			}
		}
	}

	private void handleServerResponse(HttpServletRequest clientRequest, HttpServletResponse clientResponse,
			HttpUriRequest serverRequest, HttpResponse serverResponse,
			OGCRequest ogcRequest) throws OGCProxyException {
		String methodName = "handleServerResponse()";

		String clientRequestURLAsString = clientRequest.getRequestURL().toString();
		String serverRequestURLAsString = serverRequest.getURI().toString();

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

			boolean contentCompressed = false;

			if (methodEntity.getContentEncoding() != null) {
				String contentEncoding = methodEntity.getContentEncoding().getValue();
				if ( "gzip".equalsIgnoreCase(contentEncoding) ) {
					contentCompressed = true;
				}
			}

			long responseBytes = 0;

			try (InputStream is = methodEntity.getContent();
					OutputStream os = clientResponse.getOutputStream()) {
				// !!! Are you here to edit this to enable response body content rewrite?
				// You may want to remove or edit the "Content-Length" header !!!

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
				String contentType = null == methodEntity.getContentType() ? null : methodEntity.getContentType().getValue();
				if ((contentType != null) && (contentType.toLowerCase().contains("xml"))) {
					inspectedBytes = inspectServerContent(clientRequest, serverRequest, ogcRequest, serverContent, contentCompressed);

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

				} catch (IOException e) {
				String msg = "Error obtaining input stream for server response or output stream for client response [" + e.getMessage() + "]";
				LOG.error(msg, e);

				OGCProxyExceptionID id = OGCProxyExceptionID.SERVER_RESPONSE_INPUT_STREAM_ERROR;
				throw new OGCProxyException(id, CLASSNAME, methodName, msg);

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
			}
		} else {
			LOG.warn("Server response was empty for proxy response from "
					+ clientRequestURLAsString
					+ " to "
					+ serverRequestURLAsString);
		}
	}

	protected byte[] inspectServerContent(HttpServletRequest clientRequest, HttpUriRequest serverRequest, OGCRequest ogcRequest,
			byte[] serverContent, boolean contentCompressed) throws OGCProxyException {
		String stringContent = "";
		String request = ogcRequest.getRequestType();
		
		if (contentCompressed) {
			stringContent = SystemUtils.uncompressGzipAsString(serverContent);
		} else {
			try {
				stringContent = new String(serverContent, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				throw new OGCProxyException(null, CLASSNAME, "inspectServerContent", e.getLocalizedMessage());
			}
		}

		// We now need to do some inspection on the data.  If the original OGC
		// request is a GetCapabilities, we need to insert the service's specific
		// information into the response.
		if (ProxyUtil.OGC_GET_CAPABILITIES.equalsIgnoreCase(request)) {
			// This is a GetCapabilities call.  We need to include the service
			// specific GetCapabilities information in the result so we conform
			// to the OGC spec
			stringContent = addGetCapabilitiesInfo(ogcRequest.getOgcService(), ogcRequest.getOgcParams().getOrDefault(WMSParameters.version.toString(), ""), stringContent);
		} else if (ProxyUtil.OGC_DESCRIBE_FEATURE_TYPE.equalsIgnoreCase(request) && ProxyDataSourceParameter.WQP_SITES == ogcRequest.getDataSource()) {
			// We need to replace the actual layer name with wqp_sites
			stringContent = stringContent.replaceAll("dynamicSites_\\d+", ProxyDataSourceParameter.WQP_SITES.toString());
			
		}

		String serverHost = serverRequest.getURI().getHost();
		// We also need to scrub the response for any mention of the actual
		// GeoServer's location and replace it with this proxy's location.
		if (stringContent.contains(serverHost)) {
			LOG.trace("Server response contains references to its hostname.  Redirecting entries to the proxy...");

			String proxyHost = clientRequest.getServerName();
			String proxyContext = clientRequest.getContextPath().replaceFirst("^/", "");
			String proxyPort = clientRequest.getLocalPort() + "";
			String proxyProtocol = clientRequest.getScheme();

			String serverContext = serverRequest.getURI().getPath().replaceFirst("^/", "").split("/")[0];
			String serverPort = serverRequest.getURI().getPort() + "";
			String serverProtocol = serverRequest.getURI().getScheme();

			stringContent = ProxyUtil.redirectContentToProxy(stringContent,
					serverProtocol, proxyProtocol,
					serverHost, proxyHost,
					serverPort, proxyPort,
					serverContext, proxyContext);
		} else {
			LOG.debug("Server response does not contain references to its hostname.  Continuing...");
		}

		if (contentCompressed) {
			return SystemUtils.compressStringToGzip(stringContent);
		} else {
			try {
				return stringContent.getBytes("UTF-8");
			} catch (UnsupportedEncodingException e) {
				throw new OGCProxyException(null, CLASSNAME, "inspectServerContent", e.getLocalizedMessage());
			}
		}
	}
	
	private String getDataSourceGetCapabilitiesClosingTag(OGCServices serviceType) {
		String result = null;
		switch (serviceType) {
			case WMS:
				result = "</Layer>";
				break;
			case WFS:
				result = "</FeatureTypeList>";
				break;
		}
		return result;
	}
	
	private String getProxyDataSourceGetCapabilities(OGCServices serviceType, String version) {
		String result = null;
		switch (serviceType) {
			case WMS:
				switch (version) {
					case "1.3.0":
					case "":
						result = WMS_GET_CAPABILITIES_1_3_0_CONTENT;
						break;
						
					default:
						result = WMS_GET_CAPABILITIES_1_1_1_CONTENT;
				}
				break;
			case WFS:
				switch (version) {
					case "1.0.0":
						result = WFS_GET_CAPABILITIES_1_0_0_CONTENT;
						break;
					default:
						result = WFS_GET_CAPABILITIES_2_0_0_CONTENT;
				}
				break;
		}
		return result;
	};	

	public String addGetCapabilitiesInfo(OGCServices serviceType, String version, String serverContent) {
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
		StringBuilder newContent = new StringBuilder();
		String closingTag = getDataSourceGetCapabilitiesClosingTag(serviceType);
		String addedContent = getProxyDataSourceGetCapabilities(serviceType, version);
		
		if ((null != closingTag) && (null != addedContent)) {
			int closingTagIndex = serverContent.lastIndexOf(closingTag);
			if (closingTagIndex == -1) {
				LOG.warn("WMS GetCapabilities response from mapping service does not contain a closing </Layer> element.  Returning silently...");
				newContent.append(serverContent);
			} else {
				newContent.append(serverContent.substring(0, closingTagIndex));
				newContent.append(addedContent);
				newContent.append(serverContent.substring(closingTagIndex, serverContent.length()));
			}
		}
		return newContent.toString();
	}
}
