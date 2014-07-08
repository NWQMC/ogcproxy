package gov.usgs.wqp.ogcproxy.utils;

import java.io.File;

import org.apache.log4j.Logger;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;

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
	
	public static String get(String uri, String user, String pass, String mediaType) {
		Client client = Client.create();
		
		if(user != null) {
			client.addFilter(new HTTPBasicAuthFilter(user, pass));
		}
		
		WebResource service = client.resource(uri);
		
		String response;
		try {
			response = service.type(mediaType).accept(mediaType).get(String.class);
		} catch(UniformInterfaceException e) {
			response = "RESTUtils.get() Exception: UniformInterfaceException Exception [" + e.getMessage() + "]";
			log.error(response);
		} catch(ClientHandlerException e) {
			response = "RESTUtils.get() Exception: ClientHandlerException Exception [" + e.getMessage() + "]";
			log.error(response);
		} finally {
			client.destroy();
		}
		
		return response;
	}
	
	public static String post(String uri, String data, String user, String pass, String mediaType) {
		Client client = Client.create();
		
		if(user != null) {
			client.addFilter(new HTTPBasicAuthFilter(user, pass));
		}
		
		WebResource service = client.resource(uri);
		
		String response;
		try {
			response = service.type(mediaType).accept(mediaType).post(String.class, data);
		} catch(UniformInterfaceException e) {
			response = "RESTUtils.post() Exception: UniformInterfaceException Exception [" + e.getMessage() + "]";
			log.error(response);
		} catch(ClientHandlerException e) {
			response = "RESTUtils.post() Exception: ClientHandlerException Exception [" + e.getMessage() + "]";
			log.error(response);
		} finally {
			client.destroy();
		}
		
		return response;
	}
	
	public static String postFile(String uri, String filename, String user, String pass, String mediaType) {
		Client client = Client.create();
		
		if(user != null) {
			client.addFilter(new HTTPBasicAuthFilter(user, pass));
		}
		
		WebResource service = client.resource(uri);
		File file = new File(filename);
        
        String response;
        if(file.exists()) {
        	try {
        		response = service.type(mediaType).accept(mediaType).post(String.class, file);
        	} catch(UniformInterfaceException e) {
    			response = "RESTUtils.postFile() Exception: UniformInterfaceException Exception [" + e.getMessage() + "]";
    			log.error(response);
    		} catch(ClientHandlerException e) {
    			response = "RESTUtils.postFile() Exception: ClientHandlerException Exception [" + e.getMessage() + "]";
    			log.error(response);
    		}
        } else {
        	response = "RESTUtils.postFile() Exception: File of type [" + mediaType + "] DOES NOT EXIST";
			log.error(response);
        }
        
        client.destroy();
		
		return response;
	}
	
	public static String putDataFile(String uri, String user, String pass, String mediaType, String filename) {		
		ClientConfig config = new DefaultClientConfig(); 
        Client client = Client.create(config);
		
		if(user != null) {
			client.addFilter(new HTTPBasicAuthFilter(user, pass));
		}
    
        WebResource service = client.resource(uri); 
        File file = new File(filename);
                
        String response;
        if(file.exists()) {
        	try {
        		response = service.type(mediaType).accept(mediaType).put(String.class, file);
        	} catch(UniformInterfaceException e) {
    			response = "RESTUtils.putDataFile() Exception: UniformInterfaceException Exception [" + e.getMessage() + "]";
    			log.error(response);
    		} catch(ClientHandlerException e) {
    			response = "RESTUtils.putDataFile() Exception: ClientHandlerException Exception [" + e.getMessage() + "]";
    			log.error(response);
    		}
        } else {
        	response = "RESTUtils.putDataFile() Exception: File of type [" + mediaType + "] DOES NOT EXIST";
			log.error(response);
        }
        
        client.destroy();
        
        return response;
	}
	
	public static String delete(String uri, String user, String pass, String mediaType) {
		Client client = Client.create();
		
		if(user != null) {
			client.addFilter(new HTTPBasicAuthFilter(user, pass));
		}
		
		WebResource service = client.resource(uri);
		
		String response;
		try {
			response = service.type(mediaType).accept(mediaType).delete(String.class);
		} catch(UniformInterfaceException e) {
			response = "RESTUtils.delete() Exception: UniformInterfaceException Exception [" + e.getMessage() + "]";
			log.error(response);
		} catch(ClientHandlerException e) {
			response = "RESTUtils.delete() Exception: ClientHandlerException Exception [" + e.getMessage() + "]";
			log.error(response);
		} finally {
			client.destroy();
		}
		
		return response;
	}
}
