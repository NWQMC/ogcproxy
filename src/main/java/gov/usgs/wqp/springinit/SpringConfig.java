package gov.usgs.wqp.springinit;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import gov.usgs.wqp.ogcproxy.services.ProxyService;
import gov.usgs.wqp.ogcproxy.services.wqp.WQPDynamicLayerCachingService;
import gov.usgs.wqp.ogcproxy.services.wqp.WQPLayerBuildingService;
import gov.usgs.wqp.ogcproxy.utils.CloseableHttpClientFactory;

@Configuration
@PropertySources({
	//This will get the defaults
	@PropertySource(value = "classpath:ogcproxy.properties"),
	//This will override with values from the containers file if the file can be found
	@PropertySource(value="file:${catalina.base}/conf/ogcproxy.properties", ignoreResourceNotFound=true)
})
public class SpringConfig implements WebMvcConfigurer {

	@Bean
	public InternalResourceViewResolver setupViewResolver() {
		InternalResourceViewResolver resolver = new InternalResourceViewResolver();
		resolver.setPrefix("/WEB-INF/views/");
		resolver.setSuffix("");		// Making this empty so we can explicitly call each view we require (i.e. .jsp and .xml)

		return resolver;
	}

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
