package gov.usgs.wqp.ogcproxy.model.parameters;

/**
 * ProxyDataSourceParameter
 * @author prusso
 *<br /><br />
 *	This enumeration explicitly defines all known Proxy Datasource Type's which
 *	decides the source of all proxy data at the servlet level.
 */
public enum ProxyDataSourceParameter {
	wqp_sites, UNKNOWN;

	public static ProxyDataSourceParameter getTypeFromString(String string) {
		if (string.contains("wqp_sites")) {
			return wqp_sites;
		}

		return UNKNOWN;
	}

	public static String getStringFromType(ProxyDataSourceParameter type) {
		switch (type) {
			case wqp_sites: {
				return "wqp_sites";
			}
			
			default: {
				return "UNKNOWN";
			}
		}
	}
}
