/*******************************************************************************
 * Project:		ogcproxy
 * Source:		SpringInitializer.java
 * Author:		Philip Russo
 ******************************************************************************/

package gov.usgs.wqp.springinit;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration.Dynamic;

import org.springframework.web.WebApplicationInitializer;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

public class SpringInitializer implements WebApplicationInitializer {
	// gets invoked automatically when application starts up
	public void onStartup(ServletContext servletContext)
			throws ServletException {
		// Create ApplicationContext. I'm using the
		// AnnotationConfigWebApplicationContext to avoid using beans xml files.
		AnnotationConfigWebApplicationContext ctx = new AnnotationConfigWebApplicationContext();
		ctx.register(SpringConfig.class);

		// Add the servlet mapping manually and make it initialize automatically
		// 		Need to include the following for class Dynamic
		/*
			<dependency>
				<groupId>javax.servlet</groupId>
				<artifactId>javax.servlet-api</artifactId>
				<version>3.1.0</version>
			</dependency>
		 */
		Dynamic servlet = servletContext.addServlet("dispatcher", new DispatcherServlet(ctx));
		servlet.addMapping("/");
		servlet.setAsyncSupported(true);
		servlet.setLoadOnStartup(1);
	}
}
