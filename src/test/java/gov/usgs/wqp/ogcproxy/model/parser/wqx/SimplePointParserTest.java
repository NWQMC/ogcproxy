package gov.usgs.wqp.ogcproxy.model.parser.wqx;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.List;

import org.apache.xerces.parsers.SAXParser;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import gov.usgs.wqp.ogcproxy.model.parser.xml.wqx.SimplePointParser;
import gov.usgs.wqp.ogcproxy.model.parser.xml.wqx.handler.SimplePointProviderHandler;

public class SimplePointParserTest {
	
	@Mock
	private SimpleFeatureBuilder featureBuilder;
	
	@Before
	public void init() throws Exception {
		MockitoAnnotations.initMocks(this);
	}
	
	@After
	public void destroy() throws Exception {
		
	}
	
	@Test
	public void testSimplePointParserConstructor() {
		SimplePointParser spp = null;
		
		try {
			spp = new SimplePointParser("testFileName", this.featureBuilder);
		} catch (Exception e) {
			fail("Failed creating SimplePointParser: " + e.getMessage());
		}
		
		assertNotNull(spp.getSaxParser());
		assertThat(spp.getSaxParser(), instanceOf(SAXParser.class));
		
		assertNotNull(spp.getSimplePointFeatures());
		assertThat(spp.getSimplePointFeatures(), instanceOf(List.class));
		
		assertNotNull(spp.getSpHander());
		assertThat(spp.getSpHander(), instanceOf(SimplePointProviderHandler.class));
	}
}
