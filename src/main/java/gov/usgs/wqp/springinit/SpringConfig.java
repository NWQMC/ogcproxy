package gov.usgs.wqp.springinit;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import gov.usgs.wqp.ogcproxy.services.ProxyService;
import gov.usgs.wqp.ogcproxy.services.wqp.WQPDynamicLayerCachingService;
import gov.usgs.wqp.ogcproxy.services.wqp.WQPLayerBuildingService;
import gov.usgs.wqp.ogcproxy.utils.CloseableHttpClientFactory;

@Configuration
public class SpringConfig implements WebMvcConfigurer {

	@Bean
	public WQPDynamicLayerCachingService wqpLayerCachingService() {
		return WQPDynamicLayerCachingService.getInstance();
	}

	@Bean
	public WQPLayerBuildingService wqpLayerBuildingService() {
		return WQPLayerBuildingService.getInstance();
	}

	@Bean
	public ProxyService proxyService() {
		return ProxyService.getInstance();
	}

	@Bean
	public CloseableHttpClientFactory closeableHttpClientFactory() {
		return CloseableHttpClientFactory.getInstance();
	}

}
