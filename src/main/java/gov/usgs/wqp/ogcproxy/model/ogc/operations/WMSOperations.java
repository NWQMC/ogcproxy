package gov.usgs.wqp.ogcproxy.model.ogc.operations;

import static gov.usgs.wqp.ogcproxy.model.ogc.parameters.WMSParameters.*;

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
			case GetCapabilities:
				return Arrays.asList(service, version, request, namespace);
	
			case GetMap:
				return Arrays.asList(service, version, request, layers, styles,
						srs, crs, bbox, width, height, format, transparent,
						bgcolor, exceptions, time, sld, sld_body);
	
			case GetFeatureInfo:
				return Arrays.asList(service, version, request, layers,
						styles, srs, crs, bbox, width, height, query_layers,
						info_format, feature_count, x, i, y, j, exceptions,
						buffer, cql_filter, filter, propertyName);
	
			case DescribeLayer:
				return Arrays.asList(service, version, request, layers, exceptions);
	
			case GetLegendGraphic:
				return Arrays.asList(request, layer, style, featuretype, rule,
						scale, sld, sld_body, format, width, height, exceptions);
			
			case Exceptions:
			default:
				return new ArrayList<WMSParameters>();

		}
	}
	
	public static List<WMSParameters> getRequiredParameters(WMSOperations type) {
		List<WMSParameters> params = WMSOperations.getParameters(type);
		List<WMSParameters> requiredParams = new ArrayList<WMSParameters>();
		
		for(WMSParameters param : params) {
			if (isRequired(param)) {
				requiredParams.add(param);
			}
		}
		
		return requiredParams;
	}
}
