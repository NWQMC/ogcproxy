package gov.usgs.wqp.ogcproxy.utils;

import gov.usgs.wqp.ogcproxy.model.ogc.services.OGCServices;
import gov.usgs.wqp.ogcproxy.model.parameters.SearchParameters;
import gov.usgs.wqp.ogcproxy.model.parameters.WQPParameters;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
		if (requestParams == null) {
			return false;
		}
		
		if (ogcParams == null) {
			ogcParams = new HashMap<String,String>();
		}
		
		if (searchParams == null) {
			searchParams = new SearchParameters<String,List<String>>();
		}
		
		log.debug("ProxyUtil.separateParameters() REQUEST PARAMS:\n" + requestParams);
		
		/*
		 * We need to capture the pure servlet parameter key for our searchParams parameter.
		 *
		 * Since this can be case INSENSITIVE but we use its value as a key in a map, we need
		 * to know what the exact character sequence is going forward.
		 */
		String servletSearchParamName = ProxyUtil.searchParamKey;
		
		boolean containsSearchQuery = false;
	    for (Map.Entry<String, String> pairs : requestParams.entrySet()) {
	        
	        String key = pairs.getKey();
	        
	        /*
	         * OGC Spec for WFS and WMS states clearly that parameters shall NOT be
	         * case sensitive.  We ran into some case sensitivity issues in GeoServer
	         * so we made the conscious decision to make all parameter mappings be
	         * case sensitive.  Since the "searchParams" key is basically a "vendor parameter"
	         * we can do whatever we need to make this work.  OpenLayers, by default,
	         * upper cases all of its parameters including this specific parameter.  We
	         * will make the "searchParams" case insensitive
	         */
	        if (key.toLowerCase().equals(ProxyUtil.searchParamKey.toLowerCase())) {
	        	containsSearchQuery = true;
	        	servletSearchParamName = key;
	        	continue;
	        }
	        
	        if (key.equals(ProxyUtil.testParamKey)) {
	        	continue;
	        }
	        
	        ogcParams.put(key, pairs.getValue());
	    }
	    log.debug("ProxyUtil.separateParameters() OGC PARAMETER MAP:\n[" + ogcParams + "]");
		
		if (containsSearchQuery) {
			String searchParamString = requestParams.get(servletSearchParamName);
			
			/*
			 * This is a "create layer" request.  We need to first see if it exists  salready.
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
        
        /*
         * With the proxyPath we need to replace the ogcproxy context with the
         * passed in context
         */
        String requestContext = clientrequest.getContextPath();
        proxyPath = proxyPath.replace(requestContext, context);

        StringBuilder requestBuffer = new StringBuilder(forwardURL + proxyPath + "?");
        
        String sep = "";
        for (Map.Entry<String,String> paramEntry : wmsParams.entrySet()) {
            String param = paramEntry.getKey();
            String value = paramEntry.getValue();
            
            requestBuffer.append(sep).append(param).append("=");
            sep = "&";
            
            String encodedValue;
			try {
				encodedValue = URLEncoder.encode(value, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				log.error("ProxyUtil.getServerRequestURIAsString() Encoding parameter value exception:\n[" + e.getMessage() + "].  Using un-encoded value instead [" + value + "]");
				encodedValue = value;
			}
            requestBuffer.append(encodedValue);
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
     * getRequestedService()
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
    public static OGCServices getRequestedService(OGCServices calledService, Map<String, String> ogcParams) {
    	OGCServices requestedService = OGCServices.UNKNOWN;
    	
		try {
	    	String serviceValue = ogcParams.get(OGC_SERVICE_PARAMETER);
	    	requestedService = OGCServices.getTypeFromString(serviceValue);
		} catch (Exception e) {
	    	requestedService = OGCServices.UNKNOWN;
		}
    	
    	if (requestedService == OGCServices.UNKNOWN) {
    		return calledService;
    	}
    	
    	return requestedService;
    }
    
    /**
     * getServletParameterCaseSensitiveCharacterString()
     * <br /><br />
     * The problem we have with our parameters is that the OGC spec specifically
     * mandates that all parameters are CASE-INSENSITIVE.  But since we get our
     * parameters in a Map where the parameter is a key to the value in the map,
     * case sensitivity is indeed important when trying to access the parameter map.
     * <br /><br />
     * This method returns the servlet case-sensitive version of a parameter that
     * is inherently case-insensitive.
     * @param ourParam
     * @param requestParams
     * @return
     */
    public static String getCaseSensitiveParameter(String ourParam, Map<String,String> requestParams) {
    	String result = ourParam;
    	
    	for (String key : requestParams.keySet()) {
    		if ( key.equalsIgnoreCase(ourParam) ) {
	        	return key;
	        }
    	}
    	
    	return result;
    }
}
