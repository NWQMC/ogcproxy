package gov.usgs.wqp.ogcproxy.cli;

public class ShapeFileUtility {
	public enum CLIMode {
		createAndUploadShapefile, createShapefile, uploadShapefile, geoserverPost, geoserverPostFile, geoserverGet, geoserverDelete, UNKNOWN;

		public static CLIMode getTypeFromString(String string) {
			if (string.equals("createAndUploadShapefile")) {
				return createAndUploadShapefile;
			}
			
			if (string.equals("createShapefile")) {
				return createShapefile;
			}
			
			if (string.equals("uploadShapefile")) {
				return uploadShapefile;
			}
			
			if (string.equals("geoserverPost")) {
				return geoserverPost;
			}
			
			if (string.equals("geoserverPostFile")) {
				return geoserverPostFile;
			}
			
			if (string.equals("geoserverGet")) {
				return geoserverGet;
			}
			
			if (string.equals("geoserverDelete")) {
				return geoserverDelete;
			}
			
			if (string.equals("UNKNOWN")) {
				return UNKNOWN;
			}

			return UNKNOWN;
		}

		public static String getStringFromType(CLIMode type) {
			switch (type) {
				case createAndUploadShapefile: {
					return "createAndUploadShapefile";
				}
				
				case createShapefile: {
					return "createShapefile";
				}
				
				case uploadShapefile: {
					return "uploadShapefile";
				}
				
				case geoserverPost: {
					return "geoserverPost";
				}
				
				case geoserverPostFile: {
					return "geoserverPostFile";
				}
				
				case geoserverGet: {
					return "geoserverGet";
				}
				
				case geoserverDelete: {
					return "geoserverDelete";
				}
				
				case UNKNOWN: {
					return "UNKNOWN";
				}
				
				default: {
					return "UNKNOWN";
				}
			}
		}
		
		public static String validModes() {
			CLIMode[] values = CLIMode.values();
			
			StringBuffer result = new StringBuffer();
			
			for(int i = 0; i < values.length; i++) {
				CLIMode mode = values[i];
				if(CLIMode.UNKNOWN == mode) {
					continue;
				}
				
				result.append(mode);
				
				if(i < (values.length - 1)) {
					if(values[i+1] != CLIMode.UNKNOWN) {
						result.append(", ");
					}
				}
			}
			
			return result.toString();
		}
	}

}
