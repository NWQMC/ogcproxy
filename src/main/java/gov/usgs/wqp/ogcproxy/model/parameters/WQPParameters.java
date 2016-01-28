package gov.usgs.wqp.ogcproxy.model.parameters;

import gov.usgs.wqp.ogcproxy.utils.StringUtils;


/**
 * WQPParameters
 * @author prusso
 *<br /><br />
 *	This enumeration explicitly defines all known WQP parameters possible in
 *	any OGCProxy call.
 */
public enum WQPParameters {
	searchParams, UNKNOWN;

	public static WQPParameters getTypeFromString(String string) {
		return StringUtils.getTypeFromString(string, UNKNOWN);
	}

	public static String getStringFromType(WQPParameters type) {
		return StringUtils.getStringFromType(type, UNKNOWN);
	}
}
