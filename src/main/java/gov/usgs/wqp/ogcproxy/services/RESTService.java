package gov.usgs.wqp.ogcproxy.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.ModelAndView;

import gov.usgs.wqp.ogcproxy.model.parameters.ProxyDataSourceParameter;
import gov.usgs.wqp.ogcproxy.services.wqp.WQPDynamicLayerCachingService;

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
	
	public ModelAndView checkCacheStatus(String site) {
		ModelAndView mv = new ModelAndView();
		// Depending on the value of the site we will call the correct service.
		if (ProxyDataSourceParameter.getTypeFromString(site) == ProxyDataSourceParameter.WQP_SITES) {
			LOG.info("RESTService.checkCacheStatus() Info: Checking cache status for site [" + site + "]");
			mv.setViewName("wqp_cache_status.jsp");
			mv.addObject("site", "WQP Layer Building Service");
			mv.addObject("cache", layerCachingService.getCacheStore().values());
		} else {
			// Or return an error
			mv.setViewName("invalid_site.jsp");
			mv.addObject("site", site);
		}
		return mv;
	}

	public ModelAndView clearCacheBySite(String site) {
		ModelAndView mv = new ModelAndView();
		// Depending on the value of the site we will call the correct service.
		if (ProxyDataSourceParameter.getTypeFromString(site) == ProxyDataSourceParameter.WQP_SITES) {
			LOG.info("RESTService.clearCacheBySite() Info: Clearing cache for site [" + site + "]");
			mv.setViewName("wqp_cache_cleared.jsp");
			mv.addObject("site", "WQP Layer Building Service");
			mv.addObject("count", layerCachingService.clearCache());
		} else {
			// Or return an error
			mv.setViewName("invalid_site.jsp");
			mv.addObject("site", site);
		}
		return mv;
	}

}
