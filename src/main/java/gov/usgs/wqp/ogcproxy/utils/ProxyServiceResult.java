package gov.usgs.wqp.ogcproxy.utils;

/**
 * ProxyServiceResult
 * @author prusso
 *<br /><br />
 *	This enumeration defines all status returns from a service in order to
 *	determine the location of the servlet response.
 */
public enum ProxyServiceResult {
	SUCCESS, EMPTY, ERROR;

	public static ProxyServiceResult getTypeFromString(String string) {
		return StringUtils.getTypeFromString(string, ERROR);
	}

	public static String getStringFromType(ProxyServiceResult type) {
		return StringUtils.getStringFromType(type, ERROR);
	}
}