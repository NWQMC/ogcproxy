package gov.usgs.wqp.ogcproxy.model.status;

import gov.usgs.wqp.ogcproxy.utils.StringUtils;

/**
 * DynamicLayerStatus
 * @author prusso
 *<br /><br />
 *	This enumeration explicitly defines all known states related to building
 *	a layer.
 */
public enum DynamicLayerStatus {
	AVAILABLE, BUILDING, INITIATED, EMPTY, ERROR;

	public static DynamicLayerStatus getTypeFromString(String string) {
		return StringUtils.getTypeFromString(string, ERROR);
	}

	public static String getStringFromType(DynamicLayerStatus type) {
		return StringUtils.getStringFromType(type, ERROR);
	}
}
