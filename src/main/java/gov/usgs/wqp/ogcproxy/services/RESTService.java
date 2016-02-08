package gov.usgs.wqp.ogcproxy.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.servlet.ModelAndView;

import gov.usgs.wqp.ogcproxy.model.parameters.ProxyDataSourceParameter;
import gov.usgs.wqp.ogcproxy.services.wqp.WQPDynamicLayerCachingService;
import gov.usgs.wqp.ogcproxy.services.wqp.WQPLayerBuildingService;

public class RESTService {
	private static final Logger LOG = LoggerFactory.getLogger(RESTService.class);
	
	@Autowired
	private WQPDynamicLayerCachingService layerCachingService;
	
	private static final RESTService INSTANCE = new RESTService();

	
	/**
	 * Private Constructor for Singleton Pattern
	 */
    private RESTService() {
    }
    
    /**
     * Singleton accessor
     *
     * @return RESTService instance
     */
	public static RESTService getInstance() {
        return INSTANCE;
    }
	
	public void checkCacheStatus(String site, DeferredResult<ModelAndView> finalResult) {
		ProxyDataSourceParameter siteValue = resolveSite(site);
		
		/*
		 * We can now proceed with the request.  Depending on the value of
		 * the siteValue we will call the correct service.
		 */
		if (siteValue == ProxyDataSourceParameter.WQP_SITES) {
			LOG.info("RESTService.checkCacheStatus() Info: Checking cache status for site [" + siteValue + "]");
			ModelAndView mv = new ModelAndView("wqp_cache_status.jsp");
			
			mv.addObject("site", "WQP Layer Building Service");
			mv.addObject("cache", layerCachingService.getCacheStore().values());

			finalResult.setResult(mv);
		}
		
		/*
		 * Did we find a legitimate site value or do we need to return an error?
		 */
		ModelAndView mv = new ModelAndView("invalid_site.jsp");
		finalResult.setResult(mv);
	}

	/**
	 * The proxy can handle any number of "sites".  Sites are determined by
	 * the "layers" parameter when a WMS call comes in and depending on that
	 * value, the appropriate service is invoked.  We will use the layer
	 * value as the site determination value here and invoke the correct
	 * service for status.
	 */
	public ProxyDataSourceParameter resolveSite(String site) {
		ProxyDataSourceParameter siteValue = ProxyDataSourceParameter.UNKNOWN;
		if ( ! StringUtils.isEmpty(site) ) {
			siteValue = ProxyDataSourceParameter.getTypeFromString(site);
		}
		return siteValue;
	}
	
	
	public void clearCacheBySite(String site, DeferredResult<ModelAndView> finalResult) {
		ProxyDataSourceParameter siteValue = resolveSite(site);
		
		/*
		 * We can now proceed with the request.  Depending on the value of
		 * the siteValue we will call the correct service.
		 */
		if (siteValue == ProxyDataSourceParameter.WQP_SITES) {
			LOG.info("RESTService.clearCacheBySite() Info: Clearing cache for site [" + siteValue + "]");
			
			ModelAndView mv = new ModelAndView("wqp_cache_cleared.jsp");
			
			mv.addObject("site", "WQP Layer Building Service");
			mv.addObject("count", layerCachingService.clearCache());
			
			finalResult.setResult(mv);
			return;
		}
		/*
		 * Did we find a legitimate site value or do we need to return an error?
		 */
		ModelAndView mv = new ModelAndView("invalid_site.jsp");
		mv.addObject("site", site);
		finalResult.setResult(mv);
	}
}
