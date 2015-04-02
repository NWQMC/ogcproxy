package gov.usgs.wqp.ogcproxy.model.ogc.operations;

import gov.usgs.wqp.ogcproxy.model.ogc.parameters.WMSParameters;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * WMSOperations
 * @author prusso
 *<br /><br />
 *	This enumeration describes all of the standard WMS Operations
 *<br/>
 *	TODO DEAD CODE - This class is not referenced
 */
public enum WMSOperations {
	/**
	 * http://docs.geoserver.org/stable/en/user/services/wms/reference.html
	 * 
	 */
	Exceptions,
	GetCapabilities,
	GetMap,
	GetFeatureInfo,
	DescribeLayer,
	GetLegendGraphic,
	UNKNOWN;
	
	public static List<WMSParameters> getParameters(WMSOperations type) {
		
		switch (type) {
			case Exceptions: {
				return new ArrayList<WMSParameters>();
			}
			case GetCapabilities: {
				return Arrays.asList(WMSParameters.service, WMSParameters.version, WMSParameters.request, WMSParameters.namespace);
			}
	
			case GetMap: {
				return Arrays.asList(WMSParameters.service, WMSParameters.version, WMSParameters.request, WMSParameters.layers, WMSParameters.styles,
						WMSParameters.srs, WMSParameters.crs, WMSParameters.bbox, WMSParameters.width, WMSParameters.height, WMSParameters.format,
						WMSParameters.transparent, WMSParameters.bgcolor, WMSParameters.exceptions, WMSParameters.time, WMSParameters.sld,
						WMSParameters.sld_body);
			}
	
			case GetFeatureInfo: {
				return Arrays.asList(WMSParameters.service, WMSParameters.version, WMSParameters.request, WMSParameters.layers,
						WMSParameters.styles, WMSParameters.srs, WMSParameters.crs, WMSParameters.bbox, WMSParameters.width, WMSParameters.height,
						WMSParameters.query_layers, WMSParameters.info_format, WMSParameters.feature_count, WMSParameters.x, WMSParameters.i,
						WMSParameters.y, WMSParameters.j, WMSParameters.exceptions, WMSParameters.buffer, WMSParameters.cql_filter,
						WMSParameters.filter, WMSParameters.propertyName);
			}
	
			case DescribeLayer: {
				return Arrays.asList(WMSParameters.service, WMSParameters.version, WMSParameters.request, WMSParameters.layers, WMSParameters.exceptions);
			}
	
			case GetLegendGraphic: {
				return Arrays.asList(WMSParameters.request, WMSParameters.layer, WMSParameters.style, WMSParameters.featuretype, WMSParameters.rule,
						WMSParameters.scale, WMSParameters.sld, WMSParameters.sld_body, WMSParameters.format, WMSParameters.width, WMSParameters.height,
						WMSParameters.exceptions);
			}
			
			default: {
				return new ArrayList<WMSParameters>();
			}
		}
	}
	
	public static List<WMSParameters> getRequiredParameters(WMSOperations type) {
		List<WMSParameters> params = WMSOperations.getParameters(type);
		List<WMSParameters> requiredParams = new ArrayList<WMSParameters>();
		
		for(WMSParameters param : params) {
			if(WMSParameters.isRequired(param)) {
				requiredParams.add(param);
			}
		}
		
		return requiredParams;
	}
}
