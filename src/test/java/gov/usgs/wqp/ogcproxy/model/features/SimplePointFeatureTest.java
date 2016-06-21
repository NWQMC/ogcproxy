package gov.usgs.wqp.ogcproxy.model.features;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.referencing.CRS;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.vividsolutions.jts.geom.Coordinate;

import gov.usgs.wqp.ogcproxy.model.attributes.FeatureAttributeType;

public class SimplePointFeatureTest {

	public static final String JSON_FEATURE = 
			"{\"type\": \"Feature\",\"geometry\": {\"type\": \"Point\",\"coordinates\": [-88.9773314,43.3836014]}," +
			"\"properties\": {\"ProviderName\": \"UNKNOWN\",\"OrganizationIdentifier\": \"BBGGR\"," +
			"\"OrganizationFormalName\": \"Berry Berry Good Garden Rentals\",\"MonitoringLocationIdentifier\": \"BBGGR-00000123\"," +
			"\"MonitoringLocationName\": \"DS1 Well\",\"MonitoringLocationTypeName\": \"Well of mine\"," +
			"\"ResolvedMonitoringLocationTypeName\": \"Well\",\"HUCEightDigitCode\": \"12345687\"}}";

	@Test
	public void testConstructorAndFeatureOrdering() {
		//Might seem a little tedious/unnecessary, but if getFeatureType() and getSimpleFeature() do not
		//match, the data will be all hosed.
		SimpleFeatureType featureType = null;
		try {
			featureType = SimplePointFeature.getFeatureType();
			assertEquals(CRS.decode("EPSG:4326"), featureType.getCoordinateReferenceSystem());
		} catch (Exception e) {
			fail("Failed requesting featureType: " + e.getMessage());
		}
		assertEquals("Location", featureType.getName().getLocalPart());
		assertEquals(9, featureType.getAttributeCount());
		assertEquals("the_geom", featureType.getAttributeDescriptors().get(0).getName().getLocalPart());
		assertEquals(FeatureAttributeType.orgId.toString(), featureType.getAttributeDescriptors().get(1).getName().getLocalPart());
		assertEquals(FeatureAttributeType.orgName.toString(), featureType.getAttributeDescriptors().get(2).getName().getLocalPart());
		assertEquals(FeatureAttributeType.name.toString(), featureType.getAttributeDescriptors().get(3).getName().getLocalPart());
		assertEquals(FeatureAttributeType.locName.toString(), featureType.getAttributeDescriptors().get(4).getName().getLocalPart());
		assertEquals(FeatureAttributeType.type.toString(), featureType.getAttributeDescriptors().get(5).getName().getLocalPart());
		assertEquals(FeatureAttributeType.searchType.toString(), featureType.getAttributeDescriptors().get(6).getName().getLocalPart());
		assertEquals(FeatureAttributeType.huc8.toString(), featureType.getAttributeDescriptors().get(7).getName().getLocalPart());
		assertEquals(FeatureAttributeType.provider.toString(), featureType.getAttributeDescriptors().get(8).getName().getLocalPart());

		SimplePointFeature currentPointFeature = new SimplePointFeature((JsonObject) new JsonParser().parse(JSON_FEATURE));

		SimpleFeature generatedFeature = currentPointFeature.getSimpleFeature(new SimpleFeatureBuilder(SimplePointFeature.getFeatureType()));
		assertEquals(JTSFactoryFinder.getGeometryFactory().createPoint(new Coordinate(-88.9773314, 43.3836014)),
				generatedFeature.getAttribute("the_geom"));
		assertEquals("BBGGR", generatedFeature.getAttribute(FeatureAttributeType.orgId.toString()));
		assertEquals("Berry Berry Good Garden Rentals", generatedFeature.getAttribute(FeatureAttributeType.orgName.toString()));
		assertEquals("BBGGR-00000123", generatedFeature.getAttribute(FeatureAttributeType.name.toString()));
		assertEquals("DS1 Well", generatedFeature.getAttribute(FeatureAttributeType.locName.toString()));
		assertEquals("Well of mine", generatedFeature.getAttribute(FeatureAttributeType.type.toString()));
		assertEquals("Well", generatedFeature.getAttribute(FeatureAttributeType.searchType.toString()));
		assertEquals("12345687", generatedFeature.getAttribute(FeatureAttributeType.huc8.toString()));
		assertEquals("UNKNOWN", generatedFeature.getAttribute(FeatureAttributeType.provider.toString()));
	}

}
