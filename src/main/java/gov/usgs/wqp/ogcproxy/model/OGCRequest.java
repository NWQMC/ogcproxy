package gov.usgs.wqp.ogcproxy.model;

import static org.springframework.util.StringUtils.isEmpty;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import gov.usgs.wqp.ogcproxy.model.ogc.parameters.WFSParameters;
import gov.usgs.wqp.ogcproxy.model.ogc.parameters.WMSParameters;
import gov.usgs.wqp.ogcproxy.model.ogc.services.OGCServices;
import gov.usgs.wqp.ogcproxy.model.parameters.ProxyDataSourceParameter;
import gov.usgs.wqp.ogcproxy.model.parameters.SearchParameters;
import gov.usgs.wqp.ogcproxy.utils.ProxyUtil;

public class OGCRequest {

	public static final String GET_LEGEND_GRAPHIC = "GetLegendGraphic";

	private OGCServices ogcService;
	private Map<String, String> ogcParams;
	private SearchParameters<String, List<String>> searchParams;
	private String requestType;
	private Set<String> replaceableLayers;
	private ProxyDataSourceParameter dataSource;
	private String requestBody;
	
	public OGCRequest(OGCServices ogcService) {
		this.ogcService = ogcService;
		this.ogcParams = new HashMap<>();
		this.searchParams = new SearchParameters<>();
		this.requestType = "";
		this.replaceableLayers = new HashSet<>();
		this.dataSource = ProxyDataSourceParameter.UNKNOWN;
		this.requestBody = "";
	}

	public OGCRequest(OGCServices ogcService, Map<String, String> ogcParams,
			SearchParameters<String, List<String>> searchParams, String requestBody) {
		this.ogcService = ogcService;
		this.ogcParams = null == ogcParams ? new HashMap<>() : ogcParams;
		this.searchParams = null == searchParams ? new SearchParameters<>() : searchParams;
		this.requestBody = null == requestBody ? "" : requestBody;
		
		setRequestType(this.ogcParams);
		setReplaceableLayersAndDataSource(this.ogcService, this.ogcParams, this.requestType);
	}
	
	protected void setRequestType(Map<String, String> ogcParams) {
		requestType = "";
		if (null != ogcParams && !ogcParams.isEmpty()) {
			requestType = ogcParams.get(ProxyUtil.getCaseSensitiveParameter("request", ogcParams.keySet()));
		}
		if (null == requestType) {
			requestType = "";
		}
	}
	
	protected void setReplaceableLayersAndDataSource(OGCServices ogcService, Map<String, String> ogcParams, String ogcRequestType) {
		// Lets see if the typeName, layers and/or query_layers parameter is what
		// we are expecting and decide what to do depending on its value.
		// We need to capture the pure servlet parameter key for our searchParams parameter.
		// Since this can be case INSENSITIVE but we use its value as a key in a map, we need
		// to know what the exact character sequence is going forward.
		replaceableLayers = new HashSet<>();
		dataSource = ProxyDataSourceParameter.UNKNOWN;
		String replaceableLayer = "";
		
		if (null != ogcService) {
			switch (ogcService) {
	        case WFS:
	        	//WFS 1.1.0 and earlier
	        	replaceableLayer = checkIfApplicable(ogcParams, WFSParameters.typeName.toString());
	        	//After WFS 1.1.0
	        	if (isEmpty(replaceableLayer)) {
	        		replaceableLayer = checkIfApplicable(ogcParams, WFSParameters.typeNames.toString());
	        	}
	    		break;
	        case WMS:
	        	if (GET_LEGEND_GRAPHIC.equalsIgnoreCase(ogcRequestType)) {
	        		replaceableLayer = checkIfApplicable(ogcParams, WMSParameters.layer.toString());
	        	} else {
	        		replaceableLayer = checkIfApplicable(ogcParams, WMSParameters.layers.toString());
	        		//Several WMS calls contain an additional query_layer parameter. We expect this to match the layers parameter
	        		//when present.
	        		String queryLayer = checkIfApplicable(ogcParams, WMSParameters.query_layers.toString());
	            	if (!isEmpty(queryLayer)) {
	            		replaceableLayers.add(queryLayer);
	            	}
	       	}
	            break;
	        default:
	        	break;
			}
		}

    	if (!isEmpty(replaceableLayer)) {
    		//We found either a typeName, typeNames, layer, or layers parameter - add it to our list if we recognize the value.
    		replaceableLayers.add(replaceableLayer);
    	}
	}

	/** 
	 * Check to see if the value for the parameter is one of our dynamic queries, or just one we can pass through.
	 * Be aware that the instance dataSource will be set if we finad the correct match!
	 *  
	 * @param ogcParams - Map of OGC parameters and values from the request.
	 * @param parameterName - The parameter to find in the Map.
	 * @return The exact parameter key from the map - to be used in case sensitive matching later.
	 */
	protected String checkIfApplicable(Map<String, String> ogcParams, String parameterName) {
		String rtn = "";
		if (null != ogcParams && !ogcParams.isEmpty()) {
			String key = ProxyUtil.getCaseSensitiveParameter(parameterName, ogcParams.keySet());
			String value = ogcParams.get(key);
			if (!isEmpty(value)) {
				for (String i : value.split(",")) {
					ProxyDataSourceParameter dataSourceCheck = ProxyDataSourceParameter.getTypeFromString(i);
					if (dataSourceCheck != ProxyDataSourceParameter.UNKNOWN) {
						rtn = key;
						dataSource = dataSourceCheck;
					}
				}
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

	public Set<String> getReplaceableLayers() {
		return replaceableLayers;
	}

	public String getRequestBody() {
		return requestBody;
	}

	public boolean isValidVendorRequest() {
		return !searchParams.isEmpty() && dataSource != ProxyDataSourceParameter.UNKNOWN;
	}

	public void setLayerFromVendor(String layerName) {
		/*
		 * We finally got a layer name (and its been added to GeoServer, lets
		 * add this layer to the layer parameter in the OGC request
		 */
		for (String param : replaceableLayers) {
			StringBuilder newValue = new StringBuilder("");
			String sep = "";
			String value = ogcParams.get(param);
			for (String i : value.split(",")) {
				if (dataSource.toString().equalsIgnoreCase(i)) {
					newValue.append(sep).append(layerName);
				} else {
					newValue.append(sep).append(i);
				}
				sep = ",";
			}
			ogcParams.put(param, newValue.toString());
		}
		
		requestBody = requestBody.replaceAll(ProxyDataSourceParameter.WQP_SITES.toString(), layerName);
	}

}
