package gov.usgs.wqp.ogcproxy.model.features;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.referencing.CRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

import gov.usgs.wqp.ogcproxy.model.FeatureDAO;
import gov.usgs.wqp.ogcproxy.model.attributes.BaseAttributeType;
import gov.usgs.wqp.ogcproxy.model.attributes.FeatureAttributeType;
import gov.usgs.wqp.ogcproxy.model.providers.SourceProvider;

/**
 * SimplePointFeature
 * @author prusso
 *<br /><br />
 *	This class implements the FeatureDAO interface in order to seemlessly use
 *	the ShapeFile creation logic within this package.
 *<br /><br />
 *	SimplePointFeature maps all basic attributes from a WQP SimpleStation result
 *	set to a Geo-Attribute set used by GeoTools in order to index and create
 *	a ShapeFile.
 *
 */
public class SimplePointFeature implements FeatureDAO {
	private SourceProvider provider;
	private String name;
	private String locationName;
	private String type;
	private double longitude;
	private double latitude;
	private Point point;
	
	private String orgName;
	private String orgId;
	
	private Map<BaseAttributeType, String> baseAttribs;
	private Map<FeatureAttributeType, Object> featureAttribs;
	private SimpleFeature simpleFeature;
	
	private boolean featureIsDirty = true;
	private GeometryFactory geometryFactory;
	private SimpleFeatureBuilder featureBuilder;
	
	private static SimpleFeatureType FEATURETYPE;
	
	@SuppressWarnings("serial")
	public SimplePointFeature(SimpleFeatureBuilder featureBuilder, SourceProvider srcProvider) {
		this.featureBuilder = featureBuilder;
		
		this.provider = srcProvider;
		this.name = "";
		this.type = "";
		this.longitude = 0.0;
		this.latitude = 0.0;
		
		this.geometryFactory = JTSFactoryFinder.getGeometryFactory();
		this.point = this.geometryFactory.createPoint(new Coordinate(this.longitude, this.latitude));
		
		this.baseAttribs = new HashMap<BaseAttributeType, String>() {{
			put(BaseAttributeType.Provider, provider.toString());
			put(BaseAttributeType.LocationIdentifier, type);
			put(BaseAttributeType.LocationType, type);
			put(BaseAttributeType.Longitude, "" + longitude);
			put(BaseAttributeType.Latitude, "" + latitude);
		}};
		
		this.featureAttribs = new HashMap<FeatureAttributeType, Object>() {{
			put(FeatureAttributeType.provider, provider.toString());
			put(FeatureAttributeType.name, name);
			put(FeatureAttributeType.type, type);
			put(FeatureAttributeType.point, point);
		}};
		
		this.featureIsDirty = true;
	}
	
	@SuppressWarnings("serial")
	public SimplePointFeature(SimpleFeatureBuilder featureBuilder, SourceProvider srcProvider, String featureName, String featureType, double lng, double lat) {
		this.featureBuilder = featureBuilder;
		
		this.provider = srcProvider;
		this.name = featureName;
		this.type = featureType;
		this.longitude = lng;
		this.latitude = lat;
		
		this.geometryFactory = JTSFactoryFinder.getGeometryFactory();
		this.point = this.geometryFactory.createPoint(new Coordinate(this.longitude, this.latitude));
		
		baseAttribs = new HashMap<BaseAttributeType, String>() {{
			put(BaseAttributeType.Provider, provider.toString());
			put(BaseAttributeType.LocationIdentifier, name);
			put(BaseAttributeType.LocationType, type);
			put(BaseAttributeType.Longitude, "" + longitude);
			put(BaseAttributeType.Latitude, "" + latitude);
		}};
		
		featureAttribs = new HashMap<FeatureAttributeType, Object>() {{
			put(FeatureAttributeType.provider, provider.toString());
			put(FeatureAttributeType.name, name);
			put(FeatureAttributeType.type, type);
			put(FeatureAttributeType.point, point);
		}};
				
		this.featureIsDirty = true;
	}

	public SourceProvider getProvider() {
		return provider;
	}

	public void setProvider(SourceProvider provider) {
		this.provider = provider;
		baseAttribs.put(BaseAttributeType.Provider, provider.toString());
		featureAttribs.put(FeatureAttributeType.provider, provider.toString());
		this.featureIsDirty = true;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
		baseAttribs.put(BaseAttributeType.LocationIdentifier, name);
		featureAttribs.put(FeatureAttributeType.name, name);
		this.featureIsDirty = true;
	}

	public String getLocationName() {
		return locationName;
	}

	public void setLocationName(String locationName) {
		this.locationName = locationName;
		baseAttribs.put(BaseAttributeType.LocationName, locationName);
		featureAttribs.put(FeatureAttributeType.locName, name);
		this.featureIsDirty = true;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
		baseAttribs.put(BaseAttributeType.LocationType, type);
		featureAttribs.put(FeatureAttributeType.type, type);
		this.featureIsDirty = true;
	}

	public String getOrgName() {
		return orgName;
	}

	// asdf
	public void setOrgName(String orgName) {
		this.orgName = orgName;
		baseAttribs.put(BaseAttributeType.OrganizationName, orgName);
		featureAttribs.put(FeatureAttributeType.orgName, orgName);
		this.featureIsDirty = true;
	}

	public String getOrgId() {
		return orgId;
	}

	public void setOrgId(String orgId) {
		this.orgId = orgId;
		baseAttribs.put(BaseAttributeType.OrganizationId, orgId);
		featureAttribs.put(FeatureAttributeType.orgId, orgId);
		this.featureIsDirty = true;
	}
	
	
	public double getLongitude() {
		return longitude;
	}

	public void setLongitude(double longitude) {
		this.longitude = longitude;
		baseAttribs.put(BaseAttributeType.Longitude, "" + longitude);
		
		this.point = this.geometryFactory.createPoint(new Coordinate(this.longitude, this.latitude));
		
		featureAttribs.put(FeatureAttributeType.point, point);
		this.featureIsDirty = true;
	}

	public double getLatitude() {
		return latitude;
	}

	public void setLatitude(double latitude) {
		this.latitude = latitude;
		baseAttribs.put(BaseAttributeType.Latitude, "" + latitude);
		
		this.point = this.geometryFactory.createPoint(new Coordinate(this.longitude, this.latitude));
		
		featureAttribs.put(FeatureAttributeType.point, point);
		this.featureIsDirty = true;
	}

	public Point getPoint() {
		return point;
	}
	
	public void setPoint(double longitude, double latitude) {
		this.longitude = longitude;
		baseAttribs.put(BaseAttributeType.Longitude, "" + longitude);
		this.latitude = latitude;
		baseAttribs.put(BaseAttributeType.Latitude, "" + latitude);
		
		this.point = this.geometryFactory.createPoint(new Coordinate(this.longitude, this.latitude));
		
		featureAttribs.put(FeatureAttributeType.point, point);
		this.featureIsDirty = true;
	}

	public List<BaseAttributeType> listBaseAttributes() {
		return new ArrayList<BaseAttributeType>(baseAttribs.keySet());
	}

	public String getBaseAttribute(BaseAttributeType baseType) {
		return baseAttribs.get(baseType);
	}

	public List<FeatureAttributeType> listFeatureAttributes() {
		return new ArrayList<FeatureAttributeType>(featureAttribs.keySet());
	}

	public Object getFeatureAttribute(FeatureAttributeType featureType) {
		return featureAttribs.get(featureType);
	}
	
	public SimpleFeature getSimpleFeature() {
		if (this.featureIsDirty) {
			this.simpleFeature = null;
		}
		
		if (this.simpleFeature == null) {
			/**
			 * Must be in order of the SimpleFeatureType defined below in getFeatureType()
			 */
			this.featureBuilder.add(this.point);
			this.featureBuilder.add(this.orgId);
			this.featureBuilder.add(this.orgName);
			this.featureBuilder.add(this.name);
			this.featureBuilder.add(this.locationName);
			this.featureBuilder.add(this.type);
			this.featureBuilder.add(provider.toString());
			this.simpleFeature = featureBuilder.buildFeature(null);
			
			this.featureIsDirty = false;
		}
		
		return this.simpleFeature;
	}
	
	public boolean featureIsDirty() {
		return featureIsDirty;
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		
		sb.append("SimplePointFeature Instance:");
		sb.append("\tProvider:\t" + provider.toString() + "\n");
		sb.append("\tName:\t\t" + this.name + "\n");
		sb.append("\tLocation Name:\t" + this.locationName + "\n");
		sb.append("\tType:\t\t" + this.type + "\n");
		sb.append("\tOrgName:\t" + this.orgName + "\n");
		sb.append("\tOrgId:\t\t" + this.orgId + "\n");
		sb.append("\tLongitude:\t" + this.longitude + "\n");
		sb.append("\tLatitude:\t" + this.latitude + "\n");
		sb.append("\tPOINT: " + this.point + "\n");
		
		return sb.toString();
	}
	
	public static SimpleFeatureType getFeatureType() throws NoSuchAuthorityCodeException, FactoryException {
		if (SimplePointFeature.FEATURETYPE == null) {
			synchronized(SimplePointFeature.class) {
				if (SimplePointFeature.FEATURETYPE == null) {
					SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
			        builder.setName("Location");
			        builder.setCRS(CRS.decode("EPSG:4326")); // <- Coordinate reference system

			        // add attributes in order
			        builder.add("the_geom", Point.class);
			        builder.length(32).add(FeatureAttributeType.orgId.toString(), String.class);
			        builder.length(128).add(FeatureAttributeType.orgName.toString(), String.class);
			        builder.length(32).add(FeatureAttributeType.name.toString(), String.class);
			        builder.length(256).add(FeatureAttributeType.locName.toString(), String.class);
			        builder.length(32).add(FeatureAttributeType.type.toString(), String.class);
			        builder.length(32).add(FeatureAttributeType.provider.toString(), String.class);
			        
			        // build the type
			        SimplePointFeature.FEATURETYPE = builder.buildFeatureType();
				}
			}
		}
		
		return SimplePointFeature.FEATURETYPE;
	}

}
