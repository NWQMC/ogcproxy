package gov.usgs.wqp.ogcproxy.model.parser.xml.ogc;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.hsqldb.lib.StringInputStream;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class OgcFilterDiscoveryTest {

	String ogc = "<wfs:GetFeature xmlns:wfs=\"http://www.opengis.net/wfs\" service=\"WFS\" version=\"1.1.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.opengis.net/wfs http://schemas.opengis.net/wfs/1.1.0/wfs.xsd\"><wfs:Query typeName=\"feature:dynamicSites_-1128922093\" srsName=\"EPSG:900913\"><ogc:Filter xmlns:ogc=\"http://www.opengis.net/ogc\"><ogc:BBOX><gml:Envelope xmlns:gml=\"http://www.opengis.net/gml\" srsName=\"EPSG:900913\"><gml:lowerCorner>-10485928.020674 5183674.2348657</gml:lowerCorner><gml:upperCorner>-10471557.859359 5196821.4037284</gml:upperCorner></gml:Envelope></ogc:BBOX></ogc:Filter></wfs:Query></wfs:GetFeature>";


	@Test
	public void testSuccessfulParse_BAIS() throws Exception {

		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
	    dbf.setNamespaceAware(true);
		DocumentBuilder db = dbf.newDocumentBuilder();
		
		ByteArrayInputStream bais = new ByteArrayInputStream(ogc.getBytes());
		InputSource input = new InputSource(bais);
		
		Document dom = db.parse( input );
		assertNotNull(dom.getDocumentElement());		
		
	    
		NodeList nodes = dom.getElementsByTagName("ogc:Filter");

		childNodes(nodes);
	}
	
	
	
	private void childNodes(NodeList nodes) {
		System.out.println( nodes.getLength());
		
		for (int j = 0; j < nodes.getLength(); j++) {
	        Node child = nodes.item(j);

	        if (child != null) {// || (child.getNodeType() != Node.ELEMENT_NODE)) {
				System.out.println(child.getNodeName());
				System.out.println(child.getTextContent());
				
				if (child.hasChildNodes()) 
					childNodes(child.getChildNodes());
	        }
		}
	}
	
	@Test
	public void testFailedParse_StringInputStream() throws Exception {

		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
	    dbf.setNamespaceAware(true);
		DocumentBuilder db = dbf.newDocumentBuilder();
		
		StringInputStream sis = new StringInputStream(ogc);
		InputSource input = new InputSource(sis);
		
		try {
			db.parse( input );
			fail("StringInputStream should cause XML parser exception probably because of UNICODE");
		} catch(Exception e) {
			// expected
		}
	}
	
	
}
