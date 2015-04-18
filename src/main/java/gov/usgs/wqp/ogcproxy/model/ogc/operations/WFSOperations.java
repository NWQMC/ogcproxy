package gov.usgs.wqp.ogcproxy.model.ogc.operations;

import static gov.usgs.wqp.ogcproxy.model.ogc.parameters.WFSParameters.*;

import gov.usgs.wqp.ogcproxy.model.ogc.parameters.WFSParameters;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * WFSOperations
 * @author prusso
 *<br /><br />
 *	This enumeration describes all of the standard WFS Operations
 *<br/>
 *	TODO DEAD CODE - This class is not referenced
 */
public enum WFSOperations {
	/**
	 * http://docs.geoserver.org/stable/en/user/services/wfs/reference.html
	 * 
	 */
	GetCapabilities,
	DescribeFeatureType,
	GetFeature,
	LockFeature,
	Transaction,
	GetGMLObject,				// This is for v1.1.0 ONLY
	GetPropertyValue,			// This is for v2.0.0 ONLY
	GetFeatureWithLock,			// This is for v2.0.0 ONLY
	CreateStoredQuery,			// This is for v2.0.0 ONLY
	DropStoredQuery,			// This is for v2.0.0 ONLY
	ListStoredQueries,			// This is for v2.0.0 ONLY
	DescribeStoredQueries,		// This is for v2.0.0 ONLY
	UNKNOWN;
	
	
	public static List<WFSParameters> getParameters(WFSOperations type) {
		
		switch (type) {
	
			case DescribeFeatureType:
				return Arrays.asList(service, version, request, typeNames, exceptions, outputFormat);
	
			case GetFeature:
				return Arrays.asList(service, version, request, typeNames, featureID, 
							count, maxFeatures, sortBy, propertyName, srsName, bbox);
	
			case GetCapabilities:
			case LockFeature:
			case Transaction:	
			case GetGMLObject:
			case CreateStoredQuery:
			case ListStoredQueries:
				return Arrays.asList(service, version, request);
	
			case GetPropertyValue:
				return Arrays.asList(service, version, request, typeNames, valueReference);
	
			case GetFeatureWithLock:
				return Arrays.asList(service, version, request, typeNames, featureID, 
						 count, maxFeatures, sortBy, propertyName, srsName, bbox, expiry);
	
			case DropStoredQuery:
			case DescribeStoredQueries:
				return Arrays.asList(service, storedQuery_Id);
			
			default:
				return new ArrayList<WFSParameters>();
		}
	}
	
	
	public static List<WFSParameters> getRequiredParameters(WFSOperations type) {
		List<WFSParameters> params = WFSOperations.getParameters(type);
		List<WFSParameters> requiredParams = new ArrayList<WFSParameters>();
		
		for(WFSParameters param : params) {
			if (isRequired(param)) {
				requiredParams.add(param);
			}
		}
		
		return requiredParams;
	}
}
