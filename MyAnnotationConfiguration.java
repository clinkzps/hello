package com.newheight.scm.framework.dao.support;

import java.net.URL;

import org.hibernate.HibernateException;
import org.hibernate.cfg.AnnotationConfiguration;

public class MyAnnotationConfiguration extends AnnotationConfiguration {
	private static final long serialVersionUID = 1L;

	public AnnotationConfiguration configure(URL url) throws HibernateException {
		super.configure(url);
		if ("true".equals(System.getProperty("isDev"))) {
			this.setProperty("hibernate.show_sql", "true");
			this.setProperty("hibernate.format_sql", "true");
		}
		return this;
	}
}
