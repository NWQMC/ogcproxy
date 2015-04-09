package gov.usgs.wqp.ogcproxy.model.ogc.services;

/**
 * OGCServices
 * @author prusso
 *<br /><br />
 *	This enumeration explicitly defines all known OGC Services currently
 *	provided by the WMSProxy
 */
public enum OGCServices {
	WMS, WFS, UNKNOWN;

	public static OGCServices getTypeFromString(String string) {
		if ("WMS".equalsIgnoreCase(string)) {
			return WMS;
		}
		
		if ("WFS".equalsIgnoreCase(string)) {
			return WFS;
		}

		return UNKNOWN;
	}
}
