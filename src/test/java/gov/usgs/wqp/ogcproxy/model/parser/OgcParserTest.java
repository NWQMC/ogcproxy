package gov.usgs.wqp.ogcproxy.model.parser;

import static org.junit.Assert.assertEquals;

import java.util.Map;

import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import gov.usgs.wqp.ogcproxy.model.parser.OgcParser;

public class OgcParserTest {

	static final String SEARCH_PARAMS = "countrycode:US;statecode:US%3A55%7CUS%3A28%7CUS%3A32;characteristicName:Atrazine";

	static final String SEARCH_PARAMS_DECODED = "countrycode:US;statecode:US:55|US:28|US:32;characteristicName:Atrazine";

	static final String XML_DECLARATION = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>";

	static final String WFS_TAG = "<wfs:GetFeature xmlns:wfs=\"http://www.opengis.net/wfs\""+
			" xmlns:ows=\"http://www.opengis.net/ows/1.1\""+
			" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""+
			" service=\"WFS\" version=\"1.1.0\""+
			" xsi:schemaLocation=\"http://www.opengis.net/wfs http://schemas.opengis.net/wfs/1.1.0/wfs.xsd\">";

	final String wfs_typeNames =
			"<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?> "+
			"<wfs:GetFeature xmlns:wfs=\"http://www.opengis.net/wfs\" service=\"WFS\" version=\"1.1.0\" "+
			"	xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "+
			"	xsi:schemaLocation=\"http://www.opengis.net/wfs http://schemas.opengis.net/wfs/1.1.0/wfs.xsd\" "+
			"	xmlns:ows=\"http://www.opengis.net/ows/1.1\"> "+
			"	<wfs:Query typeNames=\"wqp_sites\" srsName=\"EPSG:900913\"> "+
			"	</wfs:Query> "+
			" </wfs:GetFeature> ";

	final String wfs_typeName =
			"<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?> "+
			"<wfs:GetFeature xmlns:wfs=\"http://www.opengis.net/wfs\" service=\"WFS\" version=\"1.1.0\" "+
			"	xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "+
			"	xsi:schemaLocation=\"http://www.opengis.net/wfs http://schemas.opengis.net/wfs/1.1.0/wfs.xsd\" "+
			"	xmlns:ows=\"http://www.opengis.net/ows/1.1\"> "+
			"	<wfs:Query typeName=\"wqp_sites\" srsName=\"EPSG:900913\"> "+
			"	</wfs:Query> "+
			"</wfs:GetFeature> ";

	public static final String wfs_vendorParams =
			XML_DECLARATION +
			WFS_TAG +
			"	<wfs:Query srsName=\"EPSG:900913\" typeName=\"wqp_sites\">"+
			"		<ogc:Filter xmlns:ogc=\"http://www.opengis.net/ogc\">"+
			"			<ogc:And>"+
			"				<ogc:BBOX>"+
			"					<gml:Envelope xmlns:gml=\"http://www.opengis.net/gml\" srsName=\"EPSG:900913\">"+
			"						<gml:lowerCorner>-10534634.541594 5203332.5364988</gml:lowerCorner>"+
			"						<gml:upperCorner>-9791055.1305685 5614258.0004863</gml:upperCorner>"+
			"					</gml:Envelope>"+
			"				</ogc:BBOX>"+
			"				<ogc:PropertyIsEqualTo>"+
			"				  <ogc:PropertyName>searchParams</ogc:PropertyName>"+
			"				  <ogc:Literal>"+SEARCH_PARAMS+"</ogc:Literal>"+
			"				</ogc:PropertyIsEqualTo>"+
			"			</ogc:And>"+
			"		</ogc:Filter>"+
			"	</wfs:Query>"+
			"</wfs:GetFeature>";

	public static final String wfs_minus_vendorParams =
			XML_DECLARATION +
			WFS_TAG +
			"	<wfs:Query srsName=\"EPSG:900913\" typeName=\"wqp_sites\">"+
			"		<ogc:Filter xmlns:ogc=\"http://www.opengis.net/ogc\">"+
			"			<ogc:And>"+
			"				<ogc:BBOX>"+
			"					<gml:Envelope xmlns:gml=\"http://www.opengis.net/gml\" srsName=\"EPSG:900913\">"+
			"						<gml:lowerCorner>-10534634.541594 5203332.5364988</gml:lowerCorner>"+
			"						<gml:upperCorner>-9791055.1305685 5614258.0004863</gml:upperCorner>"+
			"					</gml:Envelope>"+
			"				</ogc:BBOX>"+
			"				" +
			"			</ogc:And>"+
			"		</ogc:Filter>"+
			"	</wfs:Query>"+
			"</wfs:GetFeature>";

	public static final String WMS_GET_MAP_VENDOR_PARAMS = 
			XML_DECLARATION + 
			"<ogc:GetMap xmlns:ogc=\"http://www.opengis.net/ows\" xmlns:gml=\"http://www.opengis.net/gml\" service=\"WMS\" version=\"1.1.1\">" +
			"	<StyledLayerDescriptor version=\"1.0.0\">" +
			"		<NamedLayer>" +
			"			<Name>wqp_sites</Name>" +
			"			<NamedStyle><Name>wqp_sources</Name></NamedStyle>" + 
			"		</NamedLayer>" +
			"	</StyledLayerDescriptor>" +
			"	<BoundingBox srsName=\"http://www.opengis.net/gml/srs/epsg.xml#4326\">"+ 
			"		<gml:coord><gml:X>-130</gml:X><gml:Y>24</gml:Y></gml:coord>" +
			"		<gml:coord><gml:X>-55</gml:X><gml:Y>50</gml:Y></gml:coord>" +
			"	</BoundingBox>" +
			"	<Output>" +
			"		<Format>image/png</Format>" +
			"		<Size><Width>550</Width><Height>250</Height></Size>" +
			"	</Output>" +
			"	<ogc:Vendor>" +
			"		<ogc:PropertyIsEqualTo>" +
			"			<ogc:PropertyName>searchParams</ogc:PropertyName>" +
			"			<ogc:Literal>" + SEARCH_PARAMS + "</ogc:Literal>" +
			"		</ogc:PropertyIsEqualTo>" +
			"	</ogc:Vendor>" +
			"</ogc:GetMap>";

	public static final String WMS_GET_MAP_MINUS_VENDOR_PARAMS = 
			XML_DECLARATION + 
			"<ogc:GetMap xmlns:ogc=\"http://www.opengis.net/ows\" xmlns:gml=\"http://www.opengis.net/gml\" service=\"WMS\" version=\"1.1.1\">" +
			"	<StyledLayerDescriptor version=\"1.0.0\">" +
			"		<NamedLayer>" +
			"			<Name>wqp_sites</Name>" +
			"			<NamedStyle><Name>wqp_sources</Name></NamedStyle>" + 
			"		</NamedLayer>" +
			"	</StyledLayerDescriptor>" +
			"	<BoundingBox srsName=\"http://www.opengis.net/gml/srs/epsg.xml#4326\">"+ 
			"		<gml:coord><gml:X>-130</gml:X><gml:Y>24</gml:Y></gml:coord>" +
			"		<gml:coord><gml:X>-55</gml:X><gml:Y>50</gml:Y></gml:coord>" +
			"	</BoundingBox>" +
			"	<Output>" +
			"		<Format>image/png</Format>" +
			"		<Size><Width>550</Width><Height>250</Height></Size>" +
			"	</Output>" +
			"	" +
			"</ogc:GetMap>";

	@Test
	public void parse_wfs_typeNames_test() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("POST");
		request.setContentType("application/xml");
		request.setContent(wfs_typeNames.getBytes());

		OgcParser parser = new OgcParser(request);
		parser.parse();
		Map<String,String[]> requestParams = parser.getRequestParamsAsMap();

		assertEquals(5, requestParams.size());
		assertEquals("GetFeature", requestParams.get("request")[0]);
		assertEquals("1.1.0", requestParams.get("version")[0]);
		assertEquals("WFS", requestParams.get("service")[0]);
		assertEquals("wqp_sites", requestParams.get("typeNames")[0]);
		assertEquals("", requestParams.get("typeName")[0]);
	}

	@Test
	public void parse_wfs_typeName_test() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("POST");
		request.setContentType("application/xml");
		request.setContent(wfs_typeName.getBytes());

		OgcParser parser = new OgcParser(request);
		parser.parse();
		Map<String,String[]> requestParams = parser.getRequestParamsAsMap();

		assertEquals(5, requestParams.size());
		assertEquals("GetFeature", requestParams.get("request")[0]);
		assertEquals("1.1.0", requestParams.get("version")[0]);
		assertEquals("WFS", requestParams.get("service")[0]);
		assertEquals("wqp_sites", requestParams.get("typeName")[0]);
		assertEquals("", requestParams.get("typeNames")[0]);
	}

	@Test
	public void parse_wfs_vendor_params_test() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("POST");
		request.setContentType("application/xml");
		request.setContent(wfs_vendorParams.getBytes());

		OgcParser parser = new OgcParser(request);
		parser.parse();
		Map<String,String[]> requestParams = parser.getRequestParamsAsMap();

		assertEquals(6, requestParams.size());
		assertEquals("GetFeature", requestParams.get("request")[0]);
		assertEquals("1.1.0", requestParams.get("version")[0]);
		assertEquals("WFS", requestParams.get("service")[0]);
		assertEquals("wqp_sites", requestParams.get("typeName")[0]);
		assertEquals("", requestParams.get("typeNames")[0]);
		assertEquals(SEARCH_PARAMS_DECODED, requestParams.get("searchParams")[0]);

		assertEquals(wfs_minus_vendorParams, parser.getBodyMinusVendorParams());
	}

	@Test
	public void parse_wms_get_map_vendor_params_test() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("POST");
		request.setContentType("application/xml");
		request.setContent(WMS_GET_MAP_VENDOR_PARAMS.getBytes());

		OgcParser parser = new OgcParser(request);
		parser.parse();
		Map<String,String[]> requestParams = parser.getRequestParamsAsMap();

		assertEquals(5, requestParams.size());
		assertEquals("GetMap", requestParams.get("request")[0]);
		assertEquals("1.1.1", requestParams.get("version")[0]);
		assertEquals("WMS", requestParams.get("service")[0]);
		assertEquals("wqp_sites", requestParams.get("layers")[0]);
		assertEquals(SEARCH_PARAMS_DECODED, requestParams.get("searchParams")[0]);

		assertEquals(WMS_GET_MAP_MINUS_VENDOR_PARAMS, parser.getBodyMinusVendorParams());
	}

}
