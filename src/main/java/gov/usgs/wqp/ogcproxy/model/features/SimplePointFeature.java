package gov.usgs.wqp.ogcproxy.model.features;

import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.opengis.feature.simple.SimpleFeature;

import com.google.gson.JsonObject;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

import gov.usgs.wqp.ogcproxy.model.attributes.GeoJSONAttributes;

/**
 * SimplePointFeature
 * @author prusso
 *<br /><br />
 *	SimplePointFeature maps all basic attributes from a GeoJSON result set
 *  to a Geo-Attribute set used by GeoTools in order to index and create
 *	a ShapeFile.
 */
public class SimplePointFeature {
	private String provider;
	private String name;
	private String locationName;
	private String type;
	private String searchType;
	private String huc8;
	private double longitude;
	private double latitude;
	private Point point;
	private String resultCount;

	private String orgName;
	private String orgId;

	private SimpleFeature simpleFeature;

	private GeometryFactory geometryFactory;

	public SimplePointFeature(JsonObject jsonFeature) {
		this.provider = jsonFeature.getAsJsonObject("properties").getAsJsonPrimitive(GeoJSONAttributes.PROVIDER).getAsString();
		this.orgId = jsonFeature.getAsJsonObject("properties").getAsJsonPrimitive(GeoJSONAttributes.ORG_ID).getAsString();
		this.orgName = jsonFeature.getAsJsonObject("properties").getAsJsonPrimitive(GeoJSONAttributes.ORG_NAME).getAsString();
		this.name = jsonFeature.getAsJsonObject("properties").getAsJsonPrimitive(GeoJSONAttributes.NAME).getAsString();
		this.locationName = jsonFeature.getAsJsonObject("properties").getAsJsonPrimitive(GeoJSONAttributes.LOC_NAME).getAsString();
		this.type = jsonFeature.getAsJsonObject("properties").getAsJsonPrimitive(GeoJSONAttributes.TYPE).getAsString();
		this.searchType = jsonFeature.getAsJsonObject("properties").getAsJsonPrimitive(GeoJSONAttributes.SEARCH_TYPE).getAsString();
		this.huc8 = jsonFeature.getAsJsonObject("properties").getAsJsonPrimitive(GeoJSONAttributes.HUC8).getAsString();
		this.longitude = jsonFeature.getAsJsonObject("geometry").getAsJsonArray(GeoJSONAttributes.POINT).get(0).getAsDouble();
		this.latitude = jsonFeature.getAsJsonObject("geometry").getAsJsonArray(GeoJSONAttributes.POINT).get(1).getAsDouble();
		this.resultCount = jsonFeature.getAsJsonObject("properties").getAsJsonPrimitive(GeoJSONAttributes.RESULT_COUNT).getAsString();

		this.geometryFactory = JTSFactoryFinder.getGeometryFactory();
		this.point = this.geometryFactory.createPoint(new Coordinate(this.longitude, this.latitude));
	}

	public SimpleFeature getSimpleFeature(SimpleFeatureBuilder featureBuilder) {
		/**
		 * Must be in order of the SimpleFeatureType defined below in getFeatureType()
		 */
		featureBuilder.add(this.point);
		featureBuilder.add(this.orgId);
		featureBuilder.add(this.orgName);
		featureBuilder.add(this.name);
		featureBuilder.add(this.locationName);
		featureBuilder.add(this.type);
		featureBuilder.add(this.searchType);
		featureBuilder.add(this.huc8);
		featureBuilder.add(this.provider);
		featureBuilder.add(this.resultCount);
		this.simpleFeature = featureBuilder.buildFeature(null);

		return this.simpleFeature;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();

		sb.append("SimplePointFeature Instance:");
		sb.append("\tProvider:\t" + this.provider + "\n");
		sb.append("\tName:\t\t" + this.name + "\n");
		sb.append("\tLocation Name:\t" + this.locationName + "\n");
		sb.append("\tType:\t\t" + this.type + "\n");
		sb.append("\tSearchType:\t\t" + this.searchType + "\n");
		sb.append("\tHUC8:\t\t" + this.huc8 + "\n");
		sb.append("\tOrgName:\t" + this.orgName + "\n");
		sb.append("\tOrgId:\t\t" + this.orgId + "\n");
		sb.append("\tLongitude:\t" + this.longitude + "\n");
		sb.append("\tLatitude:\t" + this.latitude + "\n");
		sb.append("\tResultCount:\t" + this.resultCount + "\n");
		sb.append("\tPOINT: " + this.point + "\n");

		return sb.toString();
	}

}
