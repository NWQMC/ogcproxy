package gov.usgs.wqp.ogcproxy.model;

import static org.springframework.util.StringUtils.isEmpty;

import java.util.List;
import java.util.Map;

import gov.usgs.wqp.ogcproxy.model.ogc.parameters.WFSParameters;
import gov.usgs.wqp.ogcproxy.model.ogc.parameters.WMSParameters;
import gov.usgs.wqp.ogcproxy.model.ogc.services.OGCServices;
import gov.usgs.wqp.ogcproxy.model.parameters.ProxyDataSourceParameter;
import gov.usgs.wqp.ogcproxy.model.parameters.SearchParameters;
import gov.usgs.wqp.ogcproxy.utils.ProxyUtil;

public class OGCRequest {

	private OGCServices ogcService;
	private Map<String, String> ogcParams;
	private SearchParameters<String, List<String>> searchParams;
	private String requestType;
	private String replaceableLayer;
	private ProxyDataSourceParameter dataSource;
	
	public OGCRequest(OGCServices ogcService) {
		this.ogcService = ogcService;
	}

	public OGCRequest(OGCServices ogcService, Map<String, String> ogcParams,
			SearchParameters<String, List<String>> searchParams) {
		this.ogcService = ogcService;
		this.ogcParams = ogcParams;
		this.searchParams = searchParams;
		
		setRequestType(this.ogcParams);
		setReplaceableLayer(this.ogcService, this.ogcParams, this.requestType);
		this.dataSource = ProxyDataSourceParameter.getTypeFromString(ogcParams.get(this.replaceableLayer));
	}
	
	private void setRequestType(Map<String, String> ogcParams) {
		requestType = ogcParams.get(ProxyUtil.getCaseSensitiveParameter("request", ogcParams));
		if (requestType == null) {
			requestType = "";
		}
	}
	
	private void setReplaceableLayer(OGCServices ogcService, Map<String, String> ogcParams, String ogcRequestType) {
		//TODO - THIS DOES NOT HANDLE MULTIPLE LAYERS ON THE ORIGINAL REQUEST - layers=wqp_sites,xyz_sites
		//TODO - OR MULTIPLE LAYER PARMS - layers=wqp_sites&query_layers=xyz_sites
		// Lets see if the layers and/or queryParameter parameter is what
		// we are expecting and decide what to do depending on its value.
		// We need to capture the pure servlet parameter key for our searchParams parameter.
		// Since this can be case INSENSITIVE but we use its value as a key in a map, we need
		// to know what the exact character sequence is going forward.
		replaceableLayer = null;
		
		switch (ogcService) {
        case WFS:
        	//WFS 1.1.0 and earlier
        	replaceableLayer = checkIfApplicable(ogcParams, WFSParameters.typeName.toString());
        	//After WFS 1.1.0
        	if (null == replaceableLayer) {
        		replaceableLayer = checkIfApplicable(ogcParams, WFSParameters.typeNames.toString());
        	}
    		break;
        case WMS:
        	if ("GetLegendGraphic".equalsIgnoreCase(ogcRequestType)) {
        		replaceableLayer = checkIfApplicable(ogcParams, WMSParameters.layer.toString());
        		//TODO Make this more generic (not WQP specific)
        		ogcParams.put(WMSParameters.style.toString(), "wqp_sources");
        	} else {
        		replaceableLayer = checkIfApplicable(ogcParams, WMSParameters.layers.toString());
            	if (null == replaceableLayer) {
            		replaceableLayer = checkIfApplicable(ogcParams, WMSParameters.query_layers.toString());
            	}
        	}
            break;
        default:
        	break;
		}
	}

	private String checkIfApplicable(Map<String, String> ogcParams, String parameterName) {
		String rtn = null;
		String key = ProxyUtil.getCaseSensitiveParameter(parameterName, ogcParams);
		String value = ogcParams.get(key);
		if (!isEmpty(value)) {
			ProxyDataSourceParameter dataSource = ProxyDataSourceParameter.getTypeFromString(value);
			if (dataSource != ProxyDataSourceParameter.UNKNOWN) {
				rtn = key;
			}
		}
		return rtn;
	}

	public OGCServices getOgcService() {
		return ogcService;
	}

	public Map<String, String> getOgcParams() {
		return ogcParams;
	}

	public SearchParameters<String, List<String>> getSearchParams() {
		return searchParams;
	}

	public String getRequestType() {
		return requestType;
	}

	public ProxyDataSourceParameter getDataSource() {
		return dataSource;
	}

	public boolean isValidRequest() {
		return !searchParams.isEmpty() && dataSource != ProxyDataSourceParameter.UNKNOWN;
	}

	public void setLayerFromVendor(String layerName) {
		/*
		 * We finally got a layer name (and its been added to GeoServer, lets
		 * add this layer to the layer parameter in the OGC request
		 */
		//TODO - see note when determining replacableLayerParam
		String currentLayer = ogcParams.get(replaceableLayer);
		if (isEmpty(currentLayer) || (currentLayer.equals(dataSource.toString())) ) {
			currentLayer = layerName;
		} else {
			currentLayer += "," + layerName;
		}
		ogcParams.put(replaceableLayer, currentLayer);
	}

}
