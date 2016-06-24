package gov.usgs.wqp.springinit;

import static org.springframework.util.StringUtils.isEmpty;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class LayerBuilderConfig {

	@Autowired
	private Environment environment;

	@Bean
	public String layerbuilderProtocol() {
		return isEmpty(environment.getProperty("wqp.layerbuilder.proto"))
				? "http" : environment.getProperty("wqp.layerbuilder.proto");
	}

	@Bean
	public String layerbuilderHost() {
		return isEmpty(environment.getProperty("wqp.layerbuilder.host"))
				? "localhost" : environment.getProperty("wqp.layerbuilder.host");
	}

	@Bean
	public String layerbuilderPort() {
		return isEmpty(environment.getProperty("wqp.layerbuilder.port"))
				? "8080" : environment.getProperty("wqp.layerbuilder.port");
	}

	@Bean
	public String layerbuilderContext() {
		return isEmpty(environment.getProperty("wqp.layerbuilder.context"))
				? "wqp" : environment.getProperty("wqp.layerbuilder.context");
	}

	@Bean
	public String layerbuilderPath() {
		return isEmpty(environment.getProperty("wqp.layerbuilder.path"))
				? "station/search" : environment.getProperty("wqp.layerbuilder.path");
	}

	@Bean
	public String layerbuilderBaseURI() {
		return layerbuilderProtocol() + "://" + layerbuilderHost() + ":" + layerbuilderPort() + "/" + layerbuilderContext()
			+ "/" + layerbuilderPath();
	}

	@Bean
	public String workingDirectory() {
		return environment.getProperty("layerbuilder.dir.working");
	}

	@Bean
	public String shapefileDirectory() {
		return environment.getProperty("layerbuilder.dir.shapefiles");
	}

}
