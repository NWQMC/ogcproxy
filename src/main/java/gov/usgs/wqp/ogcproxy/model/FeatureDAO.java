package gov.usgs.wqp.ogcproxy.model;


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
	public SimpleFeature getSimpleFeature();
}
