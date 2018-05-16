package gov.usgs.wqp.ogcproxy.utils;

import java.util.Properties;

import javax.servlet.ServletContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.ServletContextAware;

@Component
public class ApplicationVersion implements ServletContextAware {

	private static final Logger LOG = LoggerFactory.getLogger(ApplicationVersion.class);

	private static ServletContext servletContext;

	public static final String PROJECT_VERSION = "Implementation-Version";

	public static String getVersion() {
		StringBuilder currentVersion = new StringBuilder("v");
		try {
			String name = "/META-INF/MANIFEST.MF";
			Properties props = new Properties();
			props.load(servletContext.getResourceAsStream(name));
			String projectVersion = (String) props.get(PROJECT_VERSION);
			if (null == projectVersion) {
				currentVersion.append("Unavailable");
			} else {
				currentVersion.append(projectVersion);
			}
		} catch (Exception e) {
			LOG.info("unable to get application version", e);
			currentVersion.append(" Error Encountered");
		}
		return currentVersion.toString();
	}

	public void setServletContext(final ServletContext inServletContext) {
		servletContext = inServletContext;
	}

}
