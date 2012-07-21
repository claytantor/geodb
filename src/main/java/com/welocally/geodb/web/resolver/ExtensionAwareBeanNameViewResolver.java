package com.welocally.geodb.web.resolver;

import java.util.Locale;

import org.apache.log4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.view.BeanNameViewResolver;


public class ExtensionAwareBeanNameViewResolver extends BeanNameViewResolver
		implements ViewResolver, Ordered {

	static Logger logger = Logger
			.getLogger(ExtensionAwareBeanNameViewResolver.class);

	private String extension;

	public View resolveViewName(String viewName, Locale locale)
			throws BeansException {

		//logger.debug("extension:" + extension + " view:" + viewName);

		ApplicationContext context = getApplicationContext();

		String fullViewName = viewName + "-" + extension;
		//logger.debug("looking for view bean:" + fullViewName);
		if (!context.containsBean(fullViewName)) {
			// Allow for ViewResolver chaining.
			return null;
		}
		View v = (View) context.getBean(fullViewName, View.class);
		return v;
	}

	public void setExtension(String extension) {
		this.extension = extension;
	}

}