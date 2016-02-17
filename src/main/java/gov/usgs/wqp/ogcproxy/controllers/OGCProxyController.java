package gov.usgs.wqp.ogcproxy.controllers;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.servlet.ModelAndView;

import gov.usgs.wqp.ogcproxy.model.ogc.services.OGCServices;
import gov.usgs.wqp.ogcproxy.services.ProxyService;
import gov.usgs.wqp.ogcproxy.services.RESTService;
import gov.usgs.wqp.ogcproxy.utils.ApplicationVersion;

@Controller
public class OGCProxyController {
	private static final Logger LOG = LoggerFactory.getLogger(OGCProxyController.class);
	
	private ProxyService proxyService;
	private RESTService restService;

	@Autowired
	public OGCProxyController(ProxyService proxyService,
			RESTService restService) {
		this.proxyService = proxyService;
		this.restService = restService;
	}

	/** 
	 * Root context of the Application
	 * 
	 * @return The splash page of the application.
	 */
	@RequestMapping(value="/", method=RequestMethod.GET)
    public ModelAndView entry() {
		LOG.debug("OGCProxyController.entry() called");
		
		ModelAndView mv = new ModelAndView("index.jsp");
		mv.addObject("version", ApplicationVersion.getVersion());

		return mv;
    }
	
	/** 
	 * WMS Get endpoint.
	 * May actually contain a WMS or WFS call - the "SERVICE" parameter is used to determine the actual service being called.
	 * 
	 * @param request
	 * @param response
	 */
	@RequestMapping(value="/wms", method={RequestMethod.GET})
    public void wmsProxyGet(HttpServletRequest request, HttpServletResponse response) {
		LOG.debug("OGCProxyController.wmsProxy() INFO - Performing request.");
		proxyService.performRequest(request, response, OGCServices.WMS);
	}
	
	/** 
	 * WFS Get endpoint.
	 * May actually contain a WMS or WFS call - the "SERVICE" parameter is used to determine the actual service being called.
	 * 
	 * @param request
	 * @param response
	 */
	@RequestMapping(value="/wfs", method=RequestMethod.GET)
    public void wfsProxyGet(HttpServletRequest request, HttpServletResponse response) {
		LOG.debug("OGCProxyController.wfsProxy() INFO - Performing request.");
		proxyService.performRequest(request, response, OGCServices.WFS);
	}

	/** 
	 * WMS Post endpoint.
	 * May actually contain a WMS or WFS call - the "SERVICE" parameter is used to determine the actual service being called.
	 * 
	 * @param request
	 * @param response
	 */
	@Async
	@RequestMapping(value="/wms", method=RequestMethod.POST)
    public void wmsProxyPost(HttpServletRequest request, HttpServletResponse response) {
		LOG.debug("OGCProxyController.wmsProxyPost() INFO - Performing request.");
		proxyService.performRequest(request, response, OGCServices.WMS);
	}

	/** 
	 * WFS Post endpoint.
	 * May actually contain a WMS or WFS call - the "SERVICE" parameter is used to determine the actual service being called.
	 * 
	 * @param request
	 * @param response
	 */
	@Async
	@RequestMapping(value="/wfs", method=RequestMethod.POST)
    public void wfsProxyPost(HttpServletRequest request, HttpServletResponse response) {
		LOG.debug("OGCProxyController.wfsProxyPost() INFO - Performing request.");
		proxyService.performRequest(request, response, OGCServices.WFS);
	}
	
	/** 
	 * Get the current status of the OGCProxy cache object.
	 * 
	 * @param site The cache DataSource to display content from. 
	 * @return The cache status report.
	 */
	@RequestMapping(value="/rest/cachestatus/{site}", method=RequestMethod.GET)
    public DeferredResult<ModelAndView> restCacheStatus(@PathVariable String site) {
		DeferredResult<ModelAndView> finalResult = new DeferredResult<ModelAndView>();
		
		finalResult.setResult(restService.checkCacheStatus(site));
		
		return finalResult;
	}
	
	/** 
	 * Clear the specified DataSource's cache.
	 * @param site The cache DataSource to clear.
	 * @return The cache clear report.
	 */
	@RequestMapping(value="/rest/clearcache/{site}", method=RequestMethod.DELETE)
    public DeferredResult<ModelAndView> restClearCache(@PathVariable String site) {
		DeferredResult<ModelAndView> finalResult = new DeferredResult<ModelAndView>();
		
		finalResult.setResult(restService.clearCacheBySite(site));
		
		return finalResult;
	}

}
