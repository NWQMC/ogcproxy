package gov.usgs.wqp.ogcproxy.model.cache;

import java.time.LocalDateTime;
import java.util.List;

import gov.usgs.wqp.ogcproxy.model.OGCRequest;
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
	private String workspace;
	private DynamicLayerStatus currentStatus;
	private SearchParameters<String, List<String>> searchParameters;
	private LocalDateTime dateCreated;
	//This should probably be the "data source" when we have more than WQP
	private OGCServices originatingService;

	//see http://cida-eros-sonar.er.usgs.gov:9001/sonar/coding_rules#rule_key=squid%3AS2445 - this might be one of our issues....
	private Object lockObj = new Object();

	/** 
	 * Constructor for Layers found in Geoserver.
	 * @param layerName - layer name as retrieved from Geoserver.
	 * @param workspace - the workspace this layer resides in on geoserver.
	 */
	public DynamicLayerCache(final String layerName, String workspace) {
		this.layerName = layerName;
		this.workspace = workspace;
		this.currentStatus = DynamicLayerStatus.AVAILABLE;
	}

	/** 
	 * Constructor for Layer requests from WQP.
	 * @param ogcRequest - the normalized request.
	 * @param workspace - the workspace this layer resides in on geoserver.
	 */
	public DynamicLayerCache(final OGCRequest ogcRequest, String workspace) {
		this.layerName = DYNAMIC_LAYER_PREFIX + ogcRequest.getSearchParams().unsignedHashCode();
		this.workspace = workspace;
		this.searchParameters = ogcRequest.getSearchParams();
		this.currentStatus = DynamicLayerStatus.INITIATED;
		this.dateCreated = LocalDateTime.now();
		this.originatingService = ogcRequest.getOgcService();
	}
	
	public String getLayerName() {
		return this.layerName;
	}
	
	public String getQualifiedLayerName() {
		return String.join(":", workspace, layerName); 
	}

	public DynamicLayerStatus getCurrentStatus() {
		synchronized (this.currentStatus) {
			return this.currentStatus;
		}
	}

	public void setCurrentStatus(DynamicLayerStatus currentStatus) {
		synchronized (lockObj) {
			this.currentStatus = currentStatus;
		}
	}

	public SearchParameters<String, List<String>> getSearchParameters() {
		return this.searchParameters;
	}

	public LocalDateTime getDateCreated() {
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

	public void setWorkspace(String workspace) {
		this.workspace = workspace;
	}
}
