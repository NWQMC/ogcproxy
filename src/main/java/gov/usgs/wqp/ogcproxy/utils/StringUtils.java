package gov.usgs.wqp.ogcproxy.utils;


public class StringUtils {

	
	public static String sentenceCase(String str) {
		if (org.springframework.util.StringUtils.isEmpty(str)) {
			return str;
		}
		str = str.toString().toLowerCase();
		str = Character.toUpperCase(str.charAt(0)) + str.substring(1);
		return str;
	}


	@SuppressWarnings({ "unchecked", "static-access" })
	public static <T extends Enum<T>> T getTypeFromString(String string, T fallback) {
		T type = null;
		try {
			type = (T)fallback.valueOf(fallback.getClass(), string);
		} catch (Exception e) {
			// use default value
		}
		
		return type==null ?fallback :type;
	}
	

	public static <T extends Enum<T>> String getSentenceCaseStringFromType(Enum<T> type, Enum<T> fallback) {
		if (type==null || fallback.equals(type)) {
			return fallback.toString();
		}

		return StringUtils.sentenceCase( getStringFromType(type, fallback) );
	}
	
	
	public static <T extends Enum<T>> String getStringFromType(Enum<T> type, Enum<T> fallback) {
		if (type==null) {
			return fallback.toString();
		}
		return type.toString();
	}
	
}
