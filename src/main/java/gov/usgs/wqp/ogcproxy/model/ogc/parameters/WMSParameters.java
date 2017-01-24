package gov.usgs.wqp.ogcproxy.model.ogc.parameters;

/**
 * WMSParameters
 * http://docs.geoserver.org/stable/en/user/services/wms/reference.html
 */
public enum WMSParameters {
	service,
	version,
	request,
	namespace,
	layer,
	layers,
	style,
	styles,
	srs,
	crs,
	bbox,
	width,
	height,
	format,
	transparent,
	bgcolor,
	exceptions,
	time,
	sld,
	sld_body,
	query_layers,
	info_format,
	feature_count,
	featuretype,
	x,
	i,
	y,
	j,
	buffer,
	cql_filter,
	filter,
	propertyName,
	rule,
	scale,
	viewparams;

	
	public String toString() {
		switch (this) {
			case crs: {
				return "srs";	//  To support < WMS 1.3.0
			}
			case i: {
				return "x";  // To support < WMS 1.3.0
			}
			case j: {
				return "y";  // To support < WMS 1.3.0
			}
			default: {
				return super.toString();
			}
		}
	}
	
	
	public static boolean isRequired(WMSParameters type) {
		switch (type) {
			case service:
			case version:
			case request:
			case layer:
			case layers:
			case styles:
			case srs:
			case bbox:
			case width:
			case height:
			case format:
			case query_layers:
			case x:
			case y:
				return true;
			
			default:
				return false;
		}
	}
}
