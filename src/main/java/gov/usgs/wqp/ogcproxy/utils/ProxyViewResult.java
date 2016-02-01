package gov.usgs.wqp.ogcproxy.utils;

import static org.springframework.util.StringUtils.*;

/**
 * ProxyServiceResult
 * @author prusso
 *<br /><br />
 *	This enumeration defines all status returns from a service in order to
 *	determine the location of the servlet response.
 */
public enum ProxyViewResult {
	EMPTY_JPG("whitepixel.jpg"),
	EMPTY_PNG("whitepixel.png"),
	EMPTY_TIFF("whitepixel.tiff"),
	EMPTY_PDF("whitepixel.pdf"),
	EMPTY_XML("whitepixel.xml"),
	ERROR_XML("error.xml");

	public final String view;
	private ProxyViewResult(String filename) {
		this.view = filename;
	}
	
	public static String getErrorViewByFormat(String format) {
		if (isEmpty(format)) {
			return ERROR_XML.view;
		}
		
		if ("image/png".equals(format.toLowerCase())) {
			return EMPTY_PNG.view;
		}
		
		if ("image/png8".equals(format.toLowerCase())) {
			return EMPTY_PNG.view;
		}
		
		if ("image/jpeg".equals(format.toLowerCase())) {
			return EMPTY_JPG.view;
		}
		
		if ("image/jpg".equals(format.toLowerCase())) {
			return EMPTY_JPG.view;
		}
		
		if ("image/tiff".equals(format.toLowerCase())) {
			return EMPTY_TIFF.view;
		}
		
		if ("image/tiff8".equals(format.toLowerCase())) {
			return EMPTY_TIFF.view;
		}
		
		if ("image/geotiff".equals(format.toLowerCase())) {
			return EMPTY_TIFF.view;
		}
		
		if ("image/geotiff8".equals(format.toLowerCase())) {
			return EMPTY_TIFF.view;
		}
		
		if ("application/pdf".equals(format.toLowerCase())) {
			return EMPTY_PDF.view;
		}
		
		return ERROR_XML.view;
	}
}