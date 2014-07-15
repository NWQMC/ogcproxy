package gov.usgs.wqp.ogcproxy.model.ogc.parameters;

/**
 * WMSParameters
 * @author prusso
 *<br /><br />
 *	This enumeration was created at the beginning of the WMSProxy design but 
 *	once development got past its prototyping stage, it was deemed not needed
 *	for current operation.
 *<br /><br />
 *	It has been left in the WMSProxy codebase due to possible future necessity
 *	of declaring and filtering on WMS Parameters.
 */
public enum WMSParameters {
	/**
	 * http://docs.geoserver.org/stable/en/user/services/wms/reference.html
	 */
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
	viewparams,
	UNKNOWN;

	public static WMSParameters getTypeFromString(String string) {
		if (string.equals("service")) {
			return service;
		}
		
		if (string.equals("version")) {
			return version;
		}
		
		if (string.equals("request")) {
			return request;
		}
		
		if (string.equals("namespace")) {
			return namespace;
		}
		
		if (string.equals("layer")) {
			return layer;
		}
		
		if (string.equals("layers")) {
			return layers;
		}
		
		if (string.equals("style")) {
			return style;
		}
		
		if (string.equals("styles")) {
			return styles;
		}
		
		if (string.equals("srs")) {
			return crs;
		}
		
		if (string.equals("crs")) {
			return crs;
		}
		
		if (string.equals("bbox")) {
			return bbox;
		}
		
		if (string.equals("width")) {
			return width;
		}
		
		if (string.equals("height")) {
			return height;
		}
		
		if (string.equals("format")) {
			return format;
		}
		
		if (string.equals("transparent")) {
			return transparent;
		}
		
		if (string.equals("bgcolor")) {
			return bgcolor;
		}
		
		if (string.equals("exceptions")) {
			return exceptions;
		}
		
		if (string.equals("time")) {
			return time;
		}
		
		if (string.equals("sld")) {
			return sld;
		}
		
		if (string.equals("sld_body")) {
			return sld_body;
		}
		
		if (string.equals("query_layers")) {
			return query_layers;
		}
		
		if (string.equals("info_format")) {
			return info_format;
		}
		
		if (string.equals("feature_count")) {
			return feature_count;
		}
		
		if (string.equals("featuretype")) {
			return featuretype;
		}
		
		if (string.equals("x")) {
			return x;
		}
		
		if (string.equals("i")) {
			return i;
		}
		
		if (string.equals("y")) {
			return y;
		}
		
		if (string.equals("j")) {
			return j;
		}
		
		if (string.equals("buffer")) {
			return buffer;
		}
		
		if (string.equals("cql_filter")) {
			return cql_filter;
		}
		
		if (string.equals("filter")) {
			return filter;
		}
		
		if (string.equals("propertyName")) {
			return propertyName;
		}
		
		if (string.equals("rule")) {
			return rule;
		}
		
		if (string.equals("scale")) {
			return scale;
		}
		
		if (string.equals("viewparams")) {
			return viewparams;
		}

		return UNKNOWN;
	}

	public static String getStringFromType(WMSParameters type) {
		switch (type) {
			case service: {
				return "service";
			}
			
			case version: {
				return "version";
			}
			
			case request: {
				return "request";
			}
			
			case namespace: {
				return "namespace";
			}
			
			case layer: {
				return "layer";
			}
			
			case layers: {
				return "layers";
			}
			
			case style: {
				return "style";
			}
			
			case styles: {
				return "styles";
			}
			
			case srs: {
				return "srs";
			}
			
			case crs: {
				return "srs";	//  To support < WMS 1.3.0
			}
			
			case bbox: {
				return "bbox";
			}
			
			case width: {
				return "width";
			}
			
			case height: {
				return "height";
			}
			
			case format: {
				return "format";
			}
			
			case transparent: {
				return "transparent";
			}
			
			case bgcolor: {
				return "bgcolor";
			}
			
			case exceptions: {
				return "exceptions";
			}
			
			case time: {
				return "time";
			}
			
			case sld: {
				return "sld";
			}
			
			case sld_body: {
				return "sld_body";
			}
			
			case query_layers: {
				return "query_layers";
			}
			
			case info_format: {
				return "info_format";
			}
			
			case feature_count: {
				return "feature_count";
			}
			
			case featuretype: {
				return "featuretype";
			}
			
			case x: {
				return "x";
			}
			
			case i: {
				return "x";  // To support < WMS 1.3.0
			}
			
			case y: {
				return "y";
			}
			
			case j: {
				return "y";  // To support < WMS 1.3.0
			}
			
			case buffer: {
				return "buffer";
			}
			
			case cql_filter: {
				return "cql_filter";
			}
			
			case filter: {
				return "filter";
			}
			
			case propertyName: {
				return "propertyName";
			}
			
			case rule: {
				return "rule";
			}
			
			case scale: {
				return "scale";
			}
			
			case viewparams: {
				return "viewparams";
			}
			
			default: {
				return "UNKNOWN";
			}
		}
	}
	
	public static boolean isRequired(WMSParameters type) {
		switch (type) {
			case service: {
				return true;
			}
			
			case version: {
				return true;
			}
			
			case request: {
				return true;
			}
			
			case namespace: {
				return false;
			}
			
			case layer: {
				return true;
			}
			
			case layers: {
				return true;
			}
			
			case style: {
				return false;
			}
			
			case styles: {
				return true;
			}
			
			case srs: {
				return true;
			}
			
			case crs: {
				return false;
			}
			
			case bbox: {
				return true;
			}
			
			case width: {
				return true;
			}
			
			case height: {
				return true;
			}
			
			case format: {
				return true;
			}
			
			case transparent: {
				return false;
			}
			
			case bgcolor: {
				return false;
			}
			
			case exceptions: {
				return false;
			}
			
			case time: {
				return false;
			}
			
			case sld: {
				return false;
			}
			
			case sld_body: {
				return false;
			}
			
			case query_layers: {
				return true;
			}
			
			case info_format: {
				return false;
			}
			
			case feature_count: {
				return false;
			}
			
			case featuretype: {
				return false;
			}
			
			case x: {
				return true;
			}
			
			case i: {
				return false;
			}
			
			case y: {
				return true;
			}
			
			case j: {
				return false;
			}
			
			case buffer: {
				return false;
			}
			
			case cql_filter: {
				return false;
			}
			
			case filter: {
				return false;
			}
			
			case propertyName: {
				return false;
			}
			
			case rule: {
				return false;
			}
			
			case scale: {
				return false;
			}
			
			case viewparams: {
				return false;
			}
			
			default: {
				return false;
			}
		}
	}
}
