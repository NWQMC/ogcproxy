package gov.usgs.wqp.ogcproxy.model.parser.xml.ogc;

import static org.junit.Assert.*;

import java.util.Map;

import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;

public class OgcWfsParserTest {

	final String SEARCH_PARAMS = "countrycode:US;statecode:US%3A55%7CUS%3A28%7CUS%3A32;characteristicName:Atrazine";
	
	final String ANOTHER = "statecode:US%3A01;countycode:US%3A01%3A005%7CUS:01%3A007%7CUS%3A01%3A017;siteType:Stream%7CWell;sampleMedia:Water%7CSediment";
    
    
	final String ogcParamsWfs_typeNames = 
			" <?xml version=\"1.0\" encoding=\"UTF-8\"?> "+
			" <wfs:GetFeature xmlns:wfs=\"http://www.opengis.net/wfs\" service=\"WFS\" version=\"1.1.0\" "+
			"     xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "+
			"     xsi:schemaLocation=\"http://www.opengis.net/wfs http://schemas.opengis.net/wfs/1.1.0/wfs.xsd\" "+
			"     xmlns:ows=\"http://www.opengis.net/ows/1.1\"> "+
			"     <wfs:Query typeNames=\"wqp_sites\" srsName=\"EPSG:900913\"> "+
			"     </wfs:Query> "+
			" </wfs:GetFeature> ";
	
	final String ogcParamsWfs_typeName = 
			" <?xml version=\"1.0\" encoding=\"UTF-8\"?> "+
			" <wfs:GetFeature xmlns:wfs=\"http://www.opengis.net/wfs\" service=\"WFS\" version=\"1.1.0\" "+
			"     xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "+
			"     xsi:schemaLocation=\"http://www.opengis.net/wfs http://schemas.opengis.net/wfs/1.1.0/wfs.xsd\" "+
			"     xmlns:ows=\"http://www.opengis.net/ows/1.1\"> "+
			"     <wfs:Query typeName=\"wqp_sites\" srsName=\"EPSG:900913\"> "+
			"     </wfs:Query> "+
			" </wfs:GetFeature> ";

	final String ogcParamsWfs_noXMLversion = 
			" <wfs:GetFeature xmlns:wfs=\"http://www.opengis.net/wfs\" service=\"WFS\" version=\"1.1.0\" "+
			"     xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "+
			"     xsi:schemaLocation=\"http://www.opengis.net/wfs http://schemas.opengis.net/wfs/1.1.0/wfs.xsd\" "+
			"     xmlns:ows=\"http://www.opengis.net/ows/1.1\"> "+
			"     <wfs:Query typeName=\"wqp_sites\" srsName=\"EPSG:900913\"> "+
			"     </wfs:Query> "+
			" </wfs:GetFeature> ";

	
	final String searchParamOcgXml = 
			" <?xml version=\"1.0\" encoding=\"UTF-8\"?> "+
			" <wfs:GetFeature xmlns:wfs=\"http://www.opengis.net/wfs\" service=\"WFS\" version=\"1.1.0\" "+
			"     xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "+
			"     xsi:schemaLocation=\"http://www.opengis.net/wfs http://schemas.opengis.net/wfs/1.1.0/wfs.xsd\" "+
			"     xmlns:ows=\"http://www.opengis.net/ows/1.1\"> "+
			"     <wfs:Query typeName=\"wqp_sites\" srsName=\"EPSG:900913\"> "+
			"         <ogc:Filter xmlns:ogc=\"http://www.opengis.net/ogc\"> "+
			"             <ogc:And> "+
			"                 <ogc:BBOX> "+
			"                     <gml:Envelope xmlns:gml=\"http://www.opengis.net/gml\" srsName=\"EPSG:900913\"> "+
			"                         <gml:lowerCorner>-10534634.541594 5203332.5364988</gml:lowerCorner> "+
			"                         <gml:upperCorner>-9791055.1305685 5614258.0004863</gml:upperCorner> "+
			"                     </gml:Envelope> "+
			"                 </ogc:BBOX> "+
			"                 <ogc:PropertyIsEqualTo> "+
			"		            <ogc:PropertyName>searchParams</ogc:PropertyName> "+
			"		            <ogc:Literal>"+SEARCH_PARAMS+"</ogc:Literal> "+
			"                 </ogc:PropertyIsEqualTo> "+
			"             </ogc:And> "+
			"         </ogc:Filter> "+
			"     </wfs:Query> "+
			" </wfs:GetFeature> ";

	
	final String simpleOgcWfs = 
			" <?xml version=\"1.0\" encoding=\"UTF-8\"?> "+
			" <wfs:GetFeature xmlns:wfs=\"http://www.opengis.net/wfs\" service=\"WFS\" version=\"1.1.0\" "+
			"     xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "+
			"     xsi:schemaLocation=\"http://www.opengis.net/wfs http://schemas.opengis.net/wfs/1.1.0/wfs.xsd\" "+
			"     xmlns:ows=\"http://www.opengis.net/ows/1.1\"> "+
			"     <wfs:Query typeName=\"wqp_sites\" srsName=\"EPSG:900913\"> "+
			"         <ogc:Filter xmlns:ogc=\"http://www.opengis.net/ogc\"> "+
			"             <ogc:And> "+
			"                 <ogc:BBOX> "+
			"                     <gml:Envelope xmlns:gml=\"http://www.opengis.net/gml\" srsName=\"EPSG:900913\"> "+
			"                         <gml:lowerCorner>-10534634.541594 5203332.5364988</gml:lowerCorner> "+
			"                         <gml:upperCorner>-9791055.1305685 5614258.0004863</gml:upperCorner> "+
			"                     </gml:Envelope> "+
			"                 </ogc:BBOX> "+
			"                 <ogc:PropertyIsEqualTo> "+
			"                     <ogc:PropertyName>statecode</ogc:PropertyName> "+
			"                     <ogc:Literal>US:55;US:19</ogc:Literal> "+
			"                 </ogc:PropertyIsEqualTo> "+
			"                 <ogc:PropertyIsEqualTo> "+
			"                     <ogc:PropertyName>siteType</ogc:PropertyName> "+
			"                     <ogc:Literal>Stream</ogc:Literal> "+
			"                 </ogc:PropertyIsEqualTo> "+
			"             </ogc:And> "+
			"         </ogc:Filter> "+
			"     </wfs:Query> "+
			" </wfs:GetFeature> ";
	
		
		final String complexOgcWfs =
			" <?xml version=\"1.0\" encoding=\"UTF-8\"?> "+
			" <wfs:GetFeature xmlns:wfs=\"http://www.opengis.net/wfs\" service=\"WFS\" version=\"1.1.0\" "+
			"     xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "+
			"     xsi:schemaLocation=\"http://www.opengis.net/wfs http://schemas.opengis.net/wfs/1.1.0/wfs.xsd\" "+
			"     xmlns:ows=\"http://www.opengis.net/ows/1.1\"> "+
			"     <wfs:Query typeName=\"wqp_sites\" srsName=\"EPSG:900913\"> "+
			"         <ogc:Filter xmlns:ogc=\"http://www.opengis.net/ogc\"> "+
			"             <ogc:And> "+
			"                 <ogc:BBOX> "+
			"                     <gml:Envelope xmlns:gml=\"http://www.opengis.net/gml\" srsName=\"EPSG:900913\"> "+
			"                         <gml:lowerCorner>-10534634.541594 5203332.5364988</gml:lowerCorner> "+
			"                         <gml:upperCorner>-9791055.1305685 5614258.0004863</gml:upperCorner> "+
			"                     </gml:Envelope> "+
			"                 </ogc:BBOX> "+
			"                 <ogc:PropertyIsEqualTo> "+
			"                     <ogc:PropertyName>statecode</ogc:PropertyName> "+
			"                     <ogc:Literal>US:55;US:19</ogc:Literal> "+
			"                 </ogc:PropertyIsEqualTo> "+
			"                 <ogc:PropertyIsEqualTo> "+
			"                     <ogc:PropertyName>siteType</ogc:PropertyName> "+
			"                     <ogc:Literal>Stream</ogc:Literal> "+
			"                 </ogc:PropertyIsEqualTo> "+
			"                 <ogc:PropertyIsEqualTo> "+
			"                     <ogc:PropertyName>organization</ogc:PropertyName> "+
			"                     <ogc:Literal>21IOWA_WQX;21IOWA;USGS-IA</ogc:Literal> "+
			"                 </ogc:PropertyIsEqualTo> "+
			"                 <ogc:PropertyIsEqualTo> "+
			"                     <ogc:PropertyName>sampleMedia</ogc:PropertyName> "+
			"                     <ogc:Literal>Water;Sediment</ogc:Literal> "+
			"                 </ogc:PropertyIsEqualTo> "+
			"                 <ogc:PropertyIsEqualTo> "+
			"                     <ogc:PropertyName>characteristicType</ogc:PropertyName> "+
			"                     <ogc:Literal>Nutrient</ogc:Literal> "+
			"                 </ogc:PropertyIsEqualTo> "+
			"             </ogc:And> "+
			"         </ogc:Filter> "+
			"     </wfs:Query> "+
			" </wfs:GetFeature> ";
	
	
	@Test
	public void testOgcParse_typeNames() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("POST");
		request.setContentType("application/xml");
		
		request.setContent(ogcParamsWfs_typeNames.getBytes());
		
		Map<String,String> ogcParams = new OgcWfsParser(request).ogcParse();
		
		System.out.println(ogcParams);
		
		assertEquals(3,ogcParams.size());
		assertEquals("WFS", ogcParams.get("service"));
		assertEquals("1.1.0", ogcParams.get("version"));
		assertEquals("wqp_sites", ogcParams.get("typeNames"));
		assertEquals(null, ogcParams.get("typeName"));
	}
	
	@Test
	public void testOgcParse_typeName() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("POST");
		request.setContentType("application/xml");
		
		request.setContent(ogcParamsWfs_typeName.getBytes());
		
		Map<String,String> ogcParams = new OgcWfsParser(request).ogcParse();
		
		System.out.println(ogcParams);
		
		assertEquals(3,ogcParams.size());
		assertEquals("WFS", ogcParams.get("service"));
		assertEquals("1.1.0", ogcParams.get("version"));
		assertEquals("wqp_sites", ogcParams.get("typeName"));
		assertEquals(null, ogcParams.get("typeNames"));
	}
	
	@Test
	public void testOgcParse_noXMLversion() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("POST");
		request.setContentType("application/xml");
		
		request.setContent(ogcParamsWfs_noXMLversion.getBytes());
		
		Map<String,String> ogcParams = new OgcWfsParser(request).ogcParse();
		
		System.out.println(ogcParams);
		
		assertEquals(3,ogcParams.size());
		assertEquals("WFS", ogcParams.get("service"));
		assertEquals("1.1.0", ogcParams.get("version"));
		assertEquals("wqp_sites", ogcParams.get("typeName"));
		assertEquals(null, ogcParams.get("typeNames"));
	}
	
	@Test
	public void testOgcParse_simpleOgcWfs() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("POST");
		request.setContentType("application/xml");
		
		request.setContent(simpleOgcWfs.getBytes());
		
		Map<String,String> ogcParams = new OgcWfsParser(request).ogcParse();
		
		System.out.println(ogcParams);
		
		assertEquals(3,ogcParams.size());
		assertEquals("WFS", ogcParams.get("service"));
		assertEquals("1.1.0", ogcParams.get("version"));
		assertEquals("wqp_sites", ogcParams.get("typeName"));
		assertEquals(null, ogcParams.get("typeNames"));
	}
	
	@Test
	public void testOgcParse_complexOgcWfs() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("POST");
		request.setContentType("application/xml");
		
		request.setContent(complexOgcWfs.getBytes());
		
		Map<String,String> ogcParams = new OgcWfsParser(request).ogcParse();
		
		System.out.println(ogcParams);
		
		assertEquals(3,ogcParams.size());
		assertEquals("WFS", ogcParams.get("service"));
		assertEquals("1.1.0", ogcParams.get("version"));
		assertEquals("wqp_sites", ogcParams.get("typeName"));
		assertEquals(null, ogcParams.get("typeNames"));
	}
	
	

	@Test
	public void testGetBody_searchParamOcgXml() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("POST");
		request.setContentType("application/xml");
		
		request.setContent(searchParamOcgXml.getBytes());
		
		String content = new OgcWfsParser(request).getBody();
		
		System.out.println(content);
		
		String expected = searchParamOcgXml.substring(41).trim();
		
		assertEquals(expected, content);
	}
	
	
	@Test
	public void testSearchParse_searchParamOcgXml() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("POST");
		request.setContentType("application/xml");
		
		request.setContent(searchParamOcgXml.getBytes());
		
		String searchParams = new OgcWfsParser(request).searchParse();
		
		System.out.println();
		System.out.println(searchParams);
		System.out.println();
		
		assertEquals(SEARCH_PARAMS, searchParams);
	}

	@Test
	public void testPayloadToMap_searchParamOcgXml() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("POST");
		request.setContentType("application/xml");
		
		request.setContent(searchParamOcgXml.getBytes());
		
		Map<String,String> requestParams = new OgcWfsParser(request).requestParamsPayloadToMap();
		
		System.out.println();
		System.out.println(requestParams);
		System.out.println();
		
		assertEquals("US", requestParams.get("countrycode"));
		assertEquals("US:55;US:28;US:32", requestParams.get("statecode"));
		assertEquals("Atrazine", requestParams.get("characteristicName"));
	}

}
