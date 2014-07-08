package gov.usgs.wqp.ogcproxy.model.services;

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
		if (string.equals("WMS")) {
			return WMS;
		}
		
		if (string.equals("WFS")) {
			return WFS;
		}

		return UNKNOWN;
	}

	public static String getStringFromType(OGCServices type) {
		switch (type) {
			case WMS: {
				return "WMS";
			}
			
			case WFS: {
				return "WFS";
			}
			
			default: {
				return "UNKNOWN";
			}
		}
	}
}
