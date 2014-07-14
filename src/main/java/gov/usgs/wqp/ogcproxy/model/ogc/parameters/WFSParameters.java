package gov.usgs.wqp.ogcproxy.model.ogc.parameters;

/**
 * WFSParameters
 * @author prusso
 *<br /><br />
 *	This enumeration was created in order to utilize the WFS request parameters
 *	defined in the GeoServer WFS reference.
 *<br /><br />
 *	This enumeration is only partly implemented in order to create a "quick" fix
 *	to getting dynamic layer creation working for all wfs? calls.
 */
public enum WFSParameters {
	/**
	 * http://docs.geoserver.org/stable/en/user/services/wfs/reference.html
	 */
	service,
	version,
	request,
	typeName,			// WFS 1.1.0 and earlier
	typeNames,
	exceptions,
	outputFormat,
	featureID,
	maxFeatures,
	count, 				// WFS 2.0.0
	sortBy,
	propertyName,
	srsName,
	bbox,
	valueReference,		// WFS 2.0.0
	expiry,				// WFS 2.0.0
	storedQuery_Id,		// WFS 2.0.0
	UNKNOWN;
	
	public static WFSParameters getTypeFromString(String string) {
		if (string.equals("service")) {
			return service;
		}
		
		if (string.equals("version")) {
			return version;
		}
		
		if (string.equals("request")) {
			return request;
		}
		
		if (string.equals("typeName")) {
			return typeName;
		}
		
		if (string.equals("typeNames")) {
			return typeNames;
		}
		
		if (string.equals("exceptions")) {
			return exceptions;
		}
		
		if (string.equals("outputFormat")) {
			return outputFormat;
		}
		
		if (string.equals("featureID")) {
			return featureID;
		}
		
		if (string.equals("maxFeatures")) {
			return maxFeatures;
		}
		
		if (string.equals("count")) {
			return count;
		}
		
		if (string.equals("sortBy")) {
			return sortBy;
		}
		
		if (string.equals("propertyName")) {
			return propertyName;
		}
		
		if (string.equals("srsName")) {
			return srsName;
		}
		
		if (string.equals("bbox")) {
			return bbox;
		}
		
		if (string.equals("valueReference")) {
			return valueReference;
		}
		
		if (string.equals("expiry")) {
			return expiry;
		}
		
		if (string.equals("storedQuery_Id")) {
			return storedQuery_Id;
		}
		
		return UNKNOWN;
	}

	public static String getStringFromType(WFSParameters type) {
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
		
			case typeName: {
				return "typeName";
			}
		
			case typeNames: {
				return "typeNames";
			}
		
			case exceptions: {
				return "exceptions";
			}
		
			case outputFormat: {
				return "outputFormat";
			}
		
			case featureID: {
				return "featureID";
			}
		
			case maxFeatures: {
				return "maxFeatures";
			}
		
			case count: {
				return "count";
			}
		
			case sortBy: {
				return "sortBy";
			}
		
			case propertyName: {
				return "propertyName";
			}
		
			case srsName: {
				return "srsName";
			}
		
			case bbox: {
				return "bbox";
			}
		
			case valueReference: {
				return "valueReference";
			}
		
			case expiry: {
				return "expiry";
			}
		
			case storedQuery_Id: {
				return "storedQuery_Id";
			}
			
			default :{
				return "UNKNOWN";
			}
		}
	}
	
	public static boolean isRequired(WFSParameters type) {
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
		
			case typeName: {
				return false;
			}
		
			case typeNames: {
				return true;
			}
		
			case exceptions: {
				return false;
			}
		
			case outputFormat: {
				return false;
			}
		
			case featureID: {
				return false;
			}
		
			case maxFeatures: {
				return false;
			}
		
			case count: {
				return false;
			}
		
			case sortBy: {
				return false;
			}
		
			case propertyName: {
				return false;
			}
		
			case srsName: {
				return false;
			}
		
			case bbox: {
				return false;
			}
		
			case valueReference: {
				return false;
			}
		
			case expiry: {
				return false;
			}
		
			case storedQuery_Id: {
				return false;
			}
			
			default: {
				return false;
			}
		}
	}
}
