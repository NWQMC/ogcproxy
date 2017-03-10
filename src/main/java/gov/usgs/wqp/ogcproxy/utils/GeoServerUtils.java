package gov.usgs.wqp.ogcproxy.utils;

import java.io.File;
import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import gov.usgs.wqp.ogcproxy.exceptions.OGCProxyException;
import gov.usgs.wqp.ogcproxy.exceptions.OGCProxyExceptionID;
import gov.usgs.wqp.springinit.GeoServerConfig;

@Component
public class GeoServerUtils {
	private static final Logger LOG = LoggerFactory.getLogger(GeoServerUtils.class);

	@Autowired
	CloseableHttpClientFactory closeableHttpClientFactory;
	@Autowired
	@Qualifier("geoserverBaseURI")
	private String geoserverBaseURI;
	@Autowired
	@Qualifier("geoserverWorkspace")
	private String geoserverWorkspace;
	@Autowired
	@Qualifier("geoserverWorkspaceBase")
	private String geoserverWorkspaceBase;
	@Autowired
	@Qualifier("geoserverDataStoreBase")
	private String geoserverDataStoreBase;
	@Autowired
	@Qualifier("geoserverHost")
	private String geoserverHost;
	@Autowired
	@Qualifier("geoserverPort")
	private String geoserverPort;
	@Autowired
	@Qualifier("geoserverUser")
	private String geoserverUser;
	@Autowired
	@Qualifier("geoserverPass")
	private String geoserverPass;
	@Autowired
	@Qualifier("geoserverProtocol")
	private String geoserverProtocol;
	@Autowired
	@Qualifier("geoserverCatchupTime")
	private long geoserverCatchupTime;

	public static String JSON_EXT = ".json";

	public String buildNamespacePost() {
		return String.join("/", geoserverBaseURI, GeoServerConfig.GEOSERVER_REST, GeoServerConfig.GEOSERVER_NAMPESPACES);
	}

	public String buildDataStoreRestGet() {
		return geoserverDataStoreBase + JSON_EXT;
	}

	public String buildShapeFileRestPut(String layerName) {
		return geoserverDataStoreBase + "/" + layerName + "/file.shp";
	}

	public String buildResourceRestDelete() {
		return String.join("/", geoserverBaseURI, GeoServerConfig.GEOSERVER_REST, GeoServerConfig.GEOSERVER_RESOURCE, GeoServerConfig.GEOSERVER_DATA, geoserverWorkspace);
	}

	public String buildWorkspaceRestDelete() {
		return geoserverWorkspaceBase + "?recurse=true";
	}

	public String buildWorkspacesRestGet() {
		return geoserverWorkspaceBase + JSON_EXT;
	}

	public CloseableHttpClient buildAuthorizedClient() {
		CredentialsProvider credentialsProvider =
				closeableHttpClientFactory.getCredentialsProvider(geoserverHost, geoserverPort, geoserverUser, geoserverPass);
		return closeableHttpClientFactory.getAuthorizedCloseableHttpClient(credentialsProvider);
	}

	public HttpClientContext buildLocalContext() {
		return closeableHttpClientFactory.getPreemptiveAuthContext(geoserverHost, geoserverPort, geoserverProtocol);
	}

	public void uploadShapefile(CloseableHttpClient geoserverClient, String shapefileDirectory, String layerName) throws OGCProxyException {
		/*
		 * Upload the zipped shapefile
		 *
		 * Build the REST URI for uploading shapefiles.  Looks like:
		 * 		http://HOST:PORT/CONTEXT/rest/workspaces/WORKSPACE_NAME/datastores/LAYER_NAME/file.shp
		 *
		 * Where the "file.shp" is a GeoServer syntax that is required but does not change per request.
		 * We also duplicate the LAYER_NAME in the datastore path so we can let GeoServer separate the
		 * shapefiles easily between directories (it has an issue w/ tons of shapefiles in a single directory
		 * like what we did w/ the site_map datastore).
		 */
		String layerZipFile = shapefileDirectory + File.separator + layerName + ".zip";
		LOG.debug("GeoServerUtils.uploadShapefile() INFO: o ----- Uploading Shapefile (" + layerZipFile + ") to GeoServer");
		File file = new File(layerZipFile);
		if (file.exists()) {
			try {
				HttpClientContext localContext = buildLocalContext();
				verifyWorkspaceExists(geoserverClient, localContext);
				putShapefile(geoserverClient, localContext, layerName, SystemUtils.MEDIATYPE_APPLICATION_ZIP, file);
			} finally {
				if (!file.delete()) {
					LOG.debug("troubles deleting " + layerZipFile);
				}
			}
		} else {
			OGCProxyExceptionID id = OGCProxyExceptionID.UPLOAD_SHAPEFILE_ERROR;
			throw new OGCProxyException(id, "GeoServerUtils", "uploadShapefile()", "Exception: File [" + layerZipFile + "] DOES NOT EXIST");
		}
		LOG.debug("GeoServerUtils.uploadShapefile() INFO: o ----- Uploading Shapefile Complete");

		/*
		 * Let GeoServer catch up with its dataset ingestion.
		 * 		GeoServer has a race condition from when
		 * 		it finished uploading a shapefile to when
		 * 		the layer and datasource for that shapefile
		 * 		are available.  This is a wait time before
		 * 		we mark the layer AVAILABLE.
		 */
		try {
			Thread.sleep(geoserverCatchupTime);
		} catch (InterruptedException e) {
			LOG.warn("GeoServerUtils.uploadShapefile() caught InterruptedException when running the GeoServer Catchup Time sleep.  Continuing...", e);
		}

		/*
		 * TODO:
		 *
		 * The final step is to force GeoServer to "enable" the layer.  Sometimes
		 * we have seen GeoServer correctly upload and accept a ShapeFile along
		 * with building the datastore and layer but forget to "Enable" the layer
		 * in its memory.
		 */

	}

	protected void verifyWorkspaceExists(CloseableHttpClient geoserverClient, HttpClientContext localContext) throws OGCProxyException {
		int statusCode = -1;
		String workspaceURI = buildWorkspacesRestGet();
		CloseableHttpResponse response = null;
		try {
			response = geoserverClient.execute(new HttpGet(workspaceURI), localContext);
			statusCode = response.getStatusLine().getStatusCode();
		} catch (Exception e){
			LOG.error(e.getLocalizedMessage(), e);
			throw new OGCProxyException(OGCProxyExceptionID.UPLOAD_SHAPEFILE_ERROR, "GeoServerUtils", "verifyWorkspaceExists()", 
					"Exception: " + e.getLocalizedMessage());
		} finally {
			try {
				if (null != response) {
					response.close();
				}
			} catch (IOException e1) {
				LOG.error("Trouble closing geoserver workspace verification response", e1);
			}
		}
		if (HttpStatus.SC_NOT_FOUND == statusCode) {
			createWorkspace(geoserverClient, localContext);
		}
	}

	protected void putShapefile(CloseableHttpClient geoserverClient, HttpClientContext localContext,
			String layerName, String mediaType, File file) throws OGCProxyException {
		int statusCode = -1;
		CloseableHttpResponse response = null;
		try {
			HttpPut httpPut = new HttpPut(buildShapeFileRestPut(layerName));
			HttpEntity fileEntity = new FileEntity(file, ContentType.create(mediaType));
			httpPut.setEntity(fileEntity);
	
			response = geoserverClient.execute(httpPut, localContext);
			statusCode = response.getStatusLine().getStatusCode();
			LOG.debug("GeoServerUtils.putShapefile() INFO: \nGeoServer response for request [" + httpPut.getURI()
				+ "] is: \n[" + statusCode + "]");
		} catch (Exception e) {
			LOG.error(e.getLocalizedMessage(), e);
			throw new OGCProxyException(OGCProxyExceptionID.UPLOAD_SHAPEFILE_ERROR, "GeoServerUtils", "putShapefile()", 
					"Exception: " + e.getLocalizedMessage());
		} finally {
			try {
				if (null != response) {
					response.close();
				}
			} catch (IOException e1) {
				LOG.error("Trouble closing geoserver workspace verification response", e1);
			}
		}
		if (HttpStatus.SC_CREATED != statusCode) {
			throw new OGCProxyException(OGCProxyExceptionID.UPLOAD_SHAPEFILE_ERROR, "GeoServerUtils", "putShapefile()", 
					"Exception: Invalid status code from geoserver:" + statusCode);
		}
	}

	public void createWorkspace(CloseableHttpClient geoserverClient, HttpClientContext localContext) throws OGCProxyException {
		/*
		 * We will actually try to create a namespace (which will automatically create the workspace)
		 */
		String mediaType = "text/xml";
		String object = "<namespace><prefix>" + geoserverWorkspace
				+ "</prefix><uri>http://www.waterqualitydata.us/ogcservices</uri></namespace>";
		int statusCode = -1;
		CloseableHttpResponse response = null;
		try {
			HttpPost httpPost = new HttpPost(buildNamespacePost());
			HttpEntity httpEntity = new StringEntity(object);
			httpPost.setEntity(httpEntity);
			httpPost.addHeader("content-type", mediaType);
			response = geoserverClient.execute(httpPost, localContext);
			statusCode = response.getStatusLine().getStatusCode();
		} catch (Exception e) {
			LOG.error(e.getLocalizedMessage(), e);
			throw new OGCProxyException(OGCProxyExceptionID.UPLOAD_SHAPEFILE_ERROR, "GeoServerUtils", "createWorkspace()", 
					"Exception: " + e.getLocalizedMessage());
		} finally {
			try {
				if (null != response) {
					response.close();
				}
			} catch (IOException e1) {
				LOG.error("Trouble closing geoserver workspace verification response", e1);
			}
		}
		if (HttpStatus.SC_CREATED != statusCode) {
			throw new OGCProxyException(OGCProxyExceptionID.UPLOAD_SHAPEFILE_ERROR, "GeoServerUtils", "createWorkspace()", 
					"Exception: Invalid status code from geoserver:" + statusCode);
		}

	}

}
