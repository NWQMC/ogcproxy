package gov.usgs.wqp.ogcproxy.model.features;

import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.opengis.feature.simple.SimpleFeature;

import com.google.gson.JsonObject;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;

import gov.usgs.wqp.ogcproxy.model.attributes.GeoJSONAttributes;

/**
 * SimplePointFeature
 *
 * @author prusso
 * <br /><br />
 * SimplePointFeature maps all basic attributes from a GeoJSON result set to a
 * Geo-Attribute set used by GeoTools in order to index and create a ShapeFile.
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
    private String activityCount;
    private String resultCount;

    private String orgName;
    private String orgId;

    private SimpleFeature simpleFeature;

    private GeometryFactory geometryFactory;

    public SimplePointFeature(JsonObject jsonFeature) {
        JsonObject featureObject = jsonFeature.getAsJsonObject("properties");

        this.provider = featureObject.has(GeoJSONAttributes.PROVIDER) ? featureObject.getAsJsonPrimitive(GeoJSONAttributes.PROVIDER).getAsString() : "";
        this.orgId = featureObject.has(GeoJSONAttributes.ORG_ID) ? featureObject.getAsJsonPrimitive(GeoJSONAttributes.ORG_ID).getAsString() : "";
        this.orgName = featureObject.has(GeoJSONAttributes.ORG_NAME) ? featureObject.getAsJsonPrimitive(GeoJSONAttributes.ORG_NAME).getAsString() : "";
        this.name = featureObject.has(GeoJSONAttributes.NAME) ? featureObject.getAsJsonPrimitive(GeoJSONAttributes.NAME).getAsString() : "";
        this.locationName = featureObject.has(GeoJSONAttributes.LOC_NAME) ? featureObject.getAsJsonPrimitive(GeoJSONAttributes.LOC_NAME).getAsString() : "";
        this.type = featureObject.has(GeoJSONAttributes.TYPE) ? featureObject.getAsJsonPrimitive(GeoJSONAttributes.TYPE).getAsString() : "";
        this.searchType = featureObject.has(GeoJSONAttributes.SEARCH_TYPE) ? featureObject.getAsJsonPrimitive(GeoJSONAttributes.SEARCH_TYPE).getAsString() : "";
        this.huc8 = featureObject.has(GeoJSONAttributes.HUC8) ? featureObject.getAsJsonPrimitive(GeoJSONAttributes.HUC8).getAsString() : "";
        this.activityCount = featureObject.has(GeoJSONAttributes.ACTIVITY_COUNT) ? featureObject.getAsJsonPrimitive(GeoJSONAttributes.ACTIVITY_COUNT).getAsString() : "";
        this.resultCount = featureObject.has(GeoJSONAttributes.RESULT_COUNT) ? featureObject.getAsJsonPrimitive(GeoJSONAttributes.RESULT_COUNT).getAsString() : "";

        this.longitude = jsonFeature.getAsJsonObject("geometry").getAsJsonArray(GeoJSONAttributes.POINT).get(0).getAsDouble();
        this.latitude = jsonFeature.getAsJsonObject("geometry").getAsJsonArray(GeoJSONAttributes.POINT).get(1).getAsDouble();

        this.geometryFactory = JTSFactoryFinder.getGeometryFactory();
        this.point = this.geometryFactory.createPoint(new Coordinate(this.longitude, this.latitude));
    }

    public SimpleFeature getSimpleFeature(SimpleFeatureBuilder featureBuilder) {
        /**
         * Must be in order of the SimpleFeatureType defined below in
         * getFeatureType()
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
        featureBuilder.add(this.activityCount);
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
        sb.append("\tActivityCount:\t" + this.activityCount + "\n");
        sb.append("\tResultCount:\t" + this.resultCount + "\n");
        sb.append("\tPOINT: " + this.point + "\n");

        return sb.toString();
    }

}
