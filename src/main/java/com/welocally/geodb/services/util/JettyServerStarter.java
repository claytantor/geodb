package com.welocally.geodb.services.util;

import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import com.welocally.geodb.services.app.CommandException;
import com.welocally.geodb.services.app.CommandSupport;

@Component
public class JettyServerStarter implements CommandSupport, ApplicationContextAware {
	
	static Logger logger = 
		Logger.getLogger(JettyServerStarter.class);
	
	ApplicationContext applicationContext;
	
	@Override
    public void setApplicationContext(ApplicationContext applicationContext)
            throws BeansException {
	    this.applicationContext = applicationContext;
    }

	@Override
    public void doCommand(JSONObject command) throws CommandException {

//		try {
//	        ((AbstractApplicationContext)applicationContext).registerShutdownHook();
//
//	        Server server = (Server) applicationContext.getBean("jettyServer");  
//
//	        ServletContext servletContext = null;
//
//	        for (Handler handler : server.getHandlers()) {
//	            if (handler instanceof Context) {
//	                Context context = (Context) handler;
//	                
//	                servletContext = context.getServletContext();
//	            }
//	        }
//
//	        XmlWebApplicationContext wctx = new XmlWebApplicationContext();  
//	        wctx.setParent(applicationContext);
//	        wctx.setConfigLocation("");
//	        wctx.setServletContext(servletContext);
//	        wctx.refresh();
//
//	        servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, wctx);  
//
//	        server.start();
//        } catch (BeansException e) {
//	       logger.error("problem with context",e);
//	       throw new CommandException(e.getMessage());
//        } catch (IllegalStateException e) {
//        	logger.error("problem with state",e);
//  	       throw new CommandException(e.getMessage());
//        } catch (Exception e) {
//        	 logger.error("general problem",e);
//  	       throw new CommandException(e.getMessage());
//        }
    }


}
