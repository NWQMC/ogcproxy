package gov.usgs.wqp.ogcproxy.controllers;

import gov.usgs.wqp.ogcproxy.model.ogc.services.OGCServices;
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
	
	@RequestMapping(value="**/wms", method={RequestMethod.GET, RequestMethod.POST})
    public DeferredResult<String> wmsProxy(HttpServletRequest request, HttpServletResponse response, @RequestParam Map<String,String> requestParams) {
		return performOGCRequest(request, response, requestParams, OGCServices.WMS);
	}
	
	@RequestMapping(value="**/wfs", method={RequestMethod.GET, RequestMethod.POST})
    public DeferredResult<String> wfsProxy(HttpServletRequest request, HttpServletResponse response, @RequestParam Map<String,String> requestParams) {
		return performOGCRequest(request, response, requestParams, OGCServices.WFS);
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
	/* ====================================================================== */
	
	/*
	 * OGC Request Actions ====================================================
	 * ========================================================================
	 */
	private DeferredResult<String> performOGCRequest(HttpServletRequest request, HttpServletResponse response, Map<String,String> requestParams, OGCServices calledService) {
		DeferredResult<String> finalResult = new DeferredResult<String>();
		
		OGCServices requestedService = ProxyUtil.getFinalRequestedService(requestParams, calledService);
		switch(requestedService) {
			case WMS: {
				log.info("OGCProxyController.performOGCRequest() INFO - Performing WMS request.");
				proxyService.performWMSRequest(request, response, requestParams, finalResult);
				break;
			}
			
			case WFS: {
				log.info("OGCProxyController.performOGCRequest() INFO - Performing WFS request.");
				proxyService.performWFSRequest(request, response, requestParams, finalResult);
				break;
			}
			
			default: {
				log.warn("OGCProxyController.performOGCRequest() WARNING - Unknown OGC Service Requested.");
				finalResult.setResult("Unknown OGC Service Requested");
				break;
			}
		}
		
		return finalResult;
	}

	/* ====================================================================== */
}
