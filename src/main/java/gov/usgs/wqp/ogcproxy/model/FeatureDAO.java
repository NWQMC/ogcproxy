package gov.usgs.wqp.ogcproxy.model;


import gov.usgs.wqp.ogcproxy.model.attributes.BaseAttributeType;
import gov.usgs.wqp.ogcproxy.model.attributes.FeatureAttributeType;

import java.util.List;

import org.opengis.feature.simple.SimpleFeature;

/**
 * FeatureDAO
 * @author prusso
 *<br /><br />
 *	This interface is designed to allow a flexible API for using the ShapeFile
 *	creation logic within this package.  As long as all BaseAttributeType's and
 *	FeatureAttributeType's are defined in an enumeration, any Geo-Feature attribute
 *	set can be used to map to another System's Base attribute set.
 */
public interface FeatureDAO {
	public List<BaseAttributeType> listBaseAttributes();
	public String getBaseAttribute(BaseAttributeType baseType);
	
	public List<FeatureAttributeType> listFeatureAttributes();
	public Object getFeatureAttribute(FeatureAttributeType featureType);
	
	public SimpleFeature getSimpleFeature();
}
