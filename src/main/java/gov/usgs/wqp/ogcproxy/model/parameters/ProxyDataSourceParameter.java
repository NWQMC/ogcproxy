package gov.usgs.wqp.ogcproxy.model.parameters;

/**
 * ProxyDataSourceParameter
 * @author prusso
 *<br /><br />
 *	This enumeration explicitly defines all known Proxy Datasource Type's which
 *	decides the source of all proxy data at the servlet level.
 */
public enum ProxyDataSourceParameter {
	WQP_SITES, UNKNOWN;

	public static ProxyDataSourceParameter getTypeFromString(String string) {
		if (string != null && string.toLowerCase().contains("wqp_sites")) {
			return WQP_SITES;
		}
		return UNKNOWN;
	}

	public String toString() {
		if (WQP_SITES == this) {
			return "wqp_sites"; // lower case
		}
		return super.toString();
	}
}
