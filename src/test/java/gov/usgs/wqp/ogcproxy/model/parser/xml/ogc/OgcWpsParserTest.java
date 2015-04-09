package gov.usgs.wqp.ogcproxy.model.parser.xml.ogc;

import static org.junit.Assert.*;

import org.junit.Test;

public class OgcWpsParserTest {

		
		final String wpsXml =
			" <?xml version=\"1.0\" encoding=\"UTF-8\"?> "+
			" <wps:Execute xmlns:wps=\"http://www.opengis.net/wps/1.0.0\" version=\"1.0.0\" service=\"WPS\" "+
			"     xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "+
			"     xsi:schemaLocation=\"http://www.opengis.net/wps/1.0.0 http://schemas.opengis.net/wps/1.0.0/wpsAll.xsd\"> "+
			"     <ows:Identifier xmlns:ows=\"http://www.opengis.net/ows/1.1\">gs:SiteImport</ows:Identifier> "+
			"     <wps:DataInputs> "+
			"         <wps:Input> "+
			"             <ows:Identifier xmlns:ows=\"http://www.opengis.net/ows/1.1\">statecode</ows:Identifier> "+
			"             <wps:Data> "+
			"                 <wps:LiteralData>US:55;US:19</wps:LiteralData> "+
			"             </wps:Data> "+
			"         </wps:Input> "+
			"         <wps:Input> "+
			"             <ows:Identifier xmlns:ows=\"http://www.opengis.net/ows/1.1\">siteType</ows:Identifier> "+
			"             <wps:Data> "+
			"                 <wps:LiteralData>Stream</wps:LiteralData> "+
			"             </wps:Data> "+
			"         </wps:Input> "+
			"     </wps:DataInputs> "+
			"     <wps:ResponseForm> "+
			"         <wps:RawDataOutput> "+
			"             <ows:Identifier xmlns:ows=\"http://www.opengis.net/ows/1.1\">result</ows:Identifier> "+
			"         </wps:RawDataOutput> "+
			"     </wps:ResponseForm> "+
			" </wps:Execute> ";
	
	@Test
	public void test() {
		assertTrue(true); // TODO impl test when impl WPS service
	}

}
