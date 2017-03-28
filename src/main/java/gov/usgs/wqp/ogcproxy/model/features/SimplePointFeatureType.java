package gov.usgs.wqp.ogcproxy.model.features;

import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.opengis.feature.simple.SimpleFeatureType;

import com.vividsolutions.jts.geom.Point;

import gov.usgs.wqp.ogcproxy.model.attributes.FeatureAttributeType;

public class SimplePointFeatureType {

	public static final SimpleFeatureType FEATURETYPE;
	static {
		SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
		builder.setName("Location");
		// Coordinate reference system
		builder.setSRS("EPSG:4326");

		// add attributes in order
		builder.add("the_geom", Point.class);
		builder.length(32).add(FeatureAttributeType.orgId.toString(), String.class);
		builder.length(128).add(FeatureAttributeType.orgName.toString(), String.class);
		builder.length(32).add(FeatureAttributeType.name.toString(), String.class);
		builder.length(256).add(FeatureAttributeType.locName.toString(), String.class);
		builder.length(32).add(FeatureAttributeType.type.toString(), String.class);
		builder.length(32).add(FeatureAttributeType.searchType.toString(), String.class);
		builder.length(32).add(FeatureAttributeType.huc8.toString(), String.class);
		builder.length(32).add(FeatureAttributeType.provider.toString(), String.class);
		builder.length(32).add(FeatureAttributeType.sampleCnt.toString(), String.class);
		builder.length(32).add(FeatureAttributeType.resultCnt.toString(), String.class);
                builder.length(32).add(FeatureAttributeType.activityCn.toString(), String.class);

		// build the type
		FEATURETYPE = builder.buildFeatureType();
	}

	private SimplePointFeatureType() {
	}

}
