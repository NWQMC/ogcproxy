package gov.usgs.wqp.ogcproxy.utils;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import gov.usgs.wqp.ogcproxy.model.ogc.services.OGCServices;

public class ProxyUtilTest {

	@Test
	public void getRequestedServiceTest() {
		Map<String, String> wfs = new HashMap<>();
		wfs.put(ProxyUtil.OGC_SERVICE_PARAMETER, "WFS");
		Map<String, String> wms = new HashMap<>();
		wms.put(ProxyUtil.OGC_SERVICE_PARAMETER, "WMS");
		assertEquals(OGCServices.WFS, ProxyUtil.getRequestedService(OGCServices.WFS, null));
		assertEquals(OGCServices.WFS, ProxyUtil.getRequestedService(OGCServices.WFS, wfs));
		assertEquals(OGCServices.WMS, ProxyUtil.getRequestedService(OGCServices.WFS, wms));
		assertEquals(OGCServices.WMS, ProxyUtil.getRequestedService(OGCServices.WMS, null));
		assertEquals(OGCServices.WFS, ProxyUtil.getRequestedService(OGCServices.WMS, wfs));
		assertEquals(OGCServices.WMS, ProxyUtil.getRequestedService(OGCServices.WMS, wms));

		//Try with mixed case
		wfs.put(ProxyUtil.OGC_SERVICE_PARAMETER, "WfS");
		wms.put(ProxyUtil.OGC_SERVICE_PARAMETER, "WmS");
		assertEquals(OGCServices.WFS, ProxyUtil.getRequestedService(OGCServices.WFS, wfs));
		assertEquals(OGCServices.WMS, ProxyUtil.getRequestedService(OGCServices.WFS, wms));
		assertEquals(OGCServices.WFS, ProxyUtil.getRequestedService(OGCServices.WMS, wfs));
		assertEquals(OGCServices.WMS, ProxyUtil.getRequestedService(OGCServices.WMS, wms));

		//Try with lower case
		wfs.put(ProxyUtil.OGC_SERVICE_PARAMETER, "wfs");
		wms.put(ProxyUtil.OGC_SERVICE_PARAMETER, "wms");
		assertEquals(OGCServices.WFS, ProxyUtil.getRequestedService(OGCServices.WFS, wfs));
		assertEquals(OGCServices.WMS, ProxyUtil.getRequestedService(OGCServices.WFS, wms));
		assertEquals(OGCServices.WFS, ProxyUtil.getRequestedService(OGCServices.WMS, wfs));
		assertEquals(OGCServices.WMS, ProxyUtil.getRequestedService(OGCServices.WMS, wms));
	}

}
