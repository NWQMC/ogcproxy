package gov.usgs.wqp.ogcproxy.utils;

import gov.usgs.wqp.ogcproxy.exceptions.OGCProxyException;
import gov.usgs.wqp.ogcproxy.exceptions.OGCProxyExceptionID;
import gov.usgs.wqp.ogcproxy.model.parameters.SearchParameters;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;

/**
 * WQPUtils
 * @author prusso
 *<br /><br />
 *	This class exposes many utility methods when interfacing with the WQP project
 *	in general.  The majority of the methods here are statically provided so
 *	they can be exposed and utilized outside of the package this utility
 *	resides in.
 */
public class WQPUtils {
	private static Logger log = SystemUtils.getLogger(WQPUtils.class);
	
	public static final String layerPrefix = "dynamicSites_";
	
	public static void parseSearchParams(String searchParamString, Map<String, List<String>> searchParams) {
		if(searchParams == null) {
			searchParams = new SearchParameters<String,List<String>>();
		}
		
		//log.info("WQPUtils.parseSearchParams() - SearchParamString = [" + searchParamString + "]");
		
		/**
		 * SearchParams is a list of key/value pairs depicting key=search param and value = search param value.
		 * 
		 * Search Param Values can be lists of strings.
		 * 
		 * The overall String structure has the following formats:
		 * 
		 * 		general form:
		 * 				&searchParams=filter1:value1;filter2=value2;filter3=value3
		 * 
		 * 		empty values:
		 * 				&searchParams=filter1:;filter2:;filter3:
		 * 
		 * 		multiple values:
		 * 				&searchParams=filter1:value1|value2|value3;filter2:value1|value2|value3;filter3:value1|value2|value3
		 * 
		 *  The passed in String is just the value (everything after the = sign).
		 *  
		 *  All keys are separated by semicolons.
		 *  
		 *  Multiple values per each key are separated by pipes (|)
		 *  
		 */
		List<String> keyAndValues = Arrays.asList(searchParamString.split(";"));
		
		for(String pairs : keyAndValues) {
			/**
			 * Search Param pairs consist of a key and value separated by a colon.
			 * 
			 *    ** IMPORTANT **
			 *  	05/20/2014
			 *  
			 *  We have many "values" within the search parameters that can contain
			 *  colons as well.  This means that the delimeter between the key and
			 *  the value is compromised.  Since we are already using pipes and it
			 *  is very possible that these values can contain any other delimeter
			 *  symbol, we have decided to split the keys and their values by the
			 *  first colon found.  This way it guarantees that we at least have
			 *  a delineation between the key and its values.
			 *  
			 */
			List<String> keyValue = Arrays.asList(pairs.split(":", 2));
			
			if(keyValue.size() != 2) {
				log.warn("WQPUtils.parseSearchParams() ERROR: keyValue pair [" + pairs + "] does not split evenly with a colon.  " +
						  "Resulting split results in an array of size {" + keyValue.size() + "} with values: " + keyValue +
						  ".  Skipping this search param.");
				continue;
			}
			
			String key = keyValue.get(0);
			String stringValues = keyValue.get(1);
			
			List<String> values = Arrays.asList(stringValues.split("\\|"));
			
			searchParams.put(key, values);			
		}
	}
	
	public static HttpUriRequest generateSimpleStationRequest(SearchParameters<String, List<String>> searchParams, String simpleStationURL) throws OGCProxyException {
		HttpUriRequest request = null;
		        
        StringBuilder requestURIBuffer = new StringBuilder(simpleStationURL + "?");        
        Iterator<Entry<String,List<String>>> paramEntryItr = searchParams.entrySet().iterator();
        while (paramEntryItr.hasNext()) {
        	Entry<String,List<String>> paramEntry = paramEntryItr.next();
            String param = paramEntry.getKey();
            List<String> values = paramEntry.getValue();
            
            requestURIBuffer.append(param);
            requestURIBuffer.append("=");
            
            /**
             * Delineate multiple values per parameter with a semi-colon ';'
             */
            StringBuffer joinedValues = new StringBuffer();
            Iterator<String> rawValueItr = values.iterator();
            while (rawValueItr.hasNext()) {
            	String rawValue = rawValueItr.next();
            	joinedValues.append(rawValue);
            	
            	if(rawValueItr.hasNext()) {
            		joinedValues.append(";");
            	}
            }
            
            String encodedValue;
			try {
				encodedValue = URLEncoder.encode(joinedValues.toString(), "UTF-8");
			} catch (UnsupportedEncodingException e) {
				log.error("WQPUtils.generateSimpleStationRequest() Encoding parameter value exception:\n[" + e.getMessage() + "].  Using un-encoded value instead [" + joinedValues.toString() + "]");
				encodedValue = joinedValues.toString();
			}
			requestURIBuffer.append(encodedValue);
            
            if(paramEntryItr.hasNext()) {
            	requestURIBuffer.append("&");
            }
        }
        
        /**
         * Add our mimeType argument
         */
        requestURIBuffer.append("&mimeType=xml");
        
        URI serverRequestURI;
		try {
			serverRequestURI = (new URL(requestURIBuffer.toString())).toURI();
		} catch (MalformedURLException e) {
			String msg = "WQPUtils.generateSimpleStationRequest() Exception : Syntax error parsing server URL [" +
  				  e.getMessage() + "].";
			log.error(msg);
			
			OGCProxyExceptionID id = OGCProxyExceptionID.URL_PARSING_EXCEPTION;					
			throw new OGCProxyException(id, "WQPUtils", "generateSimpleStationRequest()", msg);
      } catch (URISyntaxException e) {
      	String msg = "WQPUtils.generateSimpleStationRequest() Exception : Syntax error parsing server URL [" +
				  e.getMessage() + "].";
			log.error(msg);
			
			OGCProxyExceptionID id = OGCProxyExceptionID.URL_PARSING_EXCEPTION;					
			throw new OGCProxyException(id, "WQPUtils", "generateSimpleStationRequest()", msg);
		}
        request = new HttpGet(serverRequestURI);
		
		return request;
	}
	
	public static String retrieveSearchParamData(HttpClient httpClient, SearchParameters<String, List<String>> searchParams, String simpleStationURL, String workingDirectory, String layerName) throws OGCProxyException {
		HttpUriRequest serverRequest = WQPUtils.generateSimpleStationRequest(searchParams, simpleStationURL);
		
		HttpResponse methodResponse;
		
		try {
            HttpContext localContext = new BasicHttpContext();
            methodResponse = httpClient.execute(serverRequest, localContext);
        } catch (ClientProtocolException e) {
            String msg = "WQPUtils.retrieveSearchParamData() Exception : Client protocol error [" +
            			 e.getMessage() + "]";
			log.error(msg);
			
			OGCProxyExceptionID id = OGCProxyExceptionID.CLIENT_PROTOCOL_ERROR;					
			throw new OGCProxyException(id, "WQPUtils", "retrieveSearchParamData()", msg);
        } catch (IOException e) {
        	String msg = "WQPUtils.retrieveSearchParamData() Exception : I/O error on server request [" +
            			 e.getMessage() + "]";
			log.error(msg);
			
			OGCProxyExceptionID id = OGCProxyExceptionID.SERVER_REQUEST_IO_ERROR;					
			throw new OGCProxyException(id, "WQPUtils", "retrieveSearchParamData()", msg);
        }
        
        StatusLine serverStatusLine = methodResponse.getStatusLine();
        int statusCode = serverStatusLine.getStatusCode();
        log.debug("WQPUtils.retrieveSearchParamData() DEBUG: Server status code " + statusCode);
        
        if(statusCode != 200) {
        	String msg = "WQPUtils.retrieveSearchParamData() Invalid response from WQP server.  Status code [" +
        			statusCode + "].\nResponseHeaders: [" + Arrays.toString(methodResponse.getAllHeaders());
			log.error(msg);
			
			OGCProxyExceptionID id = OGCProxyExceptionID.INVALID_SERVER_RESPONSE_CODE;					
			throw new OGCProxyException(id, "WQPUtils", "retrieveSearchParamData()", msg);
        }
        
        HttpEntity methodEntity = methodResponse.getEntity();
        
        String filePath = workingDirectory + "/" + layerName + ".xml";
        BufferedInputStream bis = null;
        BufferedOutputStream bos = null;
		try {
			bis = new BufferedInputStream(methodEntity.getContent());
			bos = new BufferedOutputStream(new FileOutputStream(new File(filePath)));
		   	byte[] buff = new byte[1024*8];
		   	int count=0;
			while((count = bis.read(buff)) != -1) {
				bos.write(buff,0,count);
			}
		} catch (Exception e) {
			String msg = "WQPUtils.retrieveSearchParamData() Exception reading response from server [" +
       			 e.getMessage() + "] Check that the path exists: " + filePath;
			log.error(msg);
			
			OGCProxyExceptionID id = OGCProxyExceptionID.SERVER_REQUEST_IO_ERROR;					
			throw new OGCProxyException(id, "WQPUtils", "retrieveSearchParamData()", msg);
		} finally {
			try {
                // This is important to guarantee connection release back into
                // connection pool for future reuse!
                EntityUtils.consume(methodEntity);
            } catch (IOException e) {
                log.error("WQPUtils.retrieveSearchParamData() Error: consuming remaining bytes in server response entity from SimpleStation request [" + serverRequest.getURI() + "]");
            }
        	
			try {
				if (bos != null) {
					bos.flush();
					bos.close();
				}
				if (bis != null) {
					bis.close();
				}
			} catch (IOException e) {
				String msg = "WQPUtils.retrieveSearchParamData() Exception closing buffered streams [" +
		       			 e.getMessage() + "] continuing...";
				log.error(msg);
			}
         }
		
		/**
		 * Last thing to check is if our File has any data in it.  If the request
		 * was empty, we'll have an empty file
		 */
		File dataFile = new File(filePath);
		if(dataFile.length() > 0) {
			return filePath;
		}
		
		return "";
	}

}






