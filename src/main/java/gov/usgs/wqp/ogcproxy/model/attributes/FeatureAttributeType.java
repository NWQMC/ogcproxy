package gov.usgs.wqp.ogcproxy.model.attributes;

import gov.usgs.wqp.ogcproxy.utils.StringUtils;

/**
 * FeatureAttributeType
 * @author prusso
 *<br /><br />
 *	This enumeration explicitly defines all known Geo-Feature Attributes possible
 *	in a dataset used for creating a shapefile.
 *
 *	Note that the names are limited to 10 characters!!!
 */
public enum FeatureAttributeType {
	provider, orgName, orgId, locName, name, type, point, UNKNOWN;

	public static FeatureAttributeType getTypeFromString(String string) {
		return StringUtils.getTypeFromString(string, UNKNOWN);
	}

	public static String getStringFromType(FeatureAttributeType type) {
		return StringUtils.getSentenceCaseStringFromType(type, UNKNOWN);
	}
}
