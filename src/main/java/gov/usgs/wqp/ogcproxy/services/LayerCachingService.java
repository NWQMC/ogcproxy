package gov.usgs.wqp.ogcproxy.services;

import static org.springframework.util.StringUtils.isEmpty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import gov.usgs.wqp.ogcproxy.exceptions.OGCProxyException;
import gov.usgs.wqp.ogcproxy.model.OGCRequest;
import gov.usgs.wqp.ogcproxy.model.cache.DynamicLayerCache;
import gov.usgs.wqp.ogcproxy.model.status.DynamicLayerStatus;
import gov.usgs.wqp.ogcproxy.services.wqp.WQPDynamicLayerCachingService;
import gov.usgs.wqp.ogcproxy.services.wqp.WQPLayerBuildingService;

public class LayerCachingService {
	private static final Logger LOG = LoggerFactory.getLogger(LayerCachingService.class);

	@Autowired
	private WQPLayerBuildingService wqpLayerBuildingService;

	@Autowired
	private WQPDynamicLayerCachingService wqpLayerCachingService;
	
	//TODO get from properties
	private static String geoserverWorkspace = "qw_portal_map";

	private static final LayerCachingService INSTANCE = new LayerCachingService();

	/**
	 * Private Constructor for Singleton Pattern
	 */
	private LayerCachingService() {
	}

	/**
	 * Singleton access
	 *
	 * @return LayerCachingService instance
	 */
	public static LayerCachingService getInstance() {
		return INSTANCE;
	}

	/** 
	 * Entry point for our vendor-specific searchParams layer building. If all goes well, there will be a layer created based 
	 * on the searchParams and uploaded to geoserver for use in completing this call and others with the same base parameters. 
	 * @param ogcRequest The parsed request and background information from the requesting service.
	 * @return the layer name if successful, otherwise and empty string.
	 */
	public String getDynamicLayer(OGCRequest ogcRequest) {
		DynamicLayerCache layerCache = null;
		String layerName = "";
		
		try {
			DynamicLayerCache defaultLayerCache = new DynamicLayerCache(ogcRequest.getSearchParams(),
					ogcRequest.getOgcService(), geoserverWorkspace);
			layerCache = wqpLayerCachingService.getLayerCache(defaultLayerCache);
			
			/*
			 * We should be blocked above with the getLayerCache() call and should only
			 * get a value for layerCache when its finished performing an action.  The
			 * valid non-action status's are AVAILABLE (default), INITIAL, EMPTY and ERROR
			 */
			switch (layerCache.getCurrentStatus()) {
				case INITIATED:
					LOG.debug("WQPLayerBuildingService.getDynamicLayer() Created new DynamicLayerCache for key [" +
							ogcRequest.getSearchParams().unsignedHashCode() +"].  Setting status to BUILDING and creating layer...");
					
					/*
					 * We just created a new cache object.  This means there is no
					 * layer currently for this request in our GeoServer.  We now
					 * need to make this layer through the WMSLayerService.
					 */
					layerCache.setCurrentStatus(DynamicLayerStatus.BUILDING);
					
					/*
					 * We now call the simplestation url with our search params
					 * (along with a mimeType=xml) in order to retrieve the data
					 * that creates the layer:
					 *
					 * 		http://www.waterqualitydata.us/simplestation/search?countycode=US%3A40%3A109&characteristicName=Atrazine&mimeType=xml
					 *
					 * Documentation is from http://waterqualitydata.us/webservices_documentation.jsp
					 * except we call "simplestation" instead of "Station"
					 */
					//TODO - rather than giving string, maybe just throw exception if problems in buildDynamicLayer
					layerName = wqpLayerBuildingService.buildDynamicLayer(ogcRequest.getSearchParams());
					if (isEmpty(layerName)) {
						layerCache.setCurrentStatus(DynamicLayerStatus.EMPTY);
						
						LOG.debug("WQPLayerBuildingService.getDynamicLayer() Unable to create layer [" + layerCache.getLayerName() +
								"] for key ["+ ogcRequest.getSearchParams().unsignedHashCode() +
								"].  Its status is [" + layerCache.getCurrentStatus().toString() +
								"].  Since it is an empty request this means the search parameters did not " +
								"result in any matching criteria.");
					} else {
					
						//TODO Also check to see if the layer is enabled in GeoServer...
						
						layerCache.setCurrentStatus(DynamicLayerStatus.AVAILABLE);
						
						LOG.debug("WQPLayerBuildingService.getDynamicLayer() Finished building layer for key ["+
								ogcRequest.getSearchParams().unsignedHashCode() +
								"].  Layer name is [" + layerCache.getLayerName() + "].  Setting status to " +
								"AVAILABLE and continuing on to GeoServer WMS request...");
					}
					break;
				
				case EMPTY:
					LOG.debug("WQPLayerBuildingService.getDynamicLayer() Retrieved layer name [" + layerCache.getLayerName() +
							"] for key ["+ ogcRequest.getSearchParams().unsignedHashCode() +
							"] and its status is [" + layerCache.getCurrentStatus().toString() +
							"].  Since it is an empty request this means the search parameters did not " +
							"result in any matching criteria.");
					break;
				
				case ERROR:
					LOG.error("WQPLayerBuildingService.getDynamicLayer() Error: Layer cache is in an ERROR state and cannot continue request.");
					break;
				
				default:
					LOG.debug("WQPLayerBuildingService.getDynamicLayer() Retrieved layer name [" + layerCache.getLayerName() +
							"] for key ["+ ogcRequest.getSearchParams().unsignedHashCode() +
							"] and its status is [" + layerCache.getCurrentStatus().toString() +
							"].  Continuing on to GeoServer WMS request...");
					break;
			}
		} catch (OGCProxyException e) {
			if (layerCache != null) {
				layerCache.setCurrentStatus(DynamicLayerStatus.ERROR);
				wqpLayerCachingService.removeLayerCache(layerCache.getSearchParameters().unsignedHashCode() + "");
			}
			
			LOG.error("WQPLayerBuildingService.getDynamicLayer() Error: Layer was not created for search parameters.", e);
		}
		
		return null == layerCache ? "" : layerCache.getQualifiedLayerName();
	}

}
