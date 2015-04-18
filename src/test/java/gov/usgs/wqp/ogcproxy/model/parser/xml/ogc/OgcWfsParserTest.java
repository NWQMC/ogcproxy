package gov.usgs.wqp.ogcproxy.model.parser.xml.ogc;

import static org.junit.Assert.*;
import gov.usgs.wqp.ogcproxy.model.parameters.SearchParameters;
import gov.usgs.wqp.ogcproxy.utils.ProxyUtil;

import java.util.HashMap;
import java.util.List;
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

	final String noSearchParamOcgXml =
			" <?xml version=\"1.0\" encoding=\"UTF-8\"?> "+
			" <wfs:GetFeature xmlns:wfs=\"http://www.opengis.net/wfs\" service=\"WFS\" version=\"1.1.0\" "+
			"     xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "+
			"     xsi:schemaLocation=\"http://www.opengis.net/wfs http://schemas.opengis.net/wfs/1.1.0/wfs.xsd\" "+
			"     xmlns:ows=\"http://www.opengis.net/ows/1.1\"> "+
			"     <wfs:Query typeName=\"wqp_sites\" srsName=\"EPSG:900913\"> "+
			"         <ogc:Filter xmlns:ogc=\"http://www.opengis.net/ogc\"> "+
			"                 <ogc:BBOX> "+
			"                     <gml:Envelope xmlns:gml=\"http://www.opengis.net/gml\" srsName=\"EPSG:900913\"> "+
			"                         <gml:lowerCorner>-10534634.541594 5203332.5364988</gml:lowerCorner> "+
			"                         <gml:upperCorner>-9791055.1305685 5614258.0004863</gml:upperCorner> "+
			"                     </gml:Envelope> "+
			"                 </ogc:BBOX> "+
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
		
		String searchParams = new OgcWfsParser(request).searchParams();
		
		System.out.println();
		System.out.println(searchParams);
		System.out.println();
		
		assertEquals(SEARCH_PARAMS.replaceAll("%3A", ":").replaceAll("%7C", "|"), searchParams);
	}

	@Test
	public void testSearchParse_bodyMinusSearchParams() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("POST");
		request.setContentType("application/xml");
		
		request.setContent(searchParamOcgXml.getBytes());
		
		OgcWfsParser parser = new OgcWfsParser(request);
		String searchParams = parser.searchParams();
		String bodyMinusSearchParams = parser.getBodyMinusSearchParams().replaceAll("\\s+"," ");
		String body = parser.getBody().replaceAll("\\s+", " ");
		
		System.out.println();
		System.out.println(searchParams);
		System.out.println(body);
		System.out.println(bodyMinusSearchParams);
		System.out.println();
		
		assertTrue("we expect the original to be longer than when searchParams are removed", body.length() > bodyMinusSearchParams.length() );
		assertFalse("body and body minus searchParams should not be the same",  body.equals(bodyMinusSearchParams) );
		
		// we are expecting a transformation like this
		//<wfs:GetFeature xmlns:wfs="http://www.opengis.net/wfs" service="WFS" version="1.1.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://www.opengis.net/wfs http://schemas.opengis.net/wfs/1.1.0/wfs.xsd" xmlns:ows="http://www.opengis.net/ows/1.1"> <wfs:Query typeName="wqp_sites" srsName="EPSG:900913"> <ogc:Filter xmlns:ogc="http://www.opengis.net/ogc"> <ogc:And> <ogc:BBOX> <gml:Envelope xmlns:gml="http://www.opengis.net/gml" srsName="EPSG:900913"> <gml:lowerCorner>-10534634.541594 5203332.5364988</gml:lowerCorner> <gml:upperCorner>-9791055.1305685 5614258.0004863</gml:upperCorner> </gml:Envelope> </ogc:BBOX> <ogc:PropertyIsEqualTo> 		 <ogc:PropertyName>searchParams</ogc:PropertyName> 		 <ogc:Literal>countrycode:US;statecode:US%3A55%7CUS%3A28%7CUS%3A32;characteristicName:Atrazine</ogc:Literal> </ogc:PropertyIsEqualTo> </ogc:And> </ogc:Filter> </wfs:Query> </wfs:GetFeature>
		//<?xml version="1.0" encoding="UTF-8" standalone="no"?><wfs:GetFeature xmlns:wfs="http://www.opengis.net/wfs" xmlns:ows="http://www.opengis.net/ows/1.1" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" service="WFS" version="1.1.0" xsi:schemaLocation="http://www.opengis.net/wfs http://schemas.opengis.net/wfs/1.1.0/wfs.xsd"> <wfs:Query srsName="EPSG:900913" typeName="wqp_sites"> <ogc:Filter xmlns:ogc="http://www.opengis.net/ogc"> <ogc:And> <ogc:BBOX> <gml:Envelope xmlns:gml="http://www.opengis.net/gml" srsName="EPSG:900913"> <gml:lowerCorner>-10534634.541594 5203332.5364988</gml:lowerCorner> <gml:upperCorner>-9791055.1305685 5614258.0004863</gml:upperCorner> </gml:Envelope> </ogc:BBOX> </ogc:And> </ogc:Filter> </wfs:Query> </wfs:GetFeature>
	}


	@Test
	public void testSearchParse_body_IS_withoutSearchParams() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("POST");
		request.setContentType("application/xml");
		
		request.setContent(noSearchParamOcgXml.getBytes());
		
		OgcWfsParser parser = new OgcWfsParser(request);
		String searchParams = parser.searchParams();
		String bodyMinusSearchParams = parser.getBodyMinusSearchParams().replaceAll("\\s+"," ");
		String body = parser.getBody().replaceAll("\\s+", " ");
		
		System.out.println();
		System.out.println(searchParams);
		System.out.println(body);
		System.out.println(bodyMinusSearchParams);
		System.out.println();
		
		assertEquals("searchParams should be the empty string when not provided", "", searchParams);
		assertEquals("we expect the original to be the same when searchParams are not provided", body.length(),  bodyMinusSearchParams.length() );
		assertEquals("body and body minus searchParams should be the same when searchParams are not provided",  body, bodyMinusSearchParams );
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
		
		SearchParameters<String, List<String>> searchParams = new SearchParameters<String, List<String>>();
		Map<String, String> wxsParams = new HashMap<String, String>();
		ProxyUtil.separateParameters(requestParams, wxsParams, searchParams);
		
		System.out.println();
		System.out.println(searchParams);
		System.out.println();
		
		assertEquals("US", searchParams.get("countrycode").get(0));
		assertEquals("US:55", searchParams.get("statecode").get(0));
		assertEquals("US:28", searchParams.get("statecode").get(1));
		assertEquals("US:32", searchParams.get("statecode").get(2));
		assertEquals("Atrazine", searchParams.get("characteristicName").get(0));
	}

}
