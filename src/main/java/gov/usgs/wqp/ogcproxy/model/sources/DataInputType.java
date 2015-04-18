package gov.usgs.wqp.ogcproxy.model.sources;

import gov.usgs.wqp.ogcproxy.utils.StringUtils;


/**
 * DataInputType
 * @author prusso
 *<br /><br />
 *	This enumeration explicitly defines all supported Data Formats 
 *	for a dataset used for creating a shapefile.
 */
public enum DataInputType {
	WQX_OB_XML, WQX_OB_FIS, UNKNOWN;

	public static DataInputType getTypeFromString(String string) {
		return StringUtils.getTypeFromString(string, UNKNOWN);
	}

	public static String getStringFromType(DataInputType type) {
		return StringUtils.getStringFromType(type, UNKNOWN);
	}
}
