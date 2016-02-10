package gov.usgs.wqp.ogcproxy.utils;

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
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.usgs.wqp.ogcproxy.model.OGCRequest;
import gov.usgs.wqp.ogcproxy.model.ogc.services.OGCServices;
import gov.usgs.wqp.ogcproxy.model.parameters.SearchParameters;
import gov.usgs.wqp.ogcproxy.model.parameters.WQPParameters;
import gov.usgs.wqp.ogcproxy.model.parser.xml.ogc.OgcParser;

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
	private static final Logger LOG = LoggerFactory.getLogger(ProxyUtil.class);
	
	public static final String OGC_GET_CAPABILITIES  = "GetCapabilities";
	
	public static final String OGC_SERVICE_PARAMETER = "service";
	
	public static final String searchParamKey        = WQPParameters.searchParams.toString();
	
	public static final String PROXY_LAYER_ERROR     = "no_layer_error.xml";
	
	
	/**
	 * separateParameters()
	 * <br /><br />
	 * This method separates all dynamic search parameters from any OGC specific
	 * parameters.  Search parameters are used for creating dynamic layers.
	 *
	 * @param requestParams
	 * @return
	 */
	public static OGCRequest separateParameters(HttpServletRequest request, OGCServices ogcService) {
		Map<String, String[]> requestParams = request.getParameterMap();
		String requestBody = ""; 
		if (HttpPost.METHOD_NAME.equalsIgnoreCase(request.getMethod())) {
			OgcParser ogcParser = new OgcParser(request);
			try {
				ogcParser.parse();
				requestParams = ogcParser.getRequestParamsAsMap();
				requestBody = ogcParser.getBodyMinusVendorParams();
			} catch (Exception e) {
				//TODO - maybe something?
				LOG.error(e.getLocalizedMessage(), e);
			}
		}

		if (requestParams == null || requestParams.isEmpty()) {
			return new OGCRequest(ogcService);
		}

		//This is here because the URL is a default request service but could receive a request of either WFS/WMS service
		OGCServices realOgcService = ProxyUtil.getRequestedService(ogcService, requestParams);

		Map<String, String> ogcParams = new HashMap<>();
		SearchParameters<String, List<String>> searchParams = new SearchParameters<>();
		
		LOG.debug("ProxyUtil.separateParameters() REQUEST PARAMS:\n" + requestParams);
		
		/*
		 * We need to capture the pure servlet parameter key for our searchParams parameter.
		 *
		 * Since this can be case INSENSITIVE but we use its value as a key in a map, we need
		 * to know what the exact character sequence is going forward.
		 */
		String servletSearchParamName = ProxyUtil.searchParamKey;
		
		boolean containsSearchQuery = false;
	    for (Map.Entry<String, String[]> pairs : requestParams.entrySet()) {
	        
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
	        if (ProxyUtil.searchParamKey.equalsIgnoreCase(key)) {
	        	containsSearchQuery = true;
	        	servletSearchParamName = key;
	        	continue;
	        }
	        
	        ogcParams.put(key, String.join(",", pairs.getValue()));
	    }
	    LOG.debug("ProxyUtil.separateParameters() OGC PARAMETER MAP:\n[" + ogcParams + "]");
		
		if (containsSearchQuery) {
			String searchParamString = String.join(";", requestParams.get(servletSearchParamName));
			searchParams = WQPUtils.parseSearchParams(searchParamString);
			LOG.debug("ProxyUtil.separateParameters() SEARCH PARAMETER MAP:\n[" + searchParams + "]");
		}
		
		return new OGCRequest(realOgcService, ogcParams, searchParams, requestBody);
	}
	
	
	/**
	 * Builds query string
	 * @param clientrequest
	 * @param ogcParams
	 * @param forwardURL
	 * @param context
	 * @return
	 */
	public static String getServerRequestURIAsString(HttpServletRequest clientrequest, Map<String,String> ogcParams, String forwardURL, String context) {
        StringBuilder requestBuffer = new StringBuilder(forwardURL).append("/").append(context).append(clientrequest.getServletPath()).append("?");
        
        String sep = "";
        for (Map.Entry<String,String> paramEntry : ogcParams.entrySet()) {
            String param = paramEntry.getKey();
            String value = paramEntry.getValue();
            
            requestBuffer.append(sep).append(param).append("=");
            sep = "&";
            
            String encodedValue;
			try {
				encodedValue = URLEncoder.encode(value, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				LOG.error("ProxyUtil.getServerRequestURIAsString() Encoding parameter value exception:\n[" + e.getMessage() + "].  Using un-encoded value instead [" + value + "]");
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
                
                String logMsg = " client request header \"" +requestHeaderName + ": " + requestHeaderValue + "\"";
                if ( ignoredClientRequestHeaderSet.contains(requestHeaderName) ) {
                    LOG.debug("Ignored" +logMsg);
                } else {
                    serverRequest.addHeader(requestHeaderName, requestHeaderValue);
                    LOG.debug("Mapped" +logMsg);
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
        LOG.debug("Added server request header \"Host: " + requestHost + "\"");
    }
    
    public static void generateClientResponseHeaders(HttpServletResponse clientResponse, HttpResponse serverResponse, Set<String> ignoredServerResponseHeaderSet) {
        
        for (Header header : serverResponse.getAllHeaders()) {
            String name  = header.getName();
            String value = header.getValue();
            
            String logMsg = " server response header \"" + name + ": " + value + "\"";
            if ( ignoredServerResponseHeaderSet.contains(name) ) {
                LOG.debug("Ignored" +logMsg);
            } else {
                clientResponse.addHeader(name, value);
                LOG.debug("Mapped" +logMsg );
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
    public static OGCServices getRequestedService(OGCServices calledService, Map<String, String[]> ogcParams) {
		try {
			//We only expect one service value and any Exceptions will be eaten and the original called service returned. 
	    	String serviceValue = ogcParams.get(getCaseSensitiveParameter(OGC_SERVICE_PARAMETER, ogcParams))[0].toUpperCase();
	    	return OGCServices.valueOf(serviceValue);
		} catch (Exception e) {
			return calledService;
		}
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
    public static String getCaseSensitiveParameter(String ourParam, Map<String, ?> requestParams) {
    	if (null != requestParams) {
	    	for (String key : requestParams.keySet()) {
	    		if ( key.equalsIgnoreCase(ourParam) ) {
		        	return key;
		        }
	    	}
    	}    	
    	return ourParam;
    }
}
