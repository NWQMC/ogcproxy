package gov.usgs.wqp.ogcproxy.model.providers;

/**
 * SourceProvider
 * @author prusso
 *<br /><br />
 *	This enumeration explicitly defines all supported WQP Source Providers 
 *	possible in a dataset used for creating a shapefile.
 */
public enum SourceProvider {
	NWIS, STEWARDS, STORET, UNKNOWN;

	public static SourceProvider getTypeFromString(String string) {
		if (string.equals("NWIS")) {
			return NWIS;
		}
		
		if (string.equals("STEWARDS")) {
			return STEWARDS;
		}
		
		if (string.equals("STORET")) {
			return STORET;
		}
		
		return UNKNOWN;
	}

	public static String getStringFromType(SourceProvider type) {
		switch (type) {
			case NWIS: {
				return "NWIS";
			}
			
			case STEWARDS: {
				return "STEWARDS";
			}
			
			case STORET: {
				return "STORET";
			}
			
			default: {
				return "UNKNOWN";
			}
		}
	}
}
