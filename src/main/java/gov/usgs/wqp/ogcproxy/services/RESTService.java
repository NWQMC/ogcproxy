package gov.usgs.wqp.ogcproxy.services;

import gov.usgs.wqp.ogcproxy.model.parameters.ProxyDataSourceParameter;
import gov.usgs.wqp.ogcproxy.services.wqp.WQPLayerBuildingService;
import gov.usgs.wqp.ogcproxy.utils.SystemUtils;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.servlet.ModelAndView;

@Service
public class RESTService {
	private static Logger log = SystemUtils.getLogger(RESTService.class);
	
	/*
	 * Beans		===========================================================
	 * ========================================================================
	 */
	@Autowired
	private Environment environment;
	
	@Autowired
	private WQPLayerBuildingService wqpLayerBuildingService;
	/* ====================================================================== */
	
	/*
	 * Static Local		=======================================================
	 * ========================================================================
	 */
	/* ====================================================================== */
	private static boolean initialized = false;
	/* ====================================================================== */
	
	/*
	 * INSTANCE		===========================================================
	 * ========================================================================
	 */
	/* ====================================================================== */
	private static final RESTService INSTANCE = new RESTService();
	/* ====================================================================== */
	
	/**
	 * Private Constructor for Singleton Pattern
	 */
    private RESTService() {
    	initialized = false;
    }
    
    /**
     * Singleton accessor
     * 
     * @return RESTService instance
     */
	public static RESTService getInstance() {
        return INSTANCE;
    }
	
	public void initialize() {
		/**
		 * Since we are using Spring DI we cannot access the environment bean 
		 * in the constructor.  We'll just use a locked initialized variable
		 * to check initialization after instantiation and set the env
		 * properties here.
		 */
		if (!initialized) {
			synchronized(RESTService.class) {
				if (!initialized) {
					initialized = true;
				}
			}
		}
	}
	
	public void checkCacheStatus(String site, DeferredResult<ModelAndView> finalResult) {
		/**
		 * The proxy can handle any number of "sites".  Sites are determined by
		 * the "layers" parameter when a WMS call comes in and depending on that
		 * value, the appropriate service is invoked.  We will use the layer 
		 * value as the site determination value here and invoke the correct
		 * service for status.
		 */
		boolean siteFound = false;
		ProxyDataSourceParameter siteValue = ProxyDataSourceParameter.UNKNOWN;
		if ((site != null) && (!site.equals(""))) {
			siteValue = ProxyDataSourceParameter.getTypeFromString(site);
			
			if (siteValue != ProxyDataSourceParameter.UNKNOWN) {
				siteFound = true;
			}
		}
		
		/**
		 * Did we find a legitimate site value or do we need to return an error?
		 */
		if (!siteFound) {
			ModelAndView mv = new ModelAndView("invalid_site.jsp");
			finalResult.setResult(mv);
			return;
		}
		
		/**
		 * We can now proceed with the request.  Depending on the value of
		 * the siteValue we will call the correct service.
		 */
		log.info("RESTService.checkCacheStatus() Info: Checking cache status for site [" + siteValue + "]");
		switch(siteValue) {
			case WQP_SITES: {
				finalResult.setResult(wqpLayerBuildingService.getCacheStatus());				
				break;
			}
			default: {
				break;
			}
		}
	}
	
	public void clearCacheBySite(String site, DeferredResult<ModelAndView> finalResult) {
		/**
		 * The proxy can handle any number of "sites".  Sites are determined by
		 * the "layers" parameter when a WMS call comes in and depending on that
		 * value, the appropriate service is invoked.  We will use the layer 
		 * value as the site determination value here and invoke the correct
		 * service for status.
		 */
		boolean siteFound = false;
		ProxyDataSourceParameter siteValue = ProxyDataSourceParameter.UNKNOWN;
		if ((site != null) && (!site.equals(""))) {
			siteValue = ProxyDataSourceParameter.getTypeFromString(site);
			
			if (siteValue != ProxyDataSourceParameter.UNKNOWN) {
				siteFound = true;
			}
		}
		
		/**
		 * Did we find a legitimate site value or do we need to return an error?
		 */
		if (!siteFound) {
			ModelAndView mv = new ModelAndView("invalid_site.jsp");
			mv.addObject("site", site);
			finalResult.setResult(mv);
			return;
		}
		
		/**
		 * We can now proceed with the request.  Depending on the value of
		 * the siteValue we will call the correct service.
		 */
		log.info("RESTService.clearCacheBySite() Info: Clearing cache for site [" + siteValue + "]");
		switch(siteValue) {
			case WQP_SITES: {
				finalResult.setResult(wqpLayerBuildingService.clearCache());				
				break;
			}
			default: {
				break;
			}
		}
	}
}
