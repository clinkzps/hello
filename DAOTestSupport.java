package com.newheight.scm.framework.dao.support;

import javax.sql.DataSource;

import junit.framework.Assert;

import org.apache.commons.lang.StringUtils;
import org.hibernate.SessionFactory;
import org.junit.After;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.orm.hibernate3.HibernateTransactionManager;
import org.springframework.transaction.TransactionStatus;

public class DAOTestSupport {
	private static ClassPathXmlApplicationContext context;
	
	private static Logger logger = LoggerFactory.getLogger(DAOTestSupport.class);
	
	public static final String PMS_TX = "sessionFactory";
	public static final String MISC_TX = "miscDataTransactionManager";
	public static final String PROD_TX = "prodDataTransactionManager";
	
	private HibernateTransactionManager txManager;
	private TransactionStatus ts;
	private boolean rollback = false;
	
	private boolean isInit = false;
	
	protected final synchronized void initTx() {
		if(isInit) {
			return;
		}
		isInit = true;
		TestTx txConfig = getClass().getAnnotation(TestTx.class);
		if(txConfig != null) {
			if(! txConfig.noTx()) {
				Assert.assertTrue("除非对注解TestTx设置了noTx=true，否则必须指定事务管理器的名称", StringUtils.isNotBlank(txConfig.name()));
				txManager = getTx(txConfig.name());
				Assert.assertNotNull("无法找到事务管理器["+txConfig.name()+"]", txManager);
				rollback = txConfig.rollback();
			}
		} else {
			throw new Error("必须给测试类通过TestTx注解指定事务管理器");
		}
	}
	
	@Before
	public final void before() {
		initTx();
		if(txManager != null) {
			try {
				ts = txManager.getTransaction(null);
				logger.debug("start tx");
			} catch (Exception e) {
				logger.error("start tx error");
				throw new RuntimeException(e);
			}
		}
	}
	
	@After
	public void after() {
		if(txManager == null) {
			return;
		}
		if(rollback) {
			try {
				txManager.rollback(ts);
				logger.debug("rollback tx");
			} catch (Exception e) {
				logger.error("rollback tx error");
				throw new RuntimeException(e);
			}
		} else {
			try {
				txManager.commit(ts);
				logger.debug("commit tx");
			} catch (Exception e) {
				logger.error("commit tx error");
				throw new RuntimeException(e);
			}
		}
	}
	
	public static <T> T getDAO(Class<T> clazz) {
		try {
			T dao = clazz.newInstance();
			if(dao instanceof HibernateBaseDAO) {
				SessionFactory sf = getSessionFactoryByDAOClazz(clazz);
				((HibernateBaseDAO<?>)dao).setSessionFactory(sf);
			}
			return dao;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	protected static SessionFactory getSessionFactoryByDAOClazz(Class<?> clazz) {
		if(ProdDataBaseDAO.class.isAssignableFrom(clazz)) {
			return getProdDataSource();
		}
		if(MsicDataBaseDAO.class.isAssignableFrom(clazz)) {
			return getMiscDataSource();
		}
		if(PoBaseDAO.class.isAssignableFrom(clazz)) {
			return getPmsDataSource();
		}
		return null;
	}

	protected static synchronized SessionFactory getPmsDataSource() {
		init(PMS_TX, "spring-dao-pms.xml");
		return context.getBean(PMS_TX, SessionFactory.class);
	}
	
	protected static synchronized HibernateTransactionManager getPmsTx() {
		init("transactionManager", "spring-dao-pms.xml");
		return context.getBean("transactionManager", HibernateTransactionManager.class);
	}
	
	protected static synchronized HibernateTransactionManager getMiscTx() {
		init(MISC_TX, "spring-dao-misc.xml");
		return context.getBean(MISC_TX, HibernateTransactionManager.class);
	}

	protected static synchronized SessionFactory getMiscDataSource() {
		init("miscDataSessionFactory", "spring-dao-misc.xml");
		return context.getBean("miscDataSessionFactory", SessionFactory.class);
	}

	protected static synchronized SessionFactory getProdDataSource() {
		init("prodDataSessionFactory", "spring-dao-prod.xml");
		return context.getBean("prodDataSessionFactory", SessionFactory.class);
	}
	
	protected static synchronized HibernateTransactionManager getProdTx() {
		init(PROD_TX, "spring-dao-prod.xml");
		return context.getBean(PROD_TX, HibernateTransactionManager.class);
	}
	
	protected static synchronized HibernateTransactionManager getTx(String txBeanName) {
		if(PMS_TX.equals(txBeanName)) {
			return getPmsTx();
		}
		if(MISC_TX.equals(txBeanName)) {
			return getMiscTx();
		}
		
		if(PROD_TX.equals(txBeanName)) {
			return getProdTx();
		}
		return null;
	}
	
	protected static synchronized void init(String checkBeanName, String daoConfigFile) {
		if(context == null) {
			context = new ClassPathXmlApplicationContext("spring-property.xml", daoConfigFile);
		} else if(! context.containsBean(checkBeanName)){
			context = new ClassPathXmlApplicationContext(new String[]{"spring-property.xml", daoConfigFile}, context);
		}
	}

	protected static synchronized DataSource getUserDataSource() {
		init("userDataSource", "spring-dao-user.xml");
		return context.getBean("userDataSource", DataSource.class);
	}
	
	protected synchronized DataSource getBiDataSource() {
		init("biDataSource", "spring-dao-bi.xml");
		return context.getBean("biDataSource", DataSource.class);
	}
}
