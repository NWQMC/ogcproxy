package gov.usgs.wqp.ogcproxy.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.ModelAndView;

import gov.usgs.wqp.ogcproxy.model.parameters.ProxyDataSourceParameter;
import gov.usgs.wqp.ogcproxy.services.wqp.WQPDynamicLayerCachingService;

@Component
public class RESTService {
	private static final Logger LOG = LoggerFactory.getLogger(RESTService.class);

	private WQPDynamicLayerCachingService layerCachingService;

	@Autowired
	public RESTService(WQPDynamicLayerCachingService layerCachingService) {
		this.layerCachingService = layerCachingService;
    }
    
	public ModelAndView checkCacheStatus(String site) {
		ModelAndView mv = new ModelAndView();
		// Depending on the value of the site we will call the correct service.
		if (ProxyDataSourceParameter.getTypeFromString(site) == ProxyDataSourceParameter.WQP_SITES) {
			LOG.trace("Checking cache status for site [" + site + "]");
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

	public boolean clearCacheBySite(String site) {
		// Depending on the value of the site we will call the correct service.
		if (ProxyDataSourceParameter.getTypeFromString(site) == ProxyDataSourceParameter.WQP_SITES) {
			LOG.trace("Clearing cache for site [" + site + "]");
			LOG.info("Clearing cache for site [" + site + "] count:", layerCachingService.clearCache());
			return true;
		} else {
			return false;
		}
	}

}
