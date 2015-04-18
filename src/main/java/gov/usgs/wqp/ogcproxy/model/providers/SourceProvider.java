package gov.usgs.wqp.ogcproxy.model.providers;

import gov.usgs.wqp.ogcproxy.utils.StringUtils;


/**
 * SourceProvider
 * @author prusso
 *<br /><br />
 *	This enumeration explicitly defines all supported WQP Source Providers 
 *	possible in a dataset used for creating a shapefile.
 */
public enum SourceProvider {
	NWIS, STEWARDS, STORET, UNKNOWN;

	public static SourceProvider getTypeFromString(String string) {
		return StringUtils.getTypeFromString(string, UNKNOWN);
	}

	public static String getStringFromType(SourceProvider type) {
		return StringUtils.getStringFromType(type, UNKNOWN);
	}
}
