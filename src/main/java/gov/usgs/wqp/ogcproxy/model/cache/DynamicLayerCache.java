package gov.usgs.wqp.ogcproxy.model.cache;

import gov.usgs.wqp.ogcproxy.model.ogc.services.OGCServices;
import gov.usgs.wqp.ogcproxy.model.parameters.SearchParameters;
import gov.usgs.wqp.ogcproxy.model.status.DynamicLayerStatus;

import java.util.Date;
import java.util.List;

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
	private String layerName;
	private DynamicLayerStatus currentStatus;
	private SearchParameters<String, List<String>> searchParameters;
	private Date dateCreated;
	private OGCServices originatingService;
	
	
	public DynamicLayerCache(final SearchParameters<String, List<String>> searchParams, OGCServices originatingService) {
		this.layerName = searchParams.unsignedHashCode() + "";
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

	public void updateSearchParameters(
			SearchParameters<String, List<String>> searchParams) {
		this.layerName = searchParams.unsignedHashCode() + "";
		this.searchParameters = searchParams;
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
}
