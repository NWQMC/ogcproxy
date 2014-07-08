package gov.usgs.wqp.ogcproxy.model.status;

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
		if (string.equals("AVAILABLE")) {
			return AVAILABLE;
		}
		
		if (string.equals("BUILDING")) {
			return BUILDING;
		}
		
		if (string.equals("INITIATED")) {
			return INITIATED;
		}
		
		if (string.equals("EMPTY")) {
			return EMPTY;
		}
		
		if (string.equals("ERROR")) {
			return ERROR;
		}

		return ERROR;
	}

	public static String getStringFromType(DynamicLayerStatus type) {
		switch (type) {
			case AVAILABLE: {
				return "AVAILABLE";
			}
			
			case BUILDING: {
				return "BUILDING";
			}
			
			case INITIATED: {
				return "INITIATED";
			}
			
			case EMPTY: {
				return "EMPTY";
			}
			
			case ERROR: {
				return "ERROR";
			}
			
			default: {
				return "ERROR";
			}
		}
	}
}
