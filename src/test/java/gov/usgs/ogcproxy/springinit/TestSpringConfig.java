package gov.usgs.ogcproxy.springinit;

import static org.mockito.Mockito.mock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import gov.usgs.wqp.ogcproxy.services.wqp.WQPDynamicLayerCachingService;

@Configuration
@PropertySource(value = "classpath:ogcproxy.test.properties")
public class TestSpringConfig {

	@Bean
	public WQPDynamicLayerCachingService wqpLayerCachingService() {
		return mock(WQPDynamicLayerCachingService.class);
	};

}
