package gov.usgs.wqp.ogcproxy.model.parser.xml.wqx.handler;

import java.io.CharArrayWriter;
import java.util.LinkedList;
import java.util.List;

import org.apache.xerces.parsers.SAXParser;
import org.geotools.feature.SchemaException;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import gov.usgs.wqp.ogcproxy.model.FeatureDAO;
import gov.usgs.wqp.ogcproxy.model.features.SimplePointFeature;
import gov.usgs.wqp.ogcproxy.model.providers.SourceProvider;


/**
 * SimplePointLocationHandler parses the WQX_Outbound XML format for <MonitoringLocation> elements.
 * <br /><br />
 * The XML format is as follows:
 * <pre>
 * {@code
 * 		<WQX-Outbound xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
 * 		    <Provider>
 * 		        <ProviderName>STEWARDS</ProviderName>
 *
 * 		        <Organization>
 *					<OrganizationDescription>*
 *						<OrganizationIdentifier>USGS-IA</OrganizationIdentifier>*
 *						<OrganizationFormalName>USGS Iowa Water Science Center</OrganizationFormalName>*
 *					</OrganizationDescription>*
 * 		        	<MonitoringLocation>
 * 		                <MonitoringLocationIdentity>
 * 		                    <MonitoringLocationIdentifier>ARS-IAWC-IAWC225</MonitoringLocationIdentifier>
 *							<MonitoringLocationName>Squaw Creek near Stanhope, IA</MonitoringLocationName>*
 * 		                    <ResolvedMonitoringLocationTypeName>Land</ResolvedMonitoringLocationTypeName>
 * 		                </MonitoringLocationIdentity>
 * 		                <MonitoringLocationGeospatial>
 * 		                    <LatitudeMeasure>41.9607224179</LatitudeMeasure>
 * 		                    <LongitudeMeasure>-93.698220503</LongitudeMeasure>
 * 		                </MonitoringLocationGeospatial>
 * 		            </MonitoringLocation>
 * 		            <MonitoringLocation>
 * 		                <MonitoringLocationIdentity>
 * 		                    <MonitoringLocationIdentifier>ARS-IAWC-IAWC410</MonitoringLocationIdentifier>
 * 		                    <ResolvedMonitoringLocationTypeName>Stream</ResolvedMonitoringLocationTypeName>
 * 		                </MonitoringLocationIdentity>
 * 		                <MonitoringLocationGeospatial>
 * 		                    <LatitudeMeasure>41.9505493342</LatitudeMeasure>
 * 		                    <LongitudeMeasure>-93.759072857</LongitudeMeasure>
 * 		                </MonitoringLocationGeospatial>
 * 		            </MonitoringLocation>
 * 		            <MonitoringLocation>
 * 		                <MonitoringLocationIdentity>
 * 		                    <MonitoringLocationIdentifier>ARS-IAWC-IAWC450</MonitoringLocationIdentifier>
 * 		                    <ResolvedMonitoringLocationTypeName>Stream</ResolvedMonitoringLocationTypeName>
 * 		                </MonitoringLocationIdentity>
 * 		                <MonitoringLocationGeospatial>
 * 		                    <LatitudeMeasure>41.9216043545</LatitudeMeasure>
 * 		                    <LongitudeMeasure>-93.756546312</LongitudeMeasure>
 * 		                </MonitoringLocationGeospatial>
 * 		            </MonitoringLocation>
 * 		        </Organization>
 *
 * 		    </Provider>
 * 		</WQX-Outbound>
 * }
 * </pre>
 *
 *
 *
 * @author prusso
 *
 */

public class SimplePointLocationHandler extends DefaultHandler {
	private static final Logger LOG = LoggerFactory.getLogger(SimplePointLocationHandler.class);
	private List<FeatureDAO> simplePointFeatures;
	private List<SimplePointFeature> currentFeatures;
	private SourceProvider currentSourceProvider;
	private SimplePointFeature currentPointFeature;
	private String currentOrgId;
	private String currentOrgName;
	
	public static final String ORGANIZATION_START = "OrganizationDescription";
	public static final String ORGANIZATION_IDENTIFIER = "OrganizationIdentifier";
	public static final String ORGANIZATION_NAME = "OrganizationFormalName";
	
	public static final String LOCATION_START = "MonitoringLocation";
	public static final String LOCATION_IDENTIFIER = "MonitoringLocationIdentifier";
	public static final String LOCATION_TYPE = "ResolvedMonitoringLocationTypeName";
	public static final String LOCATION_NAME = "MonitoringLocationName";
	
	public static final String LATTITUDE = "LatitudeMeasure";
	public static final String LONGITUDE = "LongitudeMeasure";
	
	private SimplePointProviderHandler parentHandler;
	private SAXParser xmlReader;
	private CharArrayWriter contents = new CharArrayWriter();
	
	private SimpleFeatureBuilder featureBuilder;
	
	public SimplePointLocationHandler(SimplePointProviderHandler parentHandler, SAXParser xmlReader, List<FeatureDAO> featureList, SourceProvider sourceProvider, SimpleFeatureBuilder featureBuilder) throws SchemaException, NoSuchAuthorityCodeException, FactoryException {
		this.parentHandler = parentHandler;
		this.xmlReader = xmlReader;
		this.simplePointFeatures = featureList;
		this.currentSourceProvider = sourceProvider;
		
		this.featureBuilder = featureBuilder;
		
		this.currentPointFeature = null;
		
		this.currentFeatures = new LinkedList<SimplePointFeature>();
	}
	
	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		contents.reset();
		
		if (SimplePointLocationHandler.LOCATION_START.equals(qName)) {
			this.currentPointFeature = new SimplePointFeature(this.featureBuilder, this.currentSourceProvider);
		}
	}
	
	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		/**
		 * SimplePointFeature organization ID and Name element values stored for later use;
		 */
		if (SimplePointLocationHandler.ORGANIZATION_IDENTIFIER.equals(qName)) {
			this.currentOrgId = contents.toString();
		}
		if (SimplePointLocationHandler.ORGANIZATION_NAME.equals(qName)) {
			this.currentOrgName = contents.toString();
		}
		
		/**
		 * SimplePointFeature name element
		 */
		if (SimplePointLocationHandler.LOCATION_NAME.equals(qName)) {
			if (this.currentPointFeature != null) {
				this.currentPointFeature.setLocationName(contents.toString());
			} else {
				String error = "SimplePointLocationHandler.endElement() ERROR: Element name [" +
						  localName + "] found but no SimplePointFeature object created!";
				LOG.debug(error);
			}
		}
		
		/**
		 * SimplePointFeature name element
		 */
		if (SimplePointLocationHandler.LOCATION_IDENTIFIER.equals(qName)) {
			if (this.currentPointFeature != null) {
				this.currentPointFeature.setName(contents.toString());
			} else {
				String error = "SimplePointLocationHandler.endElement() ERROR: Element name [" +
						  localName + "] found but no SimplePointFeature object created!";
				LOG.debug(error);
			}
		}
		
		/**
		 * SimplePointFeature type element
		 */
		if (SimplePointLocationHandler.LOCATION_TYPE.equals(qName)) {
			if (this.currentPointFeature != null) {
				this.currentPointFeature.setType(contents.toString());
			} else {
				String error = "SimplePointLocationHandler.endElement() ERROR: Element name [" +
						  localName + "] found but no SimplePointFeature object created!";
				LOG.debug(error);
			}
		}
		
		/**
		 * SimplePointFeature latitude element
		 */
		if (SimplePointLocationHandler.LATTITUDE.equals(qName)) {
			if (this.currentPointFeature != null) {
				double value = 0.0;
				String stringValue = contents.toString();
				try {
					value = Double.parseDouble(stringValue);
				} catch (NumberFormatException e) {
					String error = "SimplePointLocationHandler.endElement() ERROR: Latitude value [" + stringValue +
							  "] could not be parsed as a double value.  Setting latitude to 0.0";
					LOG.debug(error);
				} catch (NullPointerException e) {
					String error = "SimplePointLocationHandler.endElement() ERROR: Latitude value is null and " +
							  "could not be parsed as a double value.  Setting latitude to 0.0";
					LOG.debug(error);
				}
				
				this.currentPointFeature.setLatitude(value);
			} else {
				String error = "SimplePointLocationHandler.endElement() ERROR: Element name [" +
						  localName + "] found but no SimplePointFeature object created!";
				LOG.debug(error);
			}
		}
		
		/**
		 * SimplePointFeature longitude element
		 */
		if (SimplePointLocationHandler.LONGITUDE.equals(qName)) {
			if (this.currentPointFeature != null) {
				double value = 0.0;
				String stringValue = contents.toString();
				try {
					value = Double.parseDouble(stringValue);
				} catch (NumberFormatException e) {
					String error = "SimplePointLocationHandler.endElement() ERROR: Longitude value [" + stringValue +
							  "] could not be parsed as a double value.  Setting longitude to 0.0";
					LOG.debug(error);
				} catch (NullPointerException e) {
					String error = "SimplePointLocationHandler.endElement() ERROR: Longitude value is null and " +
							  "could not be parsed as a double value.  Setting longitude to 0.0";
					LOG.debug(error);
				}
				
				this.currentPointFeature.setLongitude(value);
			} else {
				String error = "SimplePointLocationHandler.endElement() ERROR: Element name [" +
						  localName + "] found but no SimplePointFeature object created!";
				LOG.debug(error);
			}
		}
		
		/**
		 * The ending tag for this feature
		 */
		if (SimplePointLocationHandler.LOCATION_START.equals(qName)) {
			// TODO this class should really return the currentFeatures rather than adding to an externally supplied collection
			this.simplePointFeatures.add(this.currentPointFeature);
			this.currentFeatures.add(this.currentPointFeature);
		}
		
		/**
		 * Tag flag to return handling to the parent
		 */
		if (SimplePointProviderHandler.SUBHANDLER_ELEMENT.equals(qName)) {
			if (this.currentOrgId != null || this.currentOrgName != null) {
				
				for (SimplePointFeature feature: this.currentFeatures) {
					if (this.currentOrgId != null) {
						feature.setOrgId(this.currentOrgId);
					}
					if (this.currentOrgName != null) {
						feature.setOrgName(this.currentOrgName);
					}
				}
			}
			this.currentFeatures = null;
			this.currentFeatures = new LinkedList<SimplePointFeature>(); // TODO necessary? While it was for testing it might not be needed.
			
			this.currentOrgId = this.currentOrgName = null;
			
			this.xmlReader.setContentHandler(this.parentHandler);
			return;
		}
	}

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		contents.write(ch, start, length);
	}

	public SimplePointFeature getCurrentPointFeature() {
		return currentPointFeature;
	}

}
