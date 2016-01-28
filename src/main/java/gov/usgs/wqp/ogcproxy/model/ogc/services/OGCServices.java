package gov.usgs.wqp.ogcproxy.model.ogc.services;

import gov.usgs.wqp.ogcproxy.utils.StringUtils;

/**
 * OGCServices
 * @author prusso
 *<br /><br />
 *	This enumeration explicitly defines all known OGC Services currently
 *	provided by the OGCProxy
 */
public enum OGCServices {
	WMS, WFS, UNKNOWN;

	public static OGCServices getTypeFromString(String string) {
		return StringUtils.getTypeFromString(string, UNKNOWN);
	}
}
