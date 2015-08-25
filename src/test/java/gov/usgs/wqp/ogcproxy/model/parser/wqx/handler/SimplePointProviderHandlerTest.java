package gov.usgs.wqp.ogcproxy.model.parser.wqx.handler;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import gov.usgs.wqp.ogcproxy.model.FeatureDAO;
import gov.usgs.wqp.ogcproxy.model.features.SimplePointFeature;
import gov.usgs.wqp.ogcproxy.model.parser.xml.wqx.handler.SimplePointLocationHandler;
import gov.usgs.wqp.ogcproxy.model.parser.xml.wqx.handler.SimplePointProviderHandler;
import gov.usgs.wqp.ogcproxy.model.providers.SourceProvider;

import java.util.ArrayList;
import java.util.List;

import org.apache.xerces.parsers.SAXParser;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class SimplePointProviderHandlerTest {
	private SAXParser xmlReader;
	SimpleFeatureBuilder featureBuilder;
	private List<FeatureDAO> featureList;
	
	@Before
	public void init() throws Exception {
		xmlReader      = mock(SAXParser.class);
		featureBuilder = mock(SimpleFeatureBuilder.class);
		featureList    = new ArrayList<FeatureDAO>();
	}
	
	@After
	public void destroy() throws Exception {
		
	}
	
	@Test
	public void testStartElementNotInteresting() {
		SimplePointProviderHandler providerHandler = new SimplePointProviderHandler(this.xmlReader, this.featureList, this.featureBuilder);
		
		assertEquals(SourceProvider.UNKNOWN, providerHandler.getCurrentProvider());
		
		try {
			providerHandler.startElement("uri", "localName", "qName", null);
		} catch (Exception e) {
			fail("Error calling startElement(): " + e.getMessage());
		}
		
		assertEquals(SourceProvider.UNKNOWN, providerHandler.getCurrentProvider());
	}
	
	@Test
	public void testStartElementInteresting() {
		SimplePointProviderHandler providerHandler = new SimplePointProviderHandler(this.xmlReader, this.featureList, this.featureBuilder);
		
		assertEquals(SourceProvider.UNKNOWN, providerHandler.getCurrentProvider());
		
		try {
			providerHandler.startElement("uri", "localName", "Organization", null);
		} catch (Exception e) {
			fail("Error calling startElement(): " + e.getMessage());
		}
		
		verify(this.xmlReader).setContentHandler(any(SimplePointLocationHandler.class));
		
		assertEquals(SourceProvider.UNKNOWN, providerHandler.getCurrentProvider());
	}
	
	@Test
	public void testEndElementNotInteresting() {
		SimplePointProviderHandler providerHandler = new SimplePointProviderHandler(this.xmlReader, this.featureList, this.featureBuilder);
		
		assertEquals(SourceProvider.UNKNOWN, providerHandler.getCurrentProvider());
		
		try {
			providerHandler.endElement("uri", "localName", "qName");
		} catch (Exception e) {
			fail("Error calling endElement(): " + e.getMessage());
		}
		
		assertEquals(SourceProvider.UNKNOWN, providerHandler.getCurrentProvider());
	}
	
	@Test
	public void testEndElementInteresting() {
		SimplePointProviderHandler providerHandler = new SimplePointProviderHandler(this.xmlReader, this.featureList, this.featureBuilder);
		
		assertEquals(SourceProvider.UNKNOWN, providerHandler.getCurrentProvider());
		
		/**
		 * Set up the contents character array to simulate parsing xml
		 */
		char[] testContents = {'N', 'W', 'I', 'S'};
		try {
			providerHandler.characters(testContents, 0, testContents.length);
		} catch (Exception e) {
			fail("Error calling characters(): " + e.getMessage());
		}
		
		try {
			providerHandler.endElement("uri", "localName", "ProviderName");
		} catch (Exception e) {
			fail("Error calling startElement(): " + e.getMessage());
		}
		
		assertEquals(SourceProvider.NWIS, providerHandler.getCurrentProvider());
	}
	
	String xml
	="<WQX-Outbound xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">"
	+"    <Provider>"
	+"        <ProviderName>STEWARDS</ProviderName>"
	+"        <Organization>"
	+"			<OrganizationDescription>"
	+"				<OrganizationIdentifier>USGS1-ID</OrganizationIdentifier>"
	+"				<OrganizationFormalName>USGS1-NAME</OrganizationFormalName>"
	+"			</OrganizationDescription>"
	+"        	<MonitoringLocation>"
	+"                <MonitoringLocationIdentity>"
	+"					  <MonitoringLocationName>AAA</MonitoringLocationName>"
	+"                    <MonitoringLocationIdentifier>ARS-IAWC-IAWC225</MonitoringLocationIdentifier>"
	+"                    <ResolvedMonitoringLocationTypeName>Land</ResolvedMonitoringLocationTypeName>"
	+"                </MonitoringLocationIdentity>"
	+"                <MonitoringLocationGeospatial>"
	+"                    <LatitudeMeasure>41.9607224179</LatitudeMeasure>"
	+"                    <LongitudeMeasure>-93.698220503</LongitudeMeasure>"
	+"                </MonitoringLocationGeospatial>"
	+"          </MonitoringLocation>"
	+"          <MonitoringLocation>"
	+"                <MonitoringLocationIdentity>"
	+"					  <MonitoringLocationName>BBB</MonitoringLocationName>"
	+"                    <MonitoringLocationIdentifier>ARS-IAWC-IAWC410</MonitoringLocationIdentifier>"
	+"                    <ResolvedMonitoringLocationTypeName>Stream</ResolvedMonitoringLocationTypeName>"
	+"                </MonitoringLocationIdentity>"
	+"                <MonitoringLocationGeospatial>"
	+"                    <LatitudeMeasure>41.9505493342</LatitudeMeasure>"
	+"                    <LongitudeMeasure>-93.759072857</LongitudeMeasure>"
	+"                </MonitoringLocationGeospatial>"
	+"          </MonitoringLocation>"
	+"          <MonitoringLocation>"
	+"                <MonitoringLocationIdentity>"
	+"					  <MonitoringLocationName>CCC</MonitoringLocationName>"
	+"                    <MonitoringLocationIdentifier>ARS-IAWC-IAWC450</MonitoringLocationIdentifier>"
	+"                    <ResolvedMonitoringLocationTypeName>Stream</ResolvedMonitoringLocationTypeName>"
	+"                </MonitoringLocationIdentity>"
	+"                <MonitoringLocationGeospatial>"
	+"                    <LatitudeMeasure>41.9216043545</LatitudeMeasure>"
	+"                    <LongitudeMeasure>-93.756546312</LongitudeMeasure>"
	+"                </MonitoringLocationGeospatial>"
	+"            </MonitoringLocation>"
	+"        </Organization>"
	+"        <Organization>"
	+"			<OrganizationDescription>"
	+"				<OrganizationIdentifier>USGS2-ID</OrganizationIdentifier>"
	+"				<OrganizationFormalName>USGS2-NAME</OrganizationFormalName>"
	+"			</OrganizationDescription>"
	+"        	<MonitoringLocation>"
	+"                <MonitoringLocationIdentity>"
	+"					  <MonitoringLocationName>DDD</MonitoringLocationName>"
	+"                    <MonitoringLocationIdentifier>ARS-IAWC-IAWC225</MonitoringLocationIdentifier>"
	+"                    <ResolvedMonitoringLocationTypeName>Land</ResolvedMonitoringLocationTypeName>"
	+"                </MonitoringLocationIdentity>"
	+"                <MonitoringLocationGeospatial>"
	+"                    <LatitudeMeasure>41.9607224179</LatitudeMeasure>"
	+"                    <LongitudeMeasure>-93.698220503</LongitudeMeasure>"
	+"                </MonitoringLocationGeospatial>"
	+"          </MonitoringLocation>"
	+"          <MonitoringLocation>"
	+"                <MonitoringLocationIdentity>"
	+"					  <MonitoringLocationName>EEE</MonitoringLocationName>"
	+"                    <MonitoringLocationIdentifier>ARS-IAWC-IAWC410</MonitoringLocationIdentifier>"
	+"                    <ResolvedMonitoringLocationTypeName>Stream</ResolvedMonitoringLocationTypeName>"
	+"                </MonitoringLocationIdentity>"
	+"                <MonitoringLocationGeospatial>"
	+"                    <LatitudeMeasure>41.9505493342</LatitudeMeasure>"
	+"                    <LongitudeMeasure>-93.759072857</LongitudeMeasure>"
	+"                </MonitoringLocationGeospatial>"
	+"          </MonitoringLocation>"
	+"        </Organization>"
	+"    </Provider>"
	+" </WQX-Outbound>";
	
	
	@Test
	public void testOrganizationSetOnEachSimpleFeature() throws Exception {
//		SimplePointProviderHandler providerHandler = new SimplePointProviderHandler(this.xmlReader, this.featureList, this.featureBuilder);
		SimplePointLocationHandler providerHandler = new SimplePointLocationHandler(null, this.xmlReader, this.featureList, SourceProvider.NWIS, this.featureBuilder);
		
		String[] xmllines = this.xml.split(">\\s+");
		
		System.out.println(xmllines.length);
		
		for (String element : xmllines) {
			String chars = element = element.trim();
			if (element.length() == 0) continue;
					
			// START TAG
			
			int start;
			boolean doEndTag = true;
			if ( (start = element.indexOf("</") + 2) == 1) {
				start = element.indexOf("<") + 1;
				doEndTag = false;
			}
			if (start > 0) {
				element = element.substring(start);
			}
			System.out.println(element);
			if ( ! doEndTag ) {
				providerHandler.startElement(null, null, element, null);
			}
			// CONTENT TEXT
			
			int startChar = chars.indexOf(">") + 1;
			int endChar   = chars.indexOf("</");
			if (endChar < 0 || endChar < startChar) {
				startChar = endChar = 0;
			}
			
			if (endChar != 0) {
				System.out.println("---" + chars.substring(startChar, endChar) + "---");
				providerHandler.characters(chars.toCharArray(), startChar, endChar-startChar);
				doEndTag = true;
			}
			
			// END TAG
			
			if (doEndTag) {
				providerHandler.endElement(null, null, element);
				System.out.println( featureList.size() );
			}
			
			// need to play a few games because this is not a real xml parser.
			// this clears the conetent without actully starting a new location.
			providerHandler.startElement(null, null, null, null);
 		}
		
		assertEquals("Expect 5 point features", 5, featureList.size());
		
		for (FeatureDAO feature : featureList) {
			if (feature instanceof SimplePointFeature) {
				SimplePointFeature point = (SimplePointFeature) feature;
				assertFalse("Organization Id should not be null.", null == point.getOrgId());
				assertFalse("Organization Id should not be empty.", point.getOrgId().trim().length() == 0);
				System.out.println( point );
				
			} else {
				fail("All entries should be " + SimplePointFeature.class.getName());
			}
		}
		for (int i=0; i<3; i++) {
			FeatureDAO feature = featureList.get(i);
			if (feature instanceof SimplePointFeature) {
				SimplePointFeature point = (SimplePointFeature) feature;
				assertEquals("USGS1-ID",point.getOrgId());
				assertEquals("USGS1-NAME",point.getOrgName());
			} else {
				fail("All entries should be " + SimplePointFeature.class.getName());
			}
		}
		for (int i=3; i<5; i++) {
			FeatureDAO feature = featureList.get(i);
			if (feature instanceof SimplePointFeature) {
				SimplePointFeature point = (SimplePointFeature) feature;
				assertEquals("USGS2-ID",point.getOrgId());
				assertEquals("USGS2-NAME",point.getOrgName());
			} else {
				fail("All entries should be " + SimplePointFeature.class.getName());
			}
		}
		
		
		String[] locationNames = {"AAA","BBB","CCC","DDD","EEE"};
		
		for (int i=0; i<5; i++) {
			FeatureDAO feature = featureList.get(i);
			if (feature instanceof SimplePointFeature) {
				SimplePointFeature point = (SimplePointFeature) feature;
				assertEquals("Expecting location number " +i+ "to be " + locationNames[i] + " but is " + point.getLocationName(),
						locationNames[i] ,point.getLocationName());
			} else {
				fail("All entries should be " + SimplePointFeature.class.getName());
			}
		}
		
	}
	
}
