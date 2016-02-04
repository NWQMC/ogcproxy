package gov.usgs.wqp.ogcproxy.controllers;

import java.util.Map;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.servlet.ModelAndView;

import gov.usgs.wqp.ogcproxy.model.ogc.services.OGCServices;
import gov.usgs.wqp.ogcproxy.model.parser.xml.ogc.OgcWfsParser;
import gov.usgs.wqp.ogcproxy.model.parser.xml.ogc.RequestWrapper;
import gov.usgs.wqp.ogcproxy.services.ProxyService;
import gov.usgs.wqp.ogcproxy.services.RESTService;
import gov.usgs.wqp.ogcproxy.utils.ApplicationVersion;

@Controller
public class OGCProxyController {
	private static final Logger LOG = LoggerFactory.getLogger(OGCProxyController.class);
	
	@Autowired
	private ProxyService proxyService;
	
	@Autowired
	private RESTService restService;

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
	 * @param requestParams
	 * @return
	 */
	@RequestMapping(value="**/wms", method={RequestMethod.GET})
    public DeferredResult<String>  wmsProxy(HttpServletRequest request, HttpServletResponse response, @RequestParam Map<String,String> requestParams) {
		LOG.debug("OGCProxyController.wmsProxy() INFO - Performing request.");
		return proxyService.performRequest(request, response, requestParams, OGCServices.WMS);
	}
	
	/** 
	 * WFS Get endpoint.
	 * May actually contain a WMS or WFS call - the "SERVICE" parameter is used to determine the actual service being called.
	 * 
	 * @param request
	 * @param response
	 * @param requestParams
	 * @return
	 */
	@RequestMapping(value="**/wfs", method=RequestMethod.GET)
    public DeferredResult<String> wfsProxyGet(HttpServletRequest request, HttpServletResponse response, @RequestParam Map<String,String> requestParams) {
		LOG.debug("OGCProxyController.wfsProxy() INFO - Performing request.");
		return proxyService.performRequest(request, response, requestParams, OGCServices.WFS);
	}

	/** 
	 * Parses the POST'd document into a map of parameters before continuing on with the request processing.
	 * 
	 * @param request
	 * @param response
	 * @param ogcService
	 */
	public void handleServiceCallPost(HttpServletRequest request, HttpServletResponse response, OGCServices ogcService) {
		OgcWfsParser ogcParser =  new OgcWfsParser(request);
		Map<String, String> requestParams = ogcParser.requestParamsPayloadToMap();
		request = new RequestWrapper(request, ogcParser.getBodyMinusSearchParams());
		
		proxyService.performRequest(request, response, requestParams, ogcService);
	}

	
	/** 
	 * WMS Post endpoint.
	 * May actually contain a WMS or WFS call - the "SERVICE" parameter is used to determine the actual service being called.
	 * 
	 * @param request
	 * @param response
	 */
	@Async
	@RequestMapping(value="**/wms", method=RequestMethod.POST)
    public void wmsProxyPost(HttpServletRequest request, HttpServletResponse response) {
		LOG.debug("OGCProxyController.wmsProxyPost() INFO - Performing request.");
		handleServiceCallPost(request, response, OGCServices.WMS);
	}

	/** 
	 * WFS Post endpoint.
	 * May actually contain a WMS or WFS call - the "SERVICE" parameter is used to determine the actual service being called.
	 * 
	 * @param request
	 * @param response
	 */
	@Async
	@RequestMapping(value="**/wfs", method=RequestMethod.POST)
    public void wfsProxyPost(HttpServletRequest request, HttpServletResponse response) {
		LOG.debug("OGCProxyController.wfsProxyPost() INFO - Performing request.");
		handleServiceCallPost(request, response, OGCServices.WFS);
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
		
		restService.checkCacheStatus(site, finalResult);
		
		return finalResult;
	}
	
	/** 
	 * Clear the specified DataSource's cache.
	 * @param site The cache DataSource to clear.
	 * @return The cache clear report.
	 */
	//TODO this should really be a DELETE
	@RequestMapping(value="/rest/clearcache/{site}", method=RequestMethod.GET)
    public DeferredResult<ModelAndView> restClearCache(@PathVariable String site) {
		DeferredResult<ModelAndView> finalResult = new DeferredResult<ModelAndView>();
		
		restService.clearCacheBySite(site, finalResult);
		
		return finalResult;
	}

}
