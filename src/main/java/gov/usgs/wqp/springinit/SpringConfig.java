/*******************************************************************************
 * Project:		ogcproxy
 * Source:		SpringConfig.java
 * Author:		Philip Russo
 ******************************************************************************/

package gov.usgs.wqp.springinit;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.web.servlet.config.annotation.DefaultServletHandlerConfigurer;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import gov.usgs.wqp.ogcproxy.services.ProxyService;
import gov.usgs.wqp.ogcproxy.services.wqp.WQPDynamicLayerCachingService;
import gov.usgs.wqp.ogcproxy.services.wqp.WQPLayerBuildingService;
import gov.usgs.wqp.ogcproxy.utils.CloseableHttpClientFactory;

/**
 * This class takes the place of the old Spring servlet.xml configuration that
 * used to reside in /WEB-INF.
 */

@Configuration
@ComponentScan(basePackages= {"gov.usgs.wqp"})
@EnableWebMvc
@Import(GeoServerConfig.class)
@PropertySources({
	//This will get the defaults
	@PropertySource(value = "classpath:ogcproxy.properties"),
	//This will override with values from the containers file if the file can be found
	@PropertySource(value="file:${catalina.base}/conf/ogcproxy.properties", ignoreResourceNotFound=true)
})
public class SpringConfig extends WebMvcConfigurerAdapter {

	/**
	 * Expose the resources (properties defined above) as an Environment to all
	 * classes.  Must declare a class variable with:
	 *
	 * 		@Autowired
	 *		private Environment environment;
	 */
	@Bean
	public static PropertySourcesPlaceholderConfigurer propertyPlaceHolderConfigurer() {
		return new PropertySourcesPlaceholderConfigurer();
	}

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		/**
		 * Our resources
		 */
		registry.addResourceHandler("/schemas/**").addResourceLocations("/WEB-INF/classes/schemas/");
	}

	/**
	 * The caveat of mapping DispatcherServlet to "/" is that by default it breaks the ability to serve
	 * static resources like images and CSS files. To remedy this, I need to configure Spring MVC to
	 * enable defaultServletHandling.
	 *
	 * 		equivalent for <mvc:default-servlet-handler/> tag
	 *
	 * To do that, my WebappConfig needs to extend WebMvcConfigurerAdapter and override the following method:
	 */
	@Override
	public void configureDefaultServletHandling(DefaultServletHandlerConfigurer configurer) {
		configurer.enable();
	}

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
