package gov.usgs.wqp.ogcproxy.model.cache;

import java.util.Date;
import java.util.List;

import gov.usgs.wqp.ogcproxy.model.ogc.services.OGCServices;
import gov.usgs.wqp.ogcproxy.model.parameters.SearchParameters;
import gov.usgs.wqp.ogcproxy.model.status.DynamicLayerStatus;

/**
 * DynamicLayerCache
 * @author prusso
 *<br /><br />
 *	This class represents a single WQP SimpleStation set of search terms.  It
 *	utilizes the thread-safe Map SimpleParameters in order to keep track of each
 *	search parameter set.
 *<br /><br />
 *	This class can be used by any caching system in order to retain consistent
 *	representations of known search parameter sets.
 */
public class DynamicLayerCache {
	public static final String DYNAMIC_LAYER_PREFIX = "dynamicSites_";

	private String layerName;
	private DynamicLayerStatus currentStatus;
	private SearchParameters<String, List<String>> searchParameters;
	private Date dateCreated;
	private OGCServices originatingService;

	/** 
	 * Constructor for Layers found in Geoserver.
	 * @param layerName - layer name as retrieved from Geoserver.
	 */
	public DynamicLayerCache(final String layerName) {
		this.layerName = layerName;
		this.currentStatus = DynamicLayerStatus.AVAILABLE;
	}

	/** 
	 * Constructor for Layer requests from WQP.
	 * @param searchParams - parsed WQP search parms.
	 * @param originatingService - .
	 */
	public DynamicLayerCache(final SearchParameters<String, List<String>> searchParams, OGCServices originatingService) {
		this.layerName = DYNAMIC_LAYER_PREFIX + searchParams.unsignedHashCode();
		this.searchParameters = searchParams;
		this.currentStatus = DynamicLayerStatus.INITIATED;
		this.dateCreated = new Date();
		this.originatingService = originatingService;
	}
	
	public String getLayerName() {
		return this.layerName;
	}
	
	public void setLayerName(String name) {
		this.layerName = name;
	}

	public DynamicLayerStatus getCurrentStatus() {
		synchronized (this.currentStatus) {
			return this.currentStatus;
		}
	}

	public void setCurrentStatus(DynamicLayerStatus currentStatus) {
		synchronized (this.currentStatus) {
			this.currentStatus = currentStatus;
		}
	}

	public SearchParameters<String, List<String>> getSearchParameters() {
		return this.searchParameters;
	}

	public Date getDateCreated() {
		return dateCreated;
	}

	public OGCServices getOriginatingService() {
		return originatingService;
	}

	public void setOriginatingService(OGCServices originatingService) {
		this.originatingService = originatingService;
	}
	public String getKey() {
		return layerName.replace(DYNAMIC_LAYER_PREFIX, "");
	}
}
