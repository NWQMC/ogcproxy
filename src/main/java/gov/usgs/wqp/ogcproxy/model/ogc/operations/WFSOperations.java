package gov.usgs.wqp.ogcproxy.model.ogc.operations;

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
			case GetCapabilities: {
				return Arrays.asList(WFSParameters.service, WFSParameters.version, WFSParameters.request);
			}
	
			case DescribeFeatureType: {
				return Arrays.asList(WFSParameters.service, WFSParameters.version, WFSParameters.request, WFSParameters.typeNames, WFSParameters.exceptions, 
									 WFSParameters.outputFormat);
			}
	
			case GetFeature: {
				return Arrays.asList(WFSParameters.service, WFSParameters.version, WFSParameters.request, WFSParameters.typeNames, WFSParameters.featureID, 
									 WFSParameters.count, WFSParameters.maxFeatures, WFSParameters.sortBy, WFSParameters.propertyName, WFSParameters.srsName,
									 WFSParameters.bbox);
			}
	
			case LockFeature: {
				return Arrays.asList(WFSParameters.service, WFSParameters.version, WFSParameters.request);
			}
	
			case Transaction: {
				return Arrays.asList(WFSParameters.service, WFSParameters.version, WFSParameters.request);
			}
	
			case GetGMLObject: {
				return Arrays.asList(WFSParameters.service, WFSParameters.version, WFSParameters.request);
			}
	
			case GetPropertyValue: {
				return Arrays.asList(WFSParameters.service, WFSParameters.version, WFSParameters.request, WFSParameters.typeNames, WFSParameters.valueReference);
			}
	
			case GetFeatureWithLock: {
				return Arrays.asList(WFSParameters.service, WFSParameters.version, WFSParameters.request, WFSParameters.typeNames, WFSParameters.featureID, 
						 			 WFSParameters.count, WFSParameters.maxFeatures, WFSParameters.sortBy, WFSParameters.propertyName, WFSParameters.srsName,
						 			 WFSParameters.bbox, WFSParameters.expiry);
			}
	
			case CreateStoredQuery: {
				return Arrays.asList(WFSParameters.service, WFSParameters.version, WFSParameters.request);
			}
	
			case DropStoredQuery: {
				return Arrays.asList(WFSParameters.service, WFSParameters.storedQuery_Id);
			}
	
			case ListStoredQueries: {
				return Arrays.asList(WFSParameters.service, WFSParameters.version, WFSParameters.request);
			}
	
			case DescribeStoredQueries: {
				return Arrays.asList(WFSParameters.service, WFSParameters.storedQuery_Id);
			}
			
			default: {
				return new ArrayList<WFSParameters>();
			}
		}
	}
	
	public static List<WFSParameters> getRequiredParameters(WFSOperations type) {
		List<WFSParameters> params = WFSOperations.getParameters(type);
		List<WFSParameters> requiredParams = new ArrayList<WFSParameters>();
		
		for(WFSParameters param : params) {
			if(WFSParameters.isRequired(param)) {
				requiredParams.add(param);
			}
		}
		
		return requiredParams;
	}
}
