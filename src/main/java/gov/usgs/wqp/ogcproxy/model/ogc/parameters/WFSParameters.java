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
	
	public static boolean isRequired(WFSParameters type) {
		switch (type) {
			case service:
			case version:
			case request:
			case typeNames:
				return true;
			default:
				return false;
		}
	}
}
