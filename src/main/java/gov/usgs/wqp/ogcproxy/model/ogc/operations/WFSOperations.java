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
	
	public static WFSOperations getTypeFromString(String string) {
		if (string.equals("GetCapabilities")) {
			return GetCapabilities;
		}
		
		if (string.equals("DescribeFeatureType")) {
			return DescribeFeatureType;
		}
		
		if (string.equals("GetFeature")) {
			return GetFeature;
		}
		
		if (string.equals("LockFeature")) {
			return LockFeature;
		}
		
		if (string.equals("Transaction")) {
			return Transaction;
		}
		
		if (string.equals("GetGMLObject")) {
			return GetGMLObject;
		}
		
		if (string.equals("GetPropertyValue")) {
			return GetPropertyValue;
		}
		
		if (string.equals("GetFeatureWithLock")) {
			return GetFeatureWithLock;
		}
		
		if (string.equals("CreateStoredQuery")) {
			return CreateStoredQuery;
		}
		
		if (string.equals("DropStoredQuery")) {
			return DropStoredQuery;
		}
		
		if (string.equals("ListStoredQueries")) {
			return ListStoredQueries;
		}
		
		if (string.equals("DescribeStoredQueries")) {
			return DescribeStoredQueries;
		}

		return UNKNOWN;
	}

	public static String getStringFromType(WFSOperations type) {
		switch (type) {
			case GetCapabilities: {
				return "GetCapabilities";
			}

			case DescribeFeatureType: {
				return "DescribeFeatureType";
			}

			case GetFeature: {
				return "GetFeature";
			}

			case LockFeature: {
				return "LockFeature";
			}

			case Transaction: {
				return "Transaction";
			}

			case GetGMLObject: {
				return "GetGMLObject";
			}

			case GetPropertyValue: {
				return "GetPropertyValue";
			}

			case GetFeatureWithLock: {
				return "GetFeatureWithLock";
			}

			case CreateStoredQuery: {
				return "CreateStoredQuery";
			}

			case DropStoredQuery: {
				return "DropStoredQuery";
			}

			case ListStoredQueries: {
				return "ListStoredQueries";
			}

			case DescribeStoredQueries: {
				return "DescribeStoredQueries";
			}
			
			default: {
				return "UNKNOWN";
			}
		}
	}
	
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
