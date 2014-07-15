package gov.usgs.wqp.ogcproxy.services;

import gov.usgs.wqp.ogcproxy.exceptions.WMSProxyException;
import gov.usgs.wqp.ogcproxy.exceptions.WMSProxyExceptionID;
import gov.usgs.wqp.ogcproxy.model.ogc.parameters.WFSParameters;
import gov.usgs.wqp.ogcproxy.model.ogc.parameters.WMSParameters;
import gov.usgs.wqp.ogcproxy.model.ogc.services.OGCServices;
import gov.usgs.wqp.ogcproxy.model.parameters.ProxyDataSourceParameter;
import gov.usgs.wqp.ogcproxy.model.parameters.SearchParameters;
import gov.usgs.wqp.ogcproxy.services.wqp.WQPLayerBuildingService;
import gov.usgs.wqp.ogcproxy.utils.ProxyUtil;
import gov.usgs.wqp.ogcproxy.utils.ProxyUtil.ProxyServiceResult;
import gov.usgs.wqp.ogcproxy.utils.SystemUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpTrace;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.SchemeRegistryFactory;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.async.DeferredResult;


@Service
public class ProxyService {
	private static Logger log = SystemUtils.getLogger(ProxyService.class);
	
	/*
	 * Beans		===========================================================
	 * ========================================================================
	 */
	@Autowired
	private Environment environment;
	
	@Autowired
	private WQPLayerBuildingService wqpLayerBuildingService;
	/* ====================================================================== */
	
	/*
	 * Static Local		=======================================================
	 * ========================================================================
	 */
	/* ====================================================================== */
	private static boolean initialized = false;
	private static long threadTimeout = 604800000;			// 1000 * 60 * 60 * 24 * 7 (1 week)
	private static long threadSleep = 500;
	private static String geoserverProtocol = "http://";
	private static String geoserverHost = "localhost";
	private static String geoserverPort = "8081";
	private static String forwardUrl = "http://localhost:8081";
	private static String geoserverContext = "/geoserver";
	private static String geoserverWorkspace = "qw_portal_map";
	
	private static int connection_ttl = 15 * 60 * 1000;       // 15 minutes, default is infinte
    private static int connections_max_total = 256;
    private static int connections_max_route = 32;
    private static int client_socket_timeout = 5 * 60 * 1000; // 5 minutes, default is infinite
    private static int client_connection_timeout = 15 * 1000; // 15 seconds, default is infinte
    
    private static boolean layerPassthrough = false;
	
	protected ThreadSafeClientConnManager clientConnectionManager;
    protected HttpClient httpClient;
	private static Set<String> ignoredClientRequestHeaderSet;
	private static Set<String> ignoredServerResponseHeaderSet;
	/* ====================================================================== */
	
	/*
	 * Local		===========================================================
	 * ========================================================================
	 */
	/* ====================================================================== */
	
	/* ====================================================================== */
	
	/*
	 * INSTANCE		===========================================================
	 * ========================================================================
	 */
	/* ====================================================================== */
	private static final ProxyService INSTANCE = new ProxyService();
	/* ====================================================================== */
	
	/**
	 * Private Constructor for Singleton Pattern
	 */
    private ProxyService() {
    	initialized = false;
    	ignoredClientRequestHeaderSet = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
    	ignoredServerResponseHeaderSet = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
    }
    
    /**
     * Singleton accessor
     * 
     * @return ProxyService instance
     */
	public static ProxyService getInstance() {
        return INSTANCE;
    }
	
	public void initialize() {
		/**
		 * Since we are using Spring DI we cannot access the environment bean 
		 * in the constructor.  We'll just use a locked initialized variable
		 * to check initialization after instantiation and set the env
		 * properties here.
		 */
		if(!initialized) {
			synchronized(ProxyService.class) {
				if(!initialized) {					
					try {
						threadTimeout = Long.parseLong(environment.getProperty("proxy.thread.timeout"));
			    	} catch (Exception e) {
			    		log.error("ProxyService() Constructor Exception: Failed to parse property [proxy.thread.timeout] " +
			    				  "- Keeping cache timeout period default to [" + threadTimeout + "].\n" + e.getMessage() + "\n");
			    	}
					
					try {
						threadSleep = Long.parseLong(environment.getProperty("proxy.thread.sleep"));
			    	} catch (Exception e) {
			    		log.error("ProxyService() Constructor Exception: Failed to parse property [proxy.thread.sleep] " +
			    				  "- Keeping thread sleep default to [" + threadSleep + "].\n" + e.getMessage() + "\n");
			    	}
					
					try {
						String defaultProto = geoserverProtocol;
						geoserverProtocol = environment.getProperty("wqp.geoserver.proto");
				        if ((geoserverProtocol == null) || (geoserverProtocol.equals(""))) {
				        	log.error("ProxyService() Constructor Exception: Failed to parse property [wqp.geoserver.proto] " +
				    				  "- Setting geoserver protocol default to [" + defaultProto + "].\n");
				        	geoserverProtocol = defaultProto;
				        }
					} catch (Exception e) {
			    		log.error("ProxyService() Constructor Exception: Failed to parse property [wqp.geoserver.proto] " +
			    				  "- Setting geoserver protocol default to [" + geoserverProtocol + "].\n");
			    	}
					
					try {
						String defaultHost = geoserverHost;
						geoserverHost = environment.getProperty("wqp.geoserver.host");
				        if ((geoserverHost == null) || (geoserverHost.equals(""))) {
				        	log.error("ProxyService() Constructor Exception: Failed to parse property [wqp.geoserver.host] " +
				    				  "- Setting geoserver host default to [" + defaultHost + "].\n");
				        	geoserverHost = defaultHost;
				        }
					} catch (Exception e) {
			    		log.error("ProxyService() Constructor Exception: Failed to parse property [wqp.geoserver.host] " +
			    				  "- Setting geoserver host default to [" + geoserverHost + "].\n");
			    	}
					
					try {
						String defaultPort = geoserverPort;
						geoserverPort = environment.getProperty("wqp.geoserver.port");
				        if ((geoserverPort == null) || (geoserverPort.equals(""))) {
				        	log.error("ProxyService() Constructor Exception: Failed to parse property [wqp.geoserver.port] " +
				    				  "- Setting geoserver port default to [" + defaultPort + "].\n");
				        	geoserverPort = defaultPort;
				        }
					} catch (Exception e) {
			    		log.error("ProxyService() Constructor Exception: Failed to parse property [wqp.geoserver.port] " +
			    				  "- Setting geoserver port default to [" + geoserverPort + "].\n");
			    	}
					
					forwardUrl = geoserverProtocol + "://" + geoserverHost + ":" + geoserverPort;
					log.info("ProxyService() Constructor Info: Setting GeoServer forwarding URL to [" + forwardUrl + "]");
					
					try {
						String defaultContext = geoserverContext;
						geoserverContext = environment.getProperty("wqp.geoserver.context");
				        if ((geoserverContext == null) || (geoserverContext.equals(""))) {
				        	log.error("ProxyService() Constructor Exception: Failed to parse property [wqp.geoserver.context] " +
				    				  "- Setting geoserver context default to [" + defaultContext + "].\n");
				        	geoserverContext = defaultContext;
				        }
					} catch (Exception e) {
			    		log.error("ProxyService() Constructor Exception: Failed to parse property [wqp.geoserver.context] " +
			    				  "- Setting geoserver context default to [" + geoserverContext + "].\n");
			    	}
					
					try {
						String defaultWorkspace = geoserverWorkspace;
						geoserverWorkspace = environment.getProperty("wqp.geoserver.workspace");
				        if ((geoserverWorkspace == null) || (geoserverWorkspace.equals(""))) {
				        	log.error("ProxyService() Constructor Exception: Failed to parse property [wqp.geoserver.workspace] " +
				    				  "- Setting geoserver workspace default to [" + defaultWorkspace + "].\n");
				        	geoserverWorkspace = defaultWorkspace;
				        }
					} catch (Exception e) {
			    		log.error("ProxyService() Constructor Exception: Failed to parse property [wqp.geoserver.workspace] " +
			    				  "- Setting geoserver workspace default to [" + geoserverWorkspace + "].\n");
			    	}
					
					/**
					 * Set the passthrough behavior.  If a searchParam request comes through and the layer parameter is NOT set to
					 * a "known" value, we can either pass it through and work as expected or return an error
					 */
					try {
						layerPassthrough = Boolean.parseBoolean(environment.getProperty("proxy.layerparam.passthrough"));
			    	} catch (Exception e) {
			    		log.error("ProxyService() Constructor Exception: Failed to parse property [proxy.layerparam.passthrough] " +
			    				  "- Proxy Layer Passthrough setting default to [" + layerPassthrough + "].\n" + e.getMessage() + "\n");
			    	}
					
					
					// Initialize connection manager, this is thread-safe.  if we use this
			        // with any HttpClient instance it becomes thread-safe.
			        clientConnectionManager = new ThreadSafeClientConnManager(SchemeRegistryFactory.createDefault(), connection_ttl, TimeUnit.MILLISECONDS);
			        clientConnectionManager.setMaxTotal(connections_max_total);
			        clientConnectionManager.setDefaultMaxPerRoute(connections_max_route);
			        
			        HttpParams httpParams = new BasicHttpParams();
			        HttpConnectionParams.setSoTimeout(httpParams, client_socket_timeout);
			        HttpConnectionParams.setConnectionTimeout(httpParams, client_connection_timeout);

			        httpClient = new DefaultHttpClient(clientConnectionManager, httpParams);
			    	
			        // Ignored headers relating to proxing requests
			        ignoredClientRequestHeaderSet.add("host");          // don't parameterize, need to swtich host from proxy to server
			        ignoredClientRequestHeaderSet.add("connection");    // don't parameterize, let proxy to server call do it's own handling
			        ignoredClientRequestHeaderSet.add("cookie");        // parameterize (cookies passthru?)
			        ignoredClientRequestHeaderSet.add("authorization"); // parameterize (authorization passthru?)
			        ignoredClientRequestHeaderSet.add("content-length");// ignore header in request, this is set in client call.
			        
			        // Ignored headers relating to proxing reponses.
			        ignoredServerResponseHeaderSet.add("transfer-encoding");// don't parameterize
			        ignoredServerResponseHeaderSet.add("keep-alive");       // don't parameterize
			        ignoredServerResponseHeaderSet.add("set-cookie");       // parameterize (cookies passthru?)
			        ignoredServerResponseHeaderSet.add("authorization");    // parameterize (authorization passthru?)
			        //ignoredServerResponseHeaderSet.add("content-length");   // allow for now, NOTE: are you doing response body content rewrite?
			        
			    	initialized = true;
				}
			}
		}
	}
	
	public void reinitialize() {
		initialized = false;
		initialize();
	}
	
	public void performProxyRequest(HttpServletRequest request, HttpServletResponse response, Map<String,String> requestParams, DeferredResult<String> finalResult) {
		if(!initialized) {
			initialize();
		}
		
		String result = "success";
		if(!proxyRequest(request, response, requestParams)) {
			log.error("ProxyService.performProxyRequest() Error:  Unable to proxy client request.");
			result = "failed";
		}

		finalResult.setResult(result);
	}
	
	/**
	 * performWMSRequest()
	 * @param request
	 * @param response
	 * @param requestParams
	 * @param finalResult
	 * 
	 * This method provides WMS request proxying along with intercepting WMS 
	 * requests and providing additional functionality based on the filters 
	 * found in the request parameters.
	 */
	public void performWMSRequest(HttpServletRequest request, HttpServletResponse response, Map<String,String> requestParams, DeferredResult<String> finalResult) {
		if(!initialized) {
			initialize();
		}
				
		Map<String,String> wmsParams = new HashMap<String, String>();
		Map<String, List<String>> searchParams = new SearchParameters<String, List<String>>();
		
		ProxyUtil.separateParameters(requestParams, wmsParams, searchParams);		
		
		if(searchParams.size() > 0) {
			List<String> layerParams = new Vector<String>();
			
			/**
			 * We have a create-layer request.  Lets see if the layers and/or
			 * queryParameter parameter 
			 * is what we are expecting and decide what to do depending on its
			 * value.
			 */
			String layerParam = wmsParams.get(WMSParameters.getStringFromType(WMSParameters.layers));
			ProxyDataSourceParameter layerValue = ProxyDataSourceParameter.UNKNOWN;
			if((layerParam != null) && (!layerParam.equals(""))) {
				layerValue = ProxyDataSourceParameter.getTypeFromString(layerParam);
				
				if(layerValue != ProxyDataSourceParameter.UNKNOWN) {
					layerParams.add(WMSParameters.getStringFromType(WMSParameters.layers));
				}
			}
			
			String queryLayerParam = wmsParams.get(WMSParameters.getStringFromType(WMSParameters.query_layers));
			ProxyDataSourceParameter queryLayerValue = ProxyDataSourceParameter.UNKNOWN;
			if((queryLayerParam != null) && (!queryLayerParam.equals(""))) {
				queryLayerValue = ProxyDataSourceParameter.getTypeFromString(layerParam);
				
				if(queryLayerValue != ProxyDataSourceParameter.UNKNOWN) {
					layerParams.add(WMSParameters.getStringFromType(WMSParameters.query_layers));
				}
			}
			
			/**
			 * Did we find a legitimate layer value or do we need to return an error?
			 */
			if((layerParams.size() == 0) && (!layerPassthrough)) {
				finalResult.setResult(ProxyUtil.PROXY_LAYER_ERROR);
				return;
			}
			
			/**
			 * We can now proceed with the request.  Depending on the value of
			 * the layer parameter we will call the correct layer building
			 * service.
			 */
			log.info("ProxyService.performWMSRequest() Info: Kicking off search parameter logic for layer [" + ProxyDataSourceParameter.getStringFromType(layerValue) + "]");
			switch(layerValue) {
				case wqp_sites: {
					ProxyServiceResult result = wqpLayerBuildingService.getDynamicLayer(wmsParams, (SearchParameters<String, List<String>>) searchParams, layerParams, OGCServices.WMS, ProxyDataSourceParameter.wqp_sites);
					
					if(result != ProxyServiceResult.SUCCESS) {
						finalResult.setResult(ProxyUtil.getErrorViewByFormat(wmsParams.get(WMSParameters.getStringFromType(WMSParameters.format))));
						return;
					}
					break;
				}
				default: {
					break;
				}
			}
			
		}
		
		/**
		 * We now need to perform the proxy call to the GeoServer and return the
		 * result to the client
		 */
		String result = "success";
		if(!proxyRequest(request, response, wmsParams)) {
			log.error("ProxyService.performWMSRequest() Error:  Unable to proxy client request.");
			result = "failed";
		}
		
		log.info("ProxyService.performWMSRequest() INFO: Proxy request is completed.");
		finalResult.setResult(result);
    }
	
	/**
	 * performWFSRequest
	 * @param request
	 * @param response
	 * @param requestParams
	 * @param finalResult
	 * 
	 * As of initial release, this method provides the same functionality as the
	 * WMS proxying provides, including building a layer if the passed in search
	 * params do not exist.
	 * 
	 * The main reason we called out a different method for WFS calls is the 
	 * possibility of doing completely different logic based on this type of
	 * call.
	 */
	public void performWFSRequest(HttpServletRequest request, HttpServletResponse response, Map<String,String> requestParams, DeferredResult<String> finalResult) {
		if(!initialized) {
			initialize();
		}
		
		Map<String,String> wfsParams = new HashMap<String, String>();
		Map<String, List<String>> searchParams = new SearchParameters<String, List<String>>();
		
		ProxyUtil.separateParameters(requestParams, wfsParams, searchParams);
		
		if(searchParams.size() > 0) {
			List<String> layerParams = new Vector<String>();
			
			/**
			 * We have a create-layer request.  Lets see if the typeName and/or 
			 * typeNames parameters are what we are expecting and decide what to
			 * do depending on its value.
			 */
			String typeNamesParam = wfsParams.get(WFSParameters.getStringFromType(WFSParameters.typeNames));
			ProxyDataSourceParameter typeNamesValue = ProxyDataSourceParameter.UNKNOWN;
			if((typeNamesParam != null) && (!typeNamesParam.equals(""))) {
				typeNamesValue = ProxyDataSourceParameter.getTypeFromString(typeNamesParam);
				
				if(typeNamesValue != ProxyDataSourceParameter.UNKNOWN) {
					layerParams.add(WFSParameters.getStringFromType(WFSParameters.typeNames));
				}
			}
			
			String typeNameParam = wfsParams.get(WFSParameters.getStringFromType(WFSParameters.typeName));
			if((typeNameParam != null) && (!typeNameParam.equals(""))) {
				typeNamesValue = ProxyDataSourceParameter.getTypeFromString(typeNameParam);
				
				if(typeNamesValue != ProxyDataSourceParameter.UNKNOWN) {
					layerParams.add(WFSParameters.getStringFromType(WFSParameters.typeName));
				}
			}
			
			/**
			 * Did we find a legitimate layer value or do we need to return an error?
			 */
			if((layerParams.size() == 0) && (!layerPassthrough)) {
				finalResult.setResult(ProxyUtil.PROXY_LAYER_ERROR);
				return;
			}
			
			/**
			 * We can now proceed with the request.  Depending on the value of
			 * the layer parameter we will call the correct layer building
			 * service.
			 */
			log.info("ProxyService.performWFSRequest() Info: Kicking off search parameter logic for layer [" + ProxyDataSourceParameter.getStringFromType(typeNamesValue) + "]");
			switch(typeNamesValue) {
				case wqp_sites: {
					ProxyServiceResult result = wqpLayerBuildingService.getDynamicLayer(wfsParams, (SearchParameters<String, List<String>>) searchParams, layerParams, OGCServices.WFS, ProxyDataSourceParameter.wqp_sites);
					if(result != ProxyServiceResult.SUCCESS) {
						finalResult.setResult(ProxyUtil.getErrorViewByFormat(wfsParams.get(WMSParameters.getStringFromType(WMSParameters.format))));
						return;
					}
					break;
				}
				default: {
					break;
				}
			}
		}
		
		/**
		 * We need to perform the proxy call to the GeoServer and return the
		 * result to the client
		 */
		String result = "success";
		if(!proxyRequest(request, response, wfsParams)) {
			log.error("ProxyService.performWFSRequest() Error:  Unable to proxy client request.");
			result = "failed";
		}
		
		log.info("ProxyService.performWFSRequest() INFO: Proxy request is completed.");
		finalResult.setResult(result);
	}
	
	/**
	 * Logic and supporting methods stolen from gov.usgs.cida.proxy.AlternateProxyServlet
	 * @param clientRequest
	 * @param clientResponse
	 * @param requestParams
	 * @return
	 */
	private boolean proxyRequest(HttpServletRequest clientRequest, HttpServletResponse clientResponse, Map<String,String> requestParams) {
        try {
            HttpUriRequest serverRequest = generateServerRequest(clientRequest, requestParams);
            handleServerRequest(clientRequest, clientResponse, serverRequest, requestParams);
        } catch (WMSProxyException e) {
            log.error("ProxyService.proxyRequest() Error: proxying client request: " + e.getMessage());
            return false;
        }
        
        return true;
    }
	
	private HttpUriRequest generateServerRequest(HttpServletRequest clientRequest, final Map<String,String> wmsParams) throws WMSProxyException {
        HttpUriRequest serverRequest = null;
        try {

            // 1) Generate Server URI
            String serverRequestURIAsString = ProxyUtil.getServerRequestURIAsString(clientRequest, wmsParams, ProxyService.forwardUrl, ProxyService.geoserverContext);
            
            log.info("ProxyService.generateServerRequest(): request to GeoServer is: [\n" + serverRequestURIAsString + "]");
            
            // instantiating to URL then calling toURI gives us some error
            // checking as URI(String) appears too forgiving.
            URI serverRequestURI = (new URL(serverRequestURIAsString)).toURI();

            // 2 ) Create request base on client request method
            String clientRequestMethod = clientRequest.getMethod();
            if ("HEAD".equals(clientRequestMethod)) {
                serverRequest = new HttpHead(serverRequestURI);
            } else if ("GET".equals(clientRequestMethod)) {
                serverRequest = new HttpGet(serverRequestURI);
            } else if ("POST".equals(clientRequestMethod)) {
                serverRequest = new HttpPost(serverRequestURI);
            } else if ("PUT".equals(clientRequestMethod)) {
                serverRequest = new HttpPut(serverRequestURI);
            } else if ("DELETE".equals(clientRequestMethod)) {
                serverRequest = new HttpDelete(serverRequestURI);
            } else if ("TRACE".equals(clientRequestMethod)) {
                serverRequest = new HttpTrace(serverRequestURI);
            } else if ("OPTIONS".equals(clientRequestMethod)) {
                serverRequest = new HttpOptions(serverRequestURI);
            } else {
            	String msg = "ProxyService.generateServerRequest() Exception : Unsupported request method [" +
            				  serverRequest + "].";
				log.error(msg);
				
				WMSProxyExceptionID id = WMSProxyExceptionID.UNSUPPORTED_REQUEST_METHOD;					
				throw new WMSProxyException(id, "ProxyService", "generateServerRequest()", msg);
            }

            // 3) Map client request headers to server request
            ProxyUtil.generateServerRequestHeaders(clientRequest, serverRequest, ProxyService.ignoredClientRequestHeaderSet);

            // 4) Copy client request body to server request
            int contentLength = clientRequest.getContentLength();
            if (contentLength > 0) {
                if (serverRequest instanceof HttpEntityEnclosingRequest) {
                    try {
                        // !!! Are you here to edit this to enable request body content rewrite?
                        //     You may want to remove or edit the "Content-Length" header !!!
                        InputStreamEntity serverRequestEntity = new InputStreamEntity(
                                clientRequest.getInputStream(),
                                contentLength);
                        serverRequestEntity.setContentType(clientRequest.getContentType());
                        ((HttpEntityEnclosingRequest) serverRequest).setEntity(serverRequestEntity);
                    } catch (IOException e) {
                        String msg = "ProxyService.generateServerRequest() Exception : Error reading client request body [" +
              				  e.getMessage() + "].";
		  				log.error(msg);
		  				
		  				WMSProxyExceptionID id = WMSProxyExceptionID.ERROR_READING_CLIENT_REQUEST_BODY;					
		  				throw new WMSProxyException(id, "ProxyService", "generateServerRequest()", msg);
                    }
                } else {
                	String msg = "ProxyService.generateServerRequest() Exception : Content in request body unsupported for client request method [" +
                				 serverRequest.getMethod() + "].";
	  				log.error(msg);
	  				
	  				WMSProxyExceptionID id = WMSProxyExceptionID.UNSUPPORTED_CONTENT_FOR_REQUEST_METHOD;					
	  				throw new WMSProxyException(id, "ProxyService", "generateServerRequest()", msg);
                }
            }

        } catch (MalformedURLException e) {
        	String msg = "ProxyService.generateServerRequest() Exception : Syntax error parsing server URL [" +
    				  e.getMessage() + "].";
			log.error(msg);
			
			WMSProxyExceptionID id = WMSProxyExceptionID.URL_PARSING_EXCEPTION;					
			throw new WMSProxyException(id, "ProxyService", "generateServerRequest()", msg);
        } catch (URISyntaxException e) {
        	String msg = "ProxyService.generateServerRequest() Exception : Syntax error parsing server URL [" +
  				  e.getMessage() + "].";
			log.error(msg);
			
			WMSProxyExceptionID id = WMSProxyExceptionID.URL_PARSING_EXCEPTION;					
			throw new WMSProxyException(id, "ProxyService", "generateServerRequest()", msg);
        }

        return serverRequest;
    }
	
	private void handleServerRequest(HttpServletRequest clientRequest, HttpServletResponse clientResponse, HttpUriRequest serverRequest, final Map<String,String> wmsParams) throws WMSProxyException {
        HttpClient serverClient = this.httpClient;
        
        try {
            HttpContext localContext = new BasicHttpContext();
            HttpResponse methodReponse = serverClient.execute(serverRequest, localContext);
            handleServerResponse(clientRequest, clientResponse, methodReponse, wmsParams);
        } catch (ClientProtocolException e) {
            String msg = "ProxyService.handleServerRequest() Exception : Client protocol error [" +
            			 e.getMessage() + "]";
			log.error(msg);
			
			WMSProxyExceptionID id = WMSProxyExceptionID.CLIENT_PROTOCOL_ERROR;					
			throw new WMSProxyException(id, "ProxyService", "handleServerRequest()", msg);
        } catch (IOException e) {
        	String msg = "ProxyService.handleServerRequest() Exception : I/O error on server request [" +
            			 e.getMessage() + "]";
			log.error(msg);
			
			WMSProxyExceptionID id = WMSProxyExceptionID.SERVER_REQUEST_IO_ERROR;					
			throw new WMSProxyException(id, "ProxyService", "handleServerRequest()", msg);
        }

    }
	
	private void handleServerResponse(HttpServletRequest clientRequest, HttpServletResponse clientResponse, HttpResponse serverResponse, final Map<String,String> wmsParams) throws WMSProxyException {
        String clientRequestURLAsString = ProxyUtil.getClientRequestURIAsString(clientRequest);
        String serverRequestURLAsString = ProxyUtil.getServerRequestURIAsString(clientRequest, wmsParams, ProxyService.forwardUrl, ProxyService.geoserverContext);

        // 1) Map server response status to client response
        // NOTE: There's no clear way to map status message, HttpServletResponse.sendError(int, String)
        // will display some custom html (we don't want that here).  HttpServletResponse.setStatus(int, String)
        // is deprecated and i'm not certain there is (will be) an functional implementation behind it.
        StatusLine serverStatusLine = serverResponse.getStatusLine();
        int statusCode = serverStatusLine.getStatusCode();
        clientResponse.setStatus(statusCode);
        log.debug("ProxyService.handleServerResponse() DEBUG: Mapped server status code " + statusCode);

        // 2) Map server response headers to client response
        ProxyUtil.generateClientResponseHeaders(clientResponse, serverResponse, ProxyService.ignoredServerResponseHeaderSet);

        // 3) Copy server response body to client response
        HttpEntity methodEntity = serverResponse.getEntity();
        if (methodEntity != null) {

            InputStream is = null;
            OutputStream os = null;
            
            String contentEncoding = "UTF-8";
            
            if(methodEntity.getContentEncoding() != null) {
            	contentEncoding = methodEntity.getContentEncoding().getValue();
	            if((contentEncoding == null) || (contentEncoding.equals(""))) {
	            	contentEncoding = "UTF-8";
	            }
            }

            long responseBytes = 0;

            try {

                // !!! Are you here to edit this to enable response body content rewrite?
                //     You may want to remove or edit the "Content-Length" header !!!
                try {
                    is = methodEntity.getContent();
                } catch (IOException e) {
                	String msg = "ProxyService.handleServerResponse() Exception : Error obtaining input stream for server response [" +
               			 e.getMessage() + "]";
		   			log.error(msg);
		   			
		   			WMSProxyExceptionID id = WMSProxyExceptionID.SERVER_RESPONSE_INPUT_STREAM_ERROR;					
		   			throw new WMSProxyException(id, "ProxyService", "handleServerResponse()", msg);
                }

                try {
                    os = clientResponse.getOutputStream();
                } catch (IOException e) {
                	String msg = "ProxyService.handleServerResponse() Exception : Error obtaining output stream for client response [" +
                  			 e.getMessage() + "]";
   		   			log.error(msg);
   		   			
   		   			WMSProxyExceptionID id = WMSProxyExceptionID.CLIENT_RESPONSE_OUTPUT_STREAM_ERROR;					
   		   			throw new WMSProxyException(id, "ProxyService", "handleServerResponse()", msg);
                }
                
                byte[] serverContent;
                try{
                	serverContent = IOUtils.toByteArray(is);
                } catch (IOException e) {
                	String msg = "ProxyService.handleServerResponse() Exception : Error copying server response to byteArray [" +
                			 e.getMessage() + "]";
 		   			log.error(msg);
 		   			
 		   			WMSProxyExceptionID id = WMSProxyExceptionID.SERVER_TO_CLIENT_RESPONSE_ERROR;					
 		   			throw new WMSProxyException(id, "ProxyService", "handleServerResponse()", msg);
               }
                
                byte [] convertedBytes;
                if(contentEncoding.toLowerCase().equals("utf-8")) {
                	log.debug("\n\nProxyService.handleServerResponse() DEBUG : CONTENT ENCODING IS UTF-8\n\n");
                	
                	String stringContent = Arrays.toString(serverContent);
                	
                	if(stringContent.contains(geoserverHost)) {
                		log.info("ProxyService.handleServerResponse() INFO : Server UTF-8 response contains references to its hostname.  Redirecting entries to the proxy...");
                		
	                	String proxyServerName = clientRequest.getServerName();
	                	String proxyContextPath = clientRequest.getContextPath();
	                	String proxyPort = clientRequest.getLocalPort() + "";
	                	String proxyProtocol = clientRequest.getScheme();
	                	
	                	String newContent = ProxyUtil.redirectContentToProxy(stringContent, geoserverProtocol, proxyProtocol, geoserverHost, proxyServerName, geoserverPort, proxyPort, geoserverContext, proxyContextPath);
	                	
	                	convertedBytes = newContent.getBytes();
                	} else {
                		log.debug("\n\nProxyService.handleServerResponse() DEBUG : Server UTF-8 response does not contain references to its hostname.  Continuing...\n\n");
                		convertedBytes = serverContent;
                	}
                } else if(contentEncoding.toLowerCase().equals("gzip")) {
                	log.debug("\n\nProxyService.handleServerResponse() DEBUG : CONTENT ENCODING IS GZIP\n\n");
                	
                	String stringContent = SystemUtils.uncompressGzipAsString(serverContent);
                	
                	if(stringContent.contains(geoserverHost)) {
                		log.info("ProxyService.handleServerResponse() INFO : Server GZIP response contains references to its hostname.  Redirecting entries to the proxy...");
                		
	                	String proxyServerName = clientRequest.getServerName();
	                	String proxyContextPath = clientRequest.getContextPath();
	                	String proxyPort = clientRequest.getLocalPort() + "";
	                	String proxyProtocol = clientRequest.getScheme();
	                	
	                	String newContent = ProxyUtil.redirectContentToProxy(stringContent, geoserverProtocol, proxyProtocol, geoserverHost, proxyServerName, geoserverPort, proxyPort, geoserverContext, proxyContextPath);
	                	
	                	/**
	                	 * We now need to re-gzip this
	                	 */
	                	convertedBytes = SystemUtils.compressStringToGzip(newContent);
                	} else {
                		log.debug("\n\nProxyService.handleServerResponse() DEBUG : Server GZIP response does not contain references to its hostname.  Continuing...\n\n");
                		convertedBytes = serverContent;
                	}
                } else {
                	log.debug("\n\nProxyService.handleServerResponse() DEBUG : CONTENT ENCODING IS [" + contentEncoding.toLowerCase() + "].  No inspection will occur...\n\n");
                	
                	/**
                	 * As far as we can tell this is not text content.  So we'll just
                	 * shove it right back at the client.
                	 */
                	convertedBytes = serverContent;
                }

                try {
                    IOUtils.write(convertedBytes, os);
                } catch (IOException e) {
                	String msg = "ProxyService.handleServerResponse() Exception : Error copying server response to client [" +
                 			 e.getMessage() + "]";
  		   			log.error(msg);
  		   			
  		   			WMSProxyExceptionID id = WMSProxyExceptionID.SERVER_TO_CLIENT_RESPONSE_ERROR;					
  		   			throw new WMSProxyException(id, "ProxyService", "handleServerResponse()", msg);
                }

            } finally {
                log.debug("ProxyService.handleServerResponse() DEBUG: Copied " + responseBytes + " bytes from server response for proxy from " + clientRequestURLAsString + " to " + serverRequestURLAsString);
                try {
                    // This is important to guarantee connection release back into
                    // connection pool for future reuse!
                    EntityUtils.consume(methodEntity);
                } catch (IOException e) {
                    log.error("ProxyService.handleServerResponse() Error: consuming remaining bytes in server response entity for proxy reponse from " + clientRequestURLAsString + " to " + serverRequestURLAsString);
                }
                IOUtils.closeQuietly(is);
                IOUtils.closeQuietly(os);
            }
        } else {
            log.warn("ProxyService.handleServerResponse() WARN: Server response was empty for proxy response from " + clientRequestURLAsString + " to " + serverRequestURLAsString);
        }
    }
}
