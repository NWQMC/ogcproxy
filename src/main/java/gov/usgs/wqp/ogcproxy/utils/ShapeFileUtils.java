package gov.usgs.wqp.ogcproxy.utils;

import java.io.IOException;
import java.util.List;

import org.apache.log4j.Logger;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.Transaction;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

/**
 * ShapeFileUtils
 * @author prusso
 *<br /><br />
 *	This class exposes many utility methods used in the creation of shapefiles.
 *	The majority of the methods here are statically provided so they can be
 *	exposed and utilized outside of the package this utility resides in.
 */
public class ShapeFileUtils {
	private static Logger log = SystemUtils.getLogger(ShapeFileUtils.class);
	
	public static final String MEDIATYPE_APPLICATION_ZIP = "application/zip";
	
	public static boolean writeToShapeFile(ShapefileDataStore newDataStore, SimpleFeatureType featureType, List<SimpleFeature> features, String path, String filename) {
		return ShapeFileUtils.writeToShapeFile(newDataStore, featureType, features, path, filename, false);
	}
	
	public static boolean writeToShapeFile(ShapefileDataStore newDataStore, SimpleFeatureType featureType, List<SimpleFeature> features, String path, String filename, boolean profile) {
		/*
         * Write the features to the shapefile
         */
		// ==============
		if (profile)
			TimeProfiler.startTimer("GeoTools - Create Transaction time");
        Transaction transaction = new DefaultTransaction("create");
        if (profile)
        	TimeProfiler.endTimer("GeoTools - Create Transaction time", log);
		// ==============
        
        String typeName;
		try {
			typeName = newDataStore.getTypeNames()[0];
		} catch (IOException e) {
			System.out.println(e.getMessage());
			log.error(e.getMessage());
			return false;
		}
		
		// ==============
		if (profile)
			TimeProfiler.startTimer("GeoTools - Create SimpleFeatureSource time");
        SimpleFeatureSource featureSource;
		try {
			featureSource = newDataStore.getFeatureSource(typeName);
		} catch (IOException e) {
			System.out.println(e.getMessage());
			log.error(e.getMessage());
			return false;
		}
		if (profile)
			TimeProfiler.endTimer("GeoTools - Create SimpleFeatureSource time", log);
		// ==============
		
        if (featureSource instanceof SimpleFeatureStore) {
            SimpleFeatureStore featureStore = (SimpleFeatureStore) featureSource;
            /*
             * SimpleFeatureStore has a method to add features from a
             * SimpleFeatureCollection object, so we use the ListFeatureCollection
             * class to wrap our list of features.
             */
            // ==============
            if (profile)
    			TimeProfiler.startTimer("GeoTools - SimpleFeatureCollection Creation time");
            SimpleFeatureCollection collection = new ListFeatureCollection(featureType, features);
            if (profile)
    			TimeProfiler.endTimer("GeoTools - SimpleFeatureCollection Creation time", log);
    		// ==============
    		
            featureStore.setTransaction(transaction);
            try {
            	// ==============
            	if (profile)
        			TimeProfiler.startTimer("GeoTools - SimpleFeatureCollection Population time");
                featureStore.addFeatures(collection);
                if (profile)
        			TimeProfiler.endTimer("GeoTools - SimpleFeatureCollection Population time", log);
        		// ==============
        		
        		// ==============
                if (profile)
        			TimeProfiler.startTimer("GeoTools - Transaction Commit time");
                transaction.commit();
                if (profile)
        			TimeProfiler.endTimer("GeoTools - Transaction Commit time", log);
        		// ==============
            } catch (Exception e) {
            	System.out.println(e.getMessage());
    			log.error(e.getMessage());
                try {
					transaction.rollback();
				} catch (IOException e1) {
					System.out.println(e1.getMessage());
					log.error(e1.getMessage());
				}
            } finally {
                try {
					transaction.close();
				} catch (IOException e) {
					System.out.println(e.getMessage());
					log.error(e.getMessage());
				}
            }
        } else {
            String msg = typeName + " does not support read/write access";
            System.out.println(msg);
			log.error(msg);
			return false;
        }
        
        /*
         * Lets zip up all files created that make up "the shape file"
         */
        // ==============
    	TimeProfiler.startTimer("ZIP Archive - Overall Archive time");
    	SystemUtils.createZipFromFilematch(path, filename);
    	TimeProfiler.endTimer("ZIP Archive - Overall Archive time", log);
		// ==============
		
		return true;
	}
}
