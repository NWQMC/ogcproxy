package gov.usgs.wqp.ogcproxy.utils;

import java.io.File;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.FileEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.log4j.Logger;


/**
 * RESTUtils
 * @author prusso
 *<br /><br />
 *	This class exposes many utility methods used for performing REST methods
 *	against a REST server.  The majority of the methods here are statically
 *	provided so they can be exposed and utilized outside of the package this
 *	utility resides in.
 */
public class RESTUtils {
	private static Logger log = SystemUtils.getLogger(RESTUtils.class);
		
	public static String putDataFile(String host, String port, String uri, String user, String pass, String mediaType, String filename) {
	    String response = null;
	    HttpResponse rtn = null;
	    CloseableHttpClient httpClient;
		File file = new File(filename);
	    
	    if (file.exists()) {
	    	CredentialsProvider credsProvider = new BasicCredentialsProvider();
	        credsProvider.setCredentials(
	                new AuthScope(host, Integer.parseInt(port)),
	                new UsernamePasswordCredentials(user, pass));
	        httpClient = HttpClients.custom()
	                .setDefaultCredentialsProvider(credsProvider)
	                .build(); 
	        HttpPut httpPut = new HttpPut(uri);
	        HttpEntity fileEntity = new FileEntity(file, ContentType.create(mediaType));
	        httpPut.setEntity(fileEntity);
	        try {
				rtn = httpClient.execute(httpPut);
				if (HttpStatus.SC_CREATED != rtn.getStatusLine().getStatusCode()) {
					response = "RESTUtils.putDataFile() Exception: Invalid status code from geoserver:" + rtn.getStatusLine().getStatusCode();
					log.error(response);
				}
			} catch (Exception e) {
				response = "RESTUtils.putDataFile() Exception: " + e.getLocalizedMessage();
				log.error(e.getStackTrace());
			}
	    } else {
	    	response = "RESTUtils.putDataFile() Exception: File of type [" + mediaType + "] DOES NOT EXIST";
			log.error(response);
	    }
	   
	    return response;
	}

	public static <H> H getObject(String host, String port, String uri, String user, String pass, ResponseHandler<H> responseHandler) {
		H rtn = null;
    	CredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(
                new AuthScope(host, Integer.parseInt(port)),
                new UsernamePasswordCredentials(user, pass));
 
        HttpGet httpGet = new HttpGet(uri);
        try (CloseableHttpClient httpClient = HttpClients.custom()
                .setDefaultCredentialsProvider(credsProvider)
                .build();) {

        	rtn = httpClient.execute(httpGet, responseHandler);
        	log.info("hiDave!!");
		} catch (Exception e) {
//			response = "RESTUtils.putDataFile() Exception: " + e.getLocalizedMessage();
			log.error(e.getStackTrace());
		}
		return rtn;
	}
	
}
