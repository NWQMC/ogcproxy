package gov.usgs.wqp.ogcproxy.exceptions;

public class WMSProxyExceptionID {
	private final Long exceptionId;
    private String name;

    private WMSProxyExceptionID( String name, int id ) {
    	this.name = name;
    	this.exceptionId = Long.valueOf(id);
    }
    
    public String toString() { return this.name; }
    public Long value() { return this.exceptionId; }
    
    //-----------------------------------------
    // EXCEPTION DEFINITIONS
    //-----------------------------------------
    
    // BASIC EXCEPTIONS
    public static final WMSProxyExceptionID INITIALIZATION_NOT_PERFORMED = 
        	new WMSProxyExceptionID("WMSProxyException Initialization Exception: " +
        			"Initialization of class not performed.", 0x00000);
    
    // CACHE EXCEPTIONS
    public static final WMSProxyExceptionID WMS_LAYER_CREATION_TIME_EXCEEDED = 
    	new WMSProxyExceptionID("WMSProxyException Layer Creation Time Exception: ShapeFile creation " +
    			"time exceeded accepted limits.", 0x01000);
    
    public static final WMSProxyExceptionID WMS_LAYER_CREATION_FAILED = 
        	new WMSProxyExceptionID("WMSProxyException Layer Creation Failed Exception: ShapeFile creation " +
        			"was unsuccessful.", 0x01001);

    // ------------------
    // PROPERTIES EXCEPTIONS
    // ------------------
    public static final WMSProxyExceptionID INVALID_PROPERTIES_FILE_REQUESTED = 
    	new WMSProxyExceptionID("WMSProxyException Properties Exception: Invalid properties file requested."
    			, 0x02000);
    
    // ------------------
    // PROXY EXCEPTIONS
    // ------------------
    public static final WMSProxyExceptionID UNSUPPORTED_REQUEST_METHOD = 
    	new WMSProxyExceptionID("WMSProxyException Proxy Exception: Unsupported http request method."
    			, 0x03000);
    public static final WMSProxyExceptionID ERROR_READING_CLIENT_REQUEST_BODY = 
        	new WMSProxyExceptionID("WMSProxyException Proxy Exception: Error reading client request body."
        			, 0x03001);
    public static final WMSProxyExceptionID UNSUPPORTED_CONTENT_FOR_REQUEST_METHOD = 
        	new WMSProxyExceptionID("WMSProxyException Proxy Exception: Content in request body unsupported for client request method."
        			, 0x03002);
    public static final WMSProxyExceptionID URL_PARSING_EXCEPTION = 
        	new WMSProxyExceptionID("WMSProxyException Proxy Exception: Syntax error parsing server URL."
        			, 0x03003);
    public static final WMSProxyExceptionID CLIENT_PROTOCOL_ERROR = 
        	new WMSProxyExceptionID("WMSProxyException Proxy Exception: Client protocol error."
        			, 0x03004);
    public static final WMSProxyExceptionID SERVER_REQUEST_IO_ERROR = 
        	new WMSProxyExceptionID("WMSProxyException Proxy Exception: I/O error on server request."
        			, 0x03005);
    public static final WMSProxyExceptionID SERVER_RESPONSE_INPUT_STREAM_ERROR = 
        	new WMSProxyExceptionID("WMSProxyException Proxy Exception: Error obtaining input stream for server response."
        			, 0x03006);
    public static final WMSProxyExceptionID CLIENT_RESPONSE_OUTPUT_STREAM_ERROR = 
        	new WMSProxyExceptionID("WMSProxyException Proxy Exception: Error obtaining output stream for client response."
        			, 0x03007);
    public static final WMSProxyExceptionID SERVER_TO_CLIENT_RESPONSE_ERROR = 
        	new WMSProxyExceptionID("WMSProxyException Proxy Exception: Error copying server response to client."
        			, 0x03008);
    
    // ------------------
    // UTIL EXCEPTIONS
    // ------------------
    public static final WMSProxyExceptionID UTIL_GZIP_COMPRESSION_ERROR = 
        	new WMSProxyExceptionID("WMSProxyException Util Exception: Gzip compression error."
        			, 0x04000);
    public static final WMSProxyExceptionID GZIP_ERROR = 
        	new WMSProxyExceptionID("WMSProxyException Proxy Exception: Error uncompressing server gzip content."
        			, 0x04001);
    public static final WMSProxyExceptionID GZIP_NOT_UTF8 = 
        	new WMSProxyExceptionID("WMSProxyException Proxy Exception: Server gzip content is not UTF-8."
        			, 0x04002);
    
    // ------------------
    // WQPUTIL EXCEPTIONS
    // ------------------
    public static final WMSProxyExceptionID INVALID_SERVER_RESPONSE_ENCODING = 
        	new WMSProxyExceptionID("WMSProxyException WQPUtil Exception: Invalid content encoding."
        			, 0x05000);
    
    public static final WMSProxyExceptionID INVALID_SERVER_RESPONSE_CODE = 
        	new WMSProxyExceptionID("WMSProxyException WQPUtil Exception: Invalid response code."
        			, 0x05001);
    
    // ------------------
    // LAYERBUILDER EXCEPTIONS
    // ------------------
    public static final WMSProxyExceptionID DATAFILE_PARSING_ERROR = 
        	new WMSProxyExceptionID("WMSProxyException LayerBuilder Exception: Unable to parse datafile for search params."
        			, 0x06000);
    public static final WMSProxyExceptionID SHAPEFILE_CREATION_ERROR = 
        	new WMSProxyExceptionID("WMSProxyException LayerBuilder Exception: Unable to create shapefile for search params."
        			, 0x06001);
    public static final WMSProxyExceptionID GEOTOOLS_FEATUREBUILDER_ERROR = 
        	new WMSProxyExceptionID("WMSProxyException LayerBuilder Exception: Unable to create GeoTools SimpleFeatureBuilder."
        			, 0x06001);
}
