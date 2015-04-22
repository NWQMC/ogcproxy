package gov.usgs.wqp.ogcproxy.exceptions;

public class OGCProxyExceptionID {
	private final Long exceptionId;
    private String name;

    private OGCProxyExceptionID( String name, int id ) {
    	this.name = name;
    	this.exceptionId = Long.valueOf(id);
    }
    
    public String toString() { return this.name; }
    public Long value() { return this.exceptionId; }
    
    //-----------------------------------------
    // EXCEPTION DEFINITIONS
    //-----------------------------------------
    
    // BASIC EXCEPTIONS
    public static final OGCProxyExceptionID INITIALIZATION_NOT_PERFORMED =
        	new OGCProxyExceptionID("WMSProxyException Initialization Exception: " +
        			"Initialization of class not performed.", 0x00000);
    
    // CACHE EXCEPTIONS
    public static final OGCProxyExceptionID WMS_LAYER_CREATION_TIME_EXCEEDED =
    	new OGCProxyExceptionID("WMSProxyException Layer Creation Time Exception: ShapeFile creation " +
    			"time exceeded accepted limits.", 0x01000);
    
    public static final OGCProxyExceptionID WMS_LAYER_CREATION_FAILED =
        	new OGCProxyExceptionID("WMSProxyException Layer Creation Failed Exception: ShapeFile creation " +
        			"was unsuccessful.", 0x01001);

    // ------------------
    // PROPERTIES EXCEPTIONS
    // ------------------
    public static final OGCProxyExceptionID INVALID_PROPERTIES_FILE_REQUESTED =
    	new OGCProxyExceptionID("WMSProxyException Properties Exception: Invalid properties file requested."
    			, 0x02000);
    
    // ------------------
    // PROXY EXCEPTIONS
    // ------------------
    public static final OGCProxyExceptionID UNSUPPORTED_REQUEST_METHOD =
    	new OGCProxyExceptionID("WMSProxyException Proxy Exception: Unsupported http request method."
    			, 0x03000);
    public static final OGCProxyExceptionID ERROR_READING_CLIENT_REQUEST_BODY =
        	new OGCProxyExceptionID("WMSProxyException Proxy Exception: Error reading client request body."
        			, 0x03001);
    public static final OGCProxyExceptionID UNSUPPORTED_CONTENT_FOR_REQUEST_METHOD =
        	new OGCProxyExceptionID("WMSProxyException Proxy Exception: Content in request body unsupported for client request method."
        			, 0x03002);
    public static final OGCProxyExceptionID URL_PARSING_EXCEPTION =
        	new OGCProxyExceptionID("WMSProxyException Proxy Exception: Syntax error parsing server URL."
        			, 0x03003);
    public static final OGCProxyExceptionID CLIENT_PROTOCOL_ERROR =
        	new OGCProxyExceptionID("WMSProxyException Proxy Exception: Client protocol error."
        			, 0x03004);
    public static final OGCProxyExceptionID SERVER_REQUEST_IO_ERROR =
        	new OGCProxyExceptionID("WMSProxyException Proxy Exception: I/O error on server request."
        			, 0x03005);
    public static final OGCProxyExceptionID SERVER_RESPONSE_INPUT_STREAM_ERROR =
        	new OGCProxyExceptionID("WMSProxyException Proxy Exception: Error obtaining input stream for server response."
        			, 0x03006);
    public static final OGCProxyExceptionID CLIENT_RESPONSE_OUTPUT_STREAM_ERROR =
        	new OGCProxyExceptionID("WMSProxyException Proxy Exception: Error obtaining output stream for client response."
        			, 0x03007);
    public static final OGCProxyExceptionID SERVER_TO_CLIENT_RESPONSE_ERROR =
        	new OGCProxyExceptionID("WMSProxyException Proxy Exception: Error copying server response to client."
        			, 0x03008);
    
    // ------------------
    // UTIL EXCEPTIONS
    // ------------------
    public static final OGCProxyExceptionID UTIL_GZIP_COMPRESSION_ERROR =
        	new OGCProxyExceptionID("WMSProxyException Util Exception: Gzip compression error."
        			, 0x04000);
    public static final OGCProxyExceptionID GZIP_ERROR =
        	new OGCProxyExceptionID("WMSProxyException Proxy Exception: Error uncompressing server gzip content."
        			, 0x04001);
    public static final OGCProxyExceptionID GZIP_NOT_UTF8 =
        	new OGCProxyExceptionID("WMSProxyException Proxy Exception: Server gzip content is not UTF-8."
        			, 0x04002);
    
    // ------------------
    // WQPUTIL EXCEPTIONS
    // ------------------
    public static final OGCProxyExceptionID INVALID_SERVER_RESPONSE_ENCODING =
        	new OGCProxyExceptionID("WMSProxyException WQPUtil Exception: Invalid content encoding."
        			, 0x05000);
    
    public static final OGCProxyExceptionID INVALID_SERVER_RESPONSE_CODE =
        	new OGCProxyExceptionID("WMSProxyException WQPUtil Exception: Invalid response code."
        			, 0x05001);
    
    // ------------------
    // LAYERBUILDER EXCEPTIONS
    // ------------------
    public static final OGCProxyExceptionID DATAFILE_PARSING_ERROR =
        	new OGCProxyExceptionID("WMSProxyException LayerBuilder Exception: Unable to parse datafile for search params."
        			, 0x06000);
    public static final OGCProxyExceptionID SHAPEFILE_CREATION_ERROR =
        	new OGCProxyExceptionID("WMSProxyException LayerBuilder Exception: Unable to create shapefile for search params."
        			, 0x06001);
    public static final OGCProxyExceptionID GEOTOOLS_FEATUREBUILDER_ERROR =
        	new OGCProxyExceptionID("WMSProxyException LayerBuilder Exception: Unable to create GeoTools SimpleFeatureBuilder."
        			, 0x06001);
}
