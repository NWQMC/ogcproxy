package gov.usgs.wqp.ogcproxy.model.attributes;

/**
 * FeatureAttributeType
 * @author prusso
 *<br /><br />
 *	This enumeration explicitly defines all known Geo-Feature Attributes possible
 *	in a dataset used for creating a shapefile.
 *
 *	Note that the names are limited to 10 characters!!!
 */
public enum FeatureAttributeType {
	provider, orgName, orgId, locName, name, type, searchType, point, huc8, resultCnt;

}
