package gov.usgs.wqp.ogcproxy.cli;

import gov.usgs.wqp.ogcproxy.utils.StringUtils;

public class ShapeFileUtility {
	public enum CLIMode {
		createAndUploadShapefile, createShapefile, uploadShapefile, geoserverPost, geoserverPostFile, geoserverGet, geoserverDelete, UNKNOWN;

		public static CLIMode getTypeFromString(String string) {
			return StringUtils.getTypeFromString(string, UNKNOWN);
		}

		public static String getStringFromType(CLIMode type) {
			return StringUtils.getStringFromType(type, UNKNOWN);
		}
		
		public static String validModes() {
			CLIMode[] values = CLIMode.values();
			
			StringBuffer result = new StringBuffer();
			
			for (int i = 0; i < values.length; i++) {
				CLIMode mode = values[i];
				if (CLIMode.UNKNOWN == mode) {
					continue;
				}
				
				result.append(mode);
				
				if (i < (values.length - 1)) {
					if (values[i+1] != CLIMode.UNKNOWN) {
						result.append(", ");
					}
				}
			}
			
			return result.toString();
		}
	}

}
