package gov.usgs.wqp.ogcproxy.utils;

import gov.usgs.wqp.ogcproxy.model.ogc.services.OGCServices;
import gov.usgs.wqp.ogcproxy.model.parameters.SearchParameters;
import gov.usgs.wqp.ogcproxy.model.parameters.WQPParameters;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.log4j.Logger;

/**
 * ProxyUtil
 * @author prusso
 *<br /><br />
 *	This class exposes many utility methods used in the proxying of data between
 *	a client and a server.  The majority of the methods here are statically 
 *	provided so they can be exposed and utilized outside of the package this
 *	utility resides in.
 */
public class ProxyUtil {
	private static Logger log = SystemUtils.getLogger(ProxyUtil.class);
	
	public static final String OGC_SERVICE_PARAMETER = "service";
	
	public static final String OGC_GET_CAPABILITIES_REQUEST_VALUE = "GetCapabilities";
	
	public static final String searchParamKey = WQPParameters.getStringFromType(WQPParameters.searchParams);
	public static final String testParamKey = "test-wms";
	
	public static final String PROXY_LAYER_ERROR = "wms_no_layer_error.xml";
	
	/**
	 * ProxyServiceResult
	 * @author prusso
	 *<br /><br />
	 *	This enumeration defines all status returns from a service in order to
	 *	determine the location of the servlet response.
	 */
	public enum ProxyServiceResult {
		SUCCESS, EMPTY, ERROR;

		public static ProxyServiceResult getTypeFromString(String string) {
			if (string.equals("SUCCESS")) {
				return SUCCESS;
			}
			
			if (string.equals("EMPTY")) {
				return EMPTY;
			}

			return ERROR;
		}

		public static String getStringFromType(ProxyServiceResult type) {
			switch (type) {
				case SUCCESS: {
					return "SUCCESS";
				}
				
				case EMPTY: {
					return "EMPTY";
				}
				
				default: {
					return "ERROR";
				}
			}
		}
	}
	
	/**
	 * ProxyServiceResult
	 * @author prusso
	 *<br /><br />
	 *	This enumeration defines all status returns from a service in order to
	 *	determine the location of the servlet response.
	 */
	public enum ProxyViewResult {
		EMPTY_JPG, EMPTY_PNG, EMPTY_TIFF, EMPTY_PDF, EMPTY_XML, ERROR_XML;

		public static ProxyViewResult getTypeFromString(String string) {
			if (string.equals("EMPTY_JPG")) {
				return EMPTY_JPG;
			}
			
			if (string.equals("EMPTY_PNG")) {
				return EMPTY_PNG;
			}
			
			if (string.equals("EMPTY_TIFF")) {
				return EMPTY_TIFF;
			}
			
			if (string.equals("EMPTY_PDF")) {
				return EMPTY_PDF;
			}
			
			if (string.equals("EMPTY_XML")) {
				return EMPTY_XML;
			}

			return ERROR_XML;
		}

		public static String getStringFromType(ProxyViewResult type) {
			switch (type) {
				case EMPTY_JPG: {
					return "EMPTY_JPG";
				}
				
				case EMPTY_PNG: {
					return "EMPTY_PNG";
				}
				
				case EMPTY_TIFF: {
					return "EMPTY_TIFF";
				}
				
				case EMPTY_PDF: {
					return "EMPTY_PDF";
				}
				
				case EMPTY_XML: {
					return "EMPTY_XML";
				}
				
				default: {
					return "ERROR_XML";
				}
			}
		}
		
		public static String getViewForType(ProxyViewResult type) {
			switch (type) {			
				case EMPTY_JPG: {
					return "whitepixel.jpg";
				}
				
				case EMPTY_PNG: {
					return "whitepixel.png";
				}
				
				case EMPTY_TIFF: {
					return "whitepixel.tiff";
				}
				
				case EMPTY_PDF: {
					return "whitepixel.pdf";
				}
				
				case EMPTY_XML: {
					return "no_results.xml";
				}
				
				default: {
					return "error.xml";
				}
			}
		}
	}
	
	public static String getErrorViewByFormat(String format) {
		if((format == null) || (format.equals(""))) {
			ProxyViewResult.getViewForType(ProxyViewResult.ERROR_XML);
		}
		
		if("image/png".equals(format.toLowerCase())) {
			return ProxyViewResult.getViewForType(ProxyViewResult.EMPTY_PNG);
		}
		
		if("image/png8".equals(format.toLowerCase())) {
			return ProxyViewResult.getViewForType(ProxyViewResult.EMPTY_PNG);
		}
		
		if("image/jpeg".equals(format.toLowerCase())) {
			return ProxyViewResult.getViewForType(ProxyViewResult.EMPTY_JPG);
		}
		
		if("image/jpg".equals(format.toLowerCase())) {
			return ProxyViewResult.getViewForType(ProxyViewResult.EMPTY_JPG);
		}
		
		if("image/tiff".equals(format.toLowerCase())) {
			return ProxyViewResult.getViewForType(ProxyViewResult.EMPTY_TIFF);
		}
		
		if("image/tiff8".equals(format.toLowerCase())) {
			return ProxyViewResult.getViewForType(ProxyViewResult.EMPTY_TIFF);
		}
		
		if("image/geotiff".equals(format.toLowerCase())) {
			return ProxyViewResult.getViewForType(ProxyViewResult.EMPTY_TIFF);
		}
		
		if("image/geotiff8".equals(format.toLowerCase())) {
			return ProxyViewResult.getViewForType(ProxyViewResult.EMPTY_TIFF);
		}
		
		if("application/pdf".equals(format.toLowerCase())) {
			return ProxyViewResult.getViewForType(ProxyViewResult.EMPTY_PDF);
		}
		
		return ProxyViewResult.getViewForType(ProxyViewResult.ERROR_XML);
	}
	
	/**
	 * separateParameters()
	 * <br /><br />
	 * This method separates all dynamic search parameters from any OGC specific
	 * parameters.  Search parameters are used for creating dynamic layers.
	 * 
	 * @param requestParams
	 * @param ogcParams
	 * @param searchParams
	 * @return
	 */
	public static boolean separateParameters(Map<String,String> requestParams, Map<String,String> ogcParams, Map<String, List<String>> searchParams) {
		if(requestParams == null) {
			return false;
		}
		
		if(ogcParams == null) {
			ogcParams = new HashMap<String,String>();
		}
		
		if(searchParams == null) {
			searchParams = new SearchParameters<String,List<String>>();
		}
		
		log.debug("ProxyUtil.separateParameters() REQUEST PARAMS:\n" + requestParams);
		
		boolean containsSearchQuery = false;
		Iterator<Entry<String, String>> itr = requestParams.entrySet().iterator();
	    while (itr.hasNext()) {
	        Map.Entry<String, String> pairs = (Map.Entry<String, String>)itr.next();
	        
	        String key = pairs.getKey();
	        
	        /**
	         * OGC Spec for WFS and WMS states clearly that parameters shall NOT be
	         * case sensitive.  We ran into some case sensitivity issues in GeoServer
	         * so we made the conscious decision to make all parameter mappings be
	         * case sensitive.  Since the "searchParams" key is basically a "vendor parameter"
	         * we can do whatever we need to make this work.  OpenLayers, by default,
	         * upper cases all of its parameters including this specific parameter.  We
	         * will make the "searchParams" case insensitive
	         */
	        if(key.toLowerCase().equals(ProxyUtil.searchParamKey.toLowerCase())) {
	        	containsSearchQuery = true;
	        	continue;
	        }
	        
	        if(key.equals(ProxyUtil.testParamKey)) {
	        	continue;
	        }
	        
	        ogcParams.put(key, pairs.getValue());
	    }
	    log.debug("ProxyUtil.separateParameters() OGC PARAMETER MAP:\n[" + ogcParams + "]");
		
		if(containsSearchQuery) {
			String searchParamString = requestParams.get(ProxyUtil.searchParamKey);
			
			/**
			 * This is a "create layer" request.  We need to first see if it exists
			 * already.
			 * 
			 * http://www.waterqualitydata.us/Station/search?countrycode=US&statecode=US%3A04|US%3A06&countycode=US%3A04%3A001|US%3A04%3A007|US%3A06%3A011|US%3A06%3A101&within=10&lat=46.12&long=-89.15&siteType=Estuary&organization=BCHMI&siteid=usgs-station&huc=010801*&sampleMedia=Air&characteristicType=Biological&characteristicName=Soluble+Reactive+Phosphorus+(SRP)&pCode=00065&startDateLo=01-01-1991&startDateHi=02-02-1992&providers=NWIS&providers=STEWARDS&providers=STORET&bBox=-89.68%2C-89.15%2C45.93%2C46.12&mimeType=csv&zip=yes
			 */			
			
			WQPUtils.parseSearchParams(searchParamString, searchParams);
			
			log.debug("ProxyUtil.separateParameters() SEARCH PARAMETER MAP:\n[" + searchParams + "]");
		}
		
		return true;
	}
	
	public static String getServerRequestURIAsString(final HttpServletRequest clientrequest, final Map<String,String> wmsParams, final String forwardURL, final String context) {
        String proxyPath = new StringBuilder(clientrequest.getContextPath()).
                append(clientrequest.getServletPath()).toString();
        
        /**
         * With the proxyPath we need to replace the ogcproxy context with the
         * passed in context
         */
        String requestContext = clientrequest.getContextPath();
        proxyPath = proxyPath.replace(requestContext, context);

        StringBuilder requestBuffer = new StringBuilder(forwardURL + proxyPath + "?");
        
        Iterator<Entry<String,String>> paramEntryItr = wmsParams.entrySet().iterator();
        while (paramEntryItr.hasNext()) {
        	Entry<String,String> paramEntry = paramEntryItr.next();
            String param = paramEntry.getKey();
            String value = paramEntry.getValue();
            
            requestBuffer.append(param);
            requestBuffer.append("=");
            
            String encodedValue;
			try {
				encodedValue = URLEncoder.encode(value, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				log.error("ProxyUtil.getServerRequestURIAsString() Encoding parameter value exception:\n[" + e.getMessage() + "].  Using un-encoded value instead [" + value + "]");
				encodedValue = value;
			}
            requestBuffer.append(encodedValue);
            
            if(paramEntryItr.hasNext()) {
            	requestBuffer.append("&");
            }
        }        

        return requestBuffer.toString();
    }
    
    public static String getClientRequestURIAsString(HttpServletRequest clientRequest) {
        return clientRequest.getRequestURL().toString();
    }
    
    public static void generateServerRequestHeaders(HttpServletRequest clientRequest, HttpUriRequest serverRequest, final Set<String> ignoredClientRequestHeaderSet) {
        Enumeration<String> headerNameEnumeration = clientRequest.getHeaderNames();
        while (headerNameEnumeration.hasMoreElements()) {
            String requestHeaderName = headerNameEnumeration.nextElement();
            Enumeration<String> headerValueEnumeration = clientRequest.getHeaders(requestHeaderName);
            while (headerValueEnumeration.hasMoreElements()) {
                String requestHeaderValue = headerValueEnumeration.nextElement();
                if (!ignoredClientRequestHeaderSet.contains(requestHeaderName)) {
                    serverRequest.addHeader(requestHeaderName, requestHeaderValue);
                    log.debug("Mapped client request header \"" + requestHeaderName + ": " + requestHeaderValue + "\"");
                } else {
                    log.debug("Ignored client request header \"" + requestHeaderName + ": " + requestHeaderValue + "\"");
                }
            }

        }

        URI serverURI = serverRequest.getURI();
        StringBuilder serverHostBuilder = new StringBuilder(serverURI.getHost());
        if (serverURI.getPort() > -1) {
            serverHostBuilder.append(':').append(serverURI.getPort());
        }
        String requestHost = serverHostBuilder.toString();
        serverRequest.addHeader("Host", serverHostBuilder.toString());
        log.debug("Added server request header \"Host: " + requestHost + "\"");
    }
    
    public static void generateClientResponseHeaders(HttpServletResponse clientResponse, HttpResponse serverResponse, Set<String> ignoredServerResponseHeaderSet) {
        Header[] proxyResponseHeaders = serverResponse.getAllHeaders();
        for (Header header : proxyResponseHeaders) {
            String responseHeaderName = header.getName();
            String responseHeaderValue = header.getValue();
            if (!ignoredServerResponseHeaderSet.contains(responseHeaderName)) {
                clientResponse.addHeader(responseHeaderName, responseHeaderValue);
                log.debug("Mapped server response header \"" + responseHeaderName + ": " + responseHeaderValue + "\"");
            } else {
                log.debug("Ignored server response header \"" + responseHeaderName + ": " + responseHeaderValue + "\"");
            }
        }
    }
    
    public static String redirectContentToProxy(String content, String serverProtocol, String proxyProtocol, String serverHost, String proxyHost, String serverPort, String proxyPort, String serverContext, String proxyContext) {
    	String newContent = content.replaceAll(serverProtocol, proxyProtocol);
    	newContent = newContent.replaceAll(serverHost, proxyHost);
    	newContent = newContent.replaceAll(serverPort, proxyPort);
    	newContent = newContent.replaceAll(serverContext, proxyContext);
    	return newContent;
    }
    
    /**
     * getFinalRequestedService()
     * <br /><br />
     * It's possible for a base request (by path) to go to one OGC service but actually
     * request another OGC service.  This is accomplished by filling in the "service" 
     * parameter with a service different from the one on the path.
     * <br /><br />
     * This method will return the final service requested.  If an unknown service
     * is requested, this method will return the base request service.
     * @param ogcParams
     * @param calledService
     * @return
     */
    public static OGCServices getFinalRequestedService(Map<String,String> requestParams, OGCServices calledService) {
    	String serviceValue = requestParams.get(OGC_SERVICE_PARAMETER);
    	
    	if((serviceValue == null) || (serviceValue.equals(""))) {
    		return calledService;
    	}
    	
    	OGCServices requestedService = OGCServices.getTypeFromString(serviceValue);
    	
    	if((requestedService == calledService) || (requestedService == OGCServices.UNKNOWN)) {
    		return calledService;
    	}
    	
    	return requestedService;
    }
}
