package gov.usgs.wqp.ogcproxy.model.parameters;

/**
 * WQPParameters
 * @author prusso
 *<br /><br />
 *	This enumeration explicitly defines all known WQP parameters possible in 
 *	any WMSProxy call.
 */
public enum WQPParameters {
	searchParams, UNKNOWN;

	public static WQPParameters getTypeFromString(String string) {
		if (string.equals("searchParams")) {
			return searchParams;
		}

		return UNKNOWN;
	}

	public static String getStringFromType(WQPParameters type) {
		switch (type) {
			case searchParams: {
				return "searchParams";
			}
			
			default: {
				return "UNKNOWN";
			}
		}
	}
}
