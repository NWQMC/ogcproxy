package gov.usgs.wqp.ogcproxy.utils;

import java.io.IOException;
import java.util.List;

import org.geotools.data.DefaultTransaction;
import org.geotools.data.Transaction;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ShapeFileUtils
 * @author prusso
 *<br /><br />
 *	This class exposes many utility methods used in the creation of shapefiles.
 *	The majority of the methods here are statically provided so they can be
 *	exposed and utilized outside of the package this utility resides in.
 */
public class ShapeFileUtils {
	private static final Logger LOG = LoggerFactory.getLogger(ShapeFileUtils.class);
	
	public static final String MEDIATYPE_APPLICATION_ZIP = "application/zip";
	
	public static boolean writeToShapeFile(ShapefileDataStore newDataStore, SimpleFeatureType featureType, List<SimpleFeature> features, String path, String filename) {
		/*
         * Write the features to the shapefile
         */
        try (Transaction transaction = new DefaultTransaction("create")) {
        
        	String typeName = newDataStore.getTypeNames()[0];
        	SimpleFeatureSource featureSource = newDataStore.getFeatureSource(typeName);
		
	        if (featureSource instanceof SimpleFeatureStore) {
	            SimpleFeatureStore featureStore = (SimpleFeatureStore) featureSource;
	            /*
	             * SimpleFeatureStore has a method to add features from a
	             * SimpleFeatureCollection object, so we use the ListFeatureCollection
	             * class to wrap our list of features.
	             */
	            SimpleFeatureCollection collection = new ListFeatureCollection(featureType, features);
	    		
	            featureStore.setTransaction(transaction);
	            try {
	            	featureStore.addFeatures(collection);
	            } catch (IOException e) {
	            	transaction.rollback();
	            	throw e;
	            }
                transaction.commit();

                /*
                 * Lets zip up all files created that make up "the shape file"
                 */
            	SystemUtils.createZipFromFilematch(path, filename);
	        } else {
	            String msg = typeName + " does not support read/write access";
	            System.out.println(msg);
				LOG.error(msg);
				return false;
	        }

        } catch (IOException e) {
        	System.out.println(e.getMessage());
        	LOG.error(e.getMessage());
        	return false;
        }
        
		return true;
	}

}
