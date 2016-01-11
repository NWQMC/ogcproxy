package gov.usgs.wqp.ogcproxy.utils;

import java.io.File;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

import org.apache.log4j.Logger;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.media.multipart.file.FileDataBodyPart;

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
	
//	public static String get(String uri, String user, String pass, String mediaType) {
//		Client client = ClientBuilder.newClient();
//		
//		WebTarget service = client.target(uri);
//		if (user != null) {
//			service.register(HttpAuthenticationFeature.basic(user, pass));
//		}
//
//		return service.request(mediaType).get(String.class);
//	}
//	
//	public static String post(String uri, Entity<?> data, String user, String pass, String mediaType) {
//		Client client = ClientBuilder.newClient();
//		
//		WebTarget service = client.target(uri);
//		if (user != null) {
//			service.register(HttpAuthenticationFeature.basic(user, pass));
//		}
//		
//		return service.request(mediaType).post(data, String.class);
//	}
//	
//	public static String postFile(String uri, String filename, String user, String pass, String mediaType) {
//		Client client = Client.create();
//		
//		if (user != null) {
//			client.addFilter(new HTTPBasicAuthFilter(user, pass));
//		}
//		
//		WebResource service = client.resource(uri);
//		File file = new File(filename);
//        
//        String response;
//        if (file.exists()) {
//        	try {
//        		response = service.type(mediaType).accept(mediaType).post(String.class, file);
//        	} catch(UniformInterfaceException e) {
//    			response = "RESTUtils.postFile() Exception: UniformInterfaceException Exception [" + e.getMessage() + "]";
//    			log.error(response);
//    		} catch(ClientHandlerException e) {
//    			response = "RESTUtils.postFile() Exception: ClientHandlerException Exception [" + e.getMessage() + "]";
//    			log.error(response);
//    		}
//        } else {
//        	response = "RESTUtils.postFile() Exception: File of type [" + mediaType + "] DOES NOT EXIST";
//			log.error(response);
//        }
//        
//        client.destroy();
//		
//		return response;
//	}
//	
	public static String putDataFile(String uri, String user, String pass, String mediaType, String filename) {
		ClientConfig clientConfig = new ClientConfig();
		clientConfig.register(MultiPartFeature.class);
		Client client = ClientBuilder.newBuilder().register(MultiPartFeature.class).build();
		
		WebTarget service = client.target(uri);
		if (user != null) {
			service.register(HttpAuthenticationFeature.basic(user, pass));
		}

		File file = new File(filename);
                
        String response;
        if (file.exists()) {
        	FileDataBodyPart fileDataBodyPart = new FileDataBodyPart("uploadFile", file, MediaType.APPLICATION_OCTET_STREAM_TYPE);
        	FormDataMultiPart formDataMultiPart = new FormDataMultiPart();
            formDataMultiPart.bodyPart(fileDataBodyPart);
            response = service.request(mediaType).put(Entity.entity(formDataMultiPart, MediaType.MULTIPART_FORM_DATA), String.class);
        } else {
        	response = "RESTUtils.putDataFile() Exception: File of type [" + mediaType + "] DOES NOT EXIST";
			log.error(response);
        }
        
        client.close();
        
        return response;
	}
	
//	public static String delete(String uri, String user, String pass, String mediaType) {
//		Client client = Client.create();
//		
//		if (user != null) {
//			client.addFilter(new HTTPBasicAuthFilter(user, pass));
//		}
//		
//		WebResource service = client.resource(uri);
//		
//		String response;
//		try {
//			response = service.type(mediaType).accept(mediaType).delete(String.class);
//		} catch(UniformInterfaceException e) {
//			response = "RESTUtils.delete() Exception: UniformInterfaceException Exception [" + e.getMessage() + "]";
//			log.error(response);
//		} catch(ClientHandlerException e) {
//			response = "RESTUtils.delete() Exception: ClientHandlerException Exception [" + e.getMessage() + "]";
//			log.error(response);
//		} finally {
//			client.destroy();
//		}
//		
//		return response;
//	}
}
