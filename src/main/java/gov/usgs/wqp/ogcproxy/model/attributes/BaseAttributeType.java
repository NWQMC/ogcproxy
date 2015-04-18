package gov.usgs.wqp.ogcproxy.model.attributes;

import gov.usgs.wqp.ogcproxy.utils.StringUtils;


/**
 * BaseAttributeType
 * @author prusso
 *<br /><br />
 *	This enumeration explicitly defines all known WQP Feature Attributes possible
 *	in a dataset used for creating a shapefile.
 */
public enum BaseAttributeType {
	Provider, LocationIdentifier, LocationType, Latitude, Longitude, UNKNOWN;

	public static BaseAttributeType getTypeFromString(String string) {
		return StringUtils.getTypeFromString(string, UNKNOWN);
	}

	public static String getStringFromType(BaseAttributeType type) {
		return StringUtils.getStringFromType(type, UNKNOWN);
	}
}
