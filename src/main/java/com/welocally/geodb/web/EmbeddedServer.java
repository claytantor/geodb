package com.welocally.geodb.web;

import javax.naming.InitialContext;
import javax.naming.Reference;

import org.apache.log4j.Logger;
import org.eclipse.jetty.plus.jndi.Resource;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.webapp.WebAppContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.context.ContextLoaderListener;

/**
 * This uses the suggestions found on: http://www.smartjava.org/content/embedded-jetty-vaadin-and-weld
 * @author ranjan
 *
 */
public class EmbeddedServer {
	private static final int DEFAULT_HTTP_PORT = 8082;
	private static Logger logger = Logger.getLogger(EmbeddedServer.class);

	  private static String[] __dftConfigurationClasses =
		    {
		        "org.eclipse.jetty.webapp.WebInfConfiguration",
		        "org.eclipse.jetty.webapp.WebXmlConfiguration",
		        "org.eclipse.jetty.webapp.MetaInfConfiguration", 
		        "org.eclipse.jetty.webapp.FragmentConfiguration",        
		        "org.eclipse.jetty.plus.webapp.EnvConfiguration",
		        "org.eclipse.jetty.webapp.JettyWebXmlConfiguration"
		    } ;
		 
		private static String contextPath = "/geodb";
		
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		System.setProperty("java.naming.factory.url","org.eclipse.jetty.jndi");
		System.setProperty("java.naming.factory.initial","org.eclipse.jetty.jndi.InitialContextFactory");
 
		InitialContext ctx = new InitialContext();
		ctx.createSubcontext("java:comp");
 
		Server server = new Server(DEFAULT_HTTP_PORT);
		WebAppContext webapp = new WebAppContext();
		webapp.setConfigurationClasses(__dftConfigurationClasses);
 
		webapp.setContextPath(contextPath);
		String webDir = new ClassPathResource("webapp/").getURI().toString();
		webapp.setResourceBase(webDir);
		webapp.setClassLoader(Thread.currentThread().getContextClassLoader());
 
		server.setHandler(webapp);
		server.start();
 
		new Resource("BeanManager", new Reference("javax.enterprise.inject.spi.BeanMnanager",
				"org.jboss.weld.resources.ManagerObjectFactory", null));
 
		server.join();
	}

}
