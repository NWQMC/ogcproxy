package gov.usgs.wqp.ogcproxy.controllers;

import gov.usgs.wqp.ogcproxy.model.ogc.services.OGCServices;
import gov.usgs.wqp.ogcproxy.model.parser.xml.ogc.OgcWfsParser;
import gov.usgs.wqp.ogcproxy.services.ProxyService;
import gov.usgs.wqp.ogcproxy.services.RESTService;
import gov.usgs.wqp.ogcproxy.utils.ProxyUtil;
import gov.usgs.wqp.ogcproxy.utils.SystemUtils;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class OGCProxyController {
	private static Logger log = SystemUtils.getLogger(OGCProxyController.class);
	
	/*
	 * Beans		===========================================================
	 * ========================================================================
	 */
	@Autowired
	private ProxyService proxyService;
	
	@Autowired
	private RESTService restService;

	@Autowired
	private Environment environment;
	/* ====================================================================== */

	/*
	 * Actions		===========================================================
	 * ========================================================================
	 */
	@RequestMapping(value="/", method=RequestMethod.GET)
    public DeferredResult<ModelAndView> entry() {
		String msg = "WMSProxyController.entry() called";
		log.info(msg);
		
		ModelAndView mv = new ModelAndView("index.jsp");
		mv.addObject("version", environment.getProperty("app.version"));

		DeferredResult<ModelAndView> finalResult = new DeferredResult<ModelAndView>();
		finalResult.setResult(mv);
		return finalResult;
    }
	
	//http://www.waterqualitydata.us/qw_portal_map/ows?service=WPS&version=1.0.0&request=Execute&identifier=gs:SingleWpsStatus
	
	@Async
	@RequestMapping(value="**/wms", method={RequestMethod.GET, RequestMethod.POST})
    public void wmsProxy(HttpServletRequest request, HttpServletResponse response, @RequestParam Map<String,String> requestParams) {
		ProxyUtil.getRequestedService(OGCServices.WMS, requestParams);
		log.info("OGCProxyController.performOGCRequest() INFO - Performing WMS request.");
		proxyService.performWMSRequest(request, response, requestParams);
	}
	
	@Async
	@RequestMapping(value="**/wfs", method=RequestMethod.GET)
    public void wfsProxyGet(HttpServletRequest request, HttpServletResponse response, @RequestParam Map<String,String> requestParams) {
		ProxyUtil.getRequestedService(OGCServices.WFS, requestParams);
		log.info("OGCProxyController.performOGCRequest() INFO - Performing WMS request.");
		proxyService.performWFSRequest(request, response, requestParams);
	}

	// NEW POST OGC XML WMS/WFS - it will check the payload for the actual service
	@Async
	@RequestMapping(value="**/wms", method=RequestMethod.POST)
    public void wmsProxyPost(HttpServletRequest request, HttpServletResponse response) {
		
		OgcWfsParser ogcParser =  new OgcWfsParser(request);
		Map<String, String> requestParams = ogcParser.requestParamsPayloadToMap();
		ProxyUtil.getRequestedService(OGCServices.WMS, requestParams);
		
		proxyService.performPostWMSRequest(request, response, requestParams);
	}
	// NEW POST OGC XML WMS/WFS - it will check the payload for the actual service
	// TODO if this is not fixed then this could just chain to the method above
	@Async
	@RequestMapping(value="**/wfs", method=RequestMethod.POST)
    public void wfsProxyPost(HttpServletRequest request, HttpServletResponse response) {

		OgcWfsParser ogcParser =  new OgcWfsParser(request);
		Map<String, String> requestParams = ogcParser.requestParamsPayloadToMap();
		ProxyUtil.getRequestedService(OGCServices.WFS, requestParams);
		
		proxyService.performPostWFSRequest(request, response, requestParams);
	}
	
	
	
	@RequestMapping(value="/rest/cachestatus/{site}", method=RequestMethod.GET)
    public DeferredResult<ModelAndView> restCacheStatus(@PathVariable String site) {
		DeferredResult<ModelAndView> finalResult = new DeferredResult<ModelAndView>();
		
		restService.checkCacheStatus(site, finalResult);
		
		return finalResult;
	}
	
	@RequestMapping(value="/rest/clearcache/{site}", method=RequestMethod.GET)
    public DeferredResult<ModelAndView> restClearCache(@PathVariable String site) {
		DeferredResult<ModelAndView> finalResult = new DeferredResult<ModelAndView>();
		
		restService.clearCacheBySite(site, finalResult);
		
		return finalResult;
	}
}
