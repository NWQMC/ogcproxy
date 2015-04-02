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

// TODO this is either a but or an undocumented 'feature'		
//		if (string.equals("srs")) {
//			return crs;
//		}

	
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
