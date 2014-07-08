package gov.usgs.wqp.ogcproxy.model.parameters;

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
	typeNames,
	UNKNOWN;
	
	public static WFSParameters getTypeFromString(String string) {
		 /**
        * we need to lowercase the type so we can standardize on equality
        */
		string = string.toLowerCase();
		
		if (string.equals("typeNames")) {
			return typeNames;
		}
		
		return UNKNOWN;
	}

	public static String getStringFromType(WFSParameters type) {
		switch (type) {
			case typeNames: {
				return "typeNames";
			}
			
			default :{
				return "UNKNOWN";
			}
		}
	}
	
	public static boolean isRequired(WFSParameters type) {
		switch (type) {
			case typeNames: {
				return true;
			}
			
			default: {
				return false;
			}
		}
	}
}
