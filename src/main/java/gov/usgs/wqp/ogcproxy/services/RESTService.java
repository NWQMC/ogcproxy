package gov.usgs.wqp.ogcproxy.services;

import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import gov.usgs.wqp.ogcproxy.model.parameters.ProxyDataSourceParameter;
import gov.usgs.wqp.ogcproxy.services.wqp.WQPDynamicLayerCachingService;
import java.util.HashMap;
import java.util.Map;

@Component
public class RESTService {
	private static final Logger LOG = LoggerFactory.getLogger(RESTService.class);

	private WQPDynamicLayerCachingService layerCachingService;

	@Autowired
	public RESTService(WQPDynamicLayerCachingService layerCachingService) {
		this.layerCachingService = layerCachingService;
	}

	public Object checkCacheStatus(String site) {
		Map<String, Object> mv = new HashMap<>();
		// Depending on the value of the site we will call the correct service.
		if (ProxyDataSourceParameter.getTypeFromString(site) == ProxyDataSourceParameter.WQP_SITES) {
			LOG.trace("Checking cache status for site [" + site + "]");
			mv.put("site", "WQP Layer Building Service");
			try {
				mv.put("cache", layerCachingService.getCacheValues());
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ExecutionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			// Or return an error
			// TODO: mv.setViewName("invalid_site.jsp");
			mv.put("site", site);
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
