package gov.usgs.wqp.ogcproxy.model;

import java.util.Date;
import java.util.List;

import gov.usgs.wqp.ogcproxy.model.ogc.services.OGCServices;
import gov.usgs.wqp.ogcproxy.model.parameters.SearchParameters;

public class DynamicLayer {
	public static final String DYNAMIC_LAYER_PREFIX = "dynamicSites_";

	private final String layerName;
	private final String workspace;
	private final SearchParameters<String, List<String>> searchParameters;
	private final Date dateCreated;
	private final OGCServices originatingService;

	/** 
	 * Constructor for Layers found in Geoserver.
	 * @param layerName - layer name as retrieved from Geoserver.
	 * @param workspace - the workspace this layer resides in on geoserver.
	 */
	public DynamicLayer(final String layerName, String workspace) {
		this.layerName = layerName;
		this.workspace = workspace;
		this.searchParameters = new SearchParameters<>();
		this.dateCreated = null;
		this.originatingService = null;
	}

	/** 
	 * Constructor for Layer requests from WQP.
	 * @param ogcRequest - the normalized request.
	 * @param workspace - the workspace this layer resides in on geoserver.
	 */
	public DynamicLayer(final OGCRequest ogcRequest, String workspace) {
		this.layerName = buildLayerName(ogcRequest);
		this.workspace = workspace;
		this.searchParameters = ogcRequest.getSearchParams();
		this.dateCreated = new Date();
		this.originatingService = ogcRequest.getOgcService();
	}

	public static String buildLayerName(OGCRequest ogcRequest) {
		return DYNAMIC_LAYER_PREFIX + ogcRequest.getSearchParams().unsignedHashCode();
	}

	public String getLayerName() {
		return this.layerName;
	}

	public String getQualifiedLayerName() {
		return String.join(":", workspace, layerName); 
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
}
