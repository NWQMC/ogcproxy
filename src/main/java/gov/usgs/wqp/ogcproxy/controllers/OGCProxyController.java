package gov.usgs.wqp.ogcproxy.controllers;

import gov.usgs.wqp.ogcproxy.services.ProxyService;
import gov.usgs.wqp.ogcproxy.services.RESTService;
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
	
	@RequestMapping(value="**/wms", method=RequestMethod.GET)
    public DeferredResult<String> wmsProxy(HttpServletRequest request, HttpServletResponse response, @RequestParam Map<String,String> requestParams) {
		DeferredResult<String> finalResult = new DeferredResult<String>();
		proxyService.performWMSRequest(request, response, requestParams, finalResult);
		return finalResult;
	}
	
	@RequestMapping(value="**/wfs", method=RequestMethod.GET)
    public DeferredResult<String> wfsProxy(HttpServletRequest request, HttpServletResponse response, @RequestParam Map<String,String> requestParams) {
		DeferredResult<String> finalResult = new DeferredResult<String>();
		proxyService.performWFSRequest(request, response, requestParams, finalResult);
		return finalResult;
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
}
