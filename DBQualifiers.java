package com.newheight.scm.framework.dao.support;

/**
 * 如果应用需要访问多个数据库，则需要为每个数据库分别定义SessionFactory与TransactionManager。
 * 本接口对系统需要访问的每个数据库的TransactionManager的Qualifier和SessionFactory的Qualifier进行定义，
 * 注意相关的值必须与配置文件"dao.xml"中的对应的Qualifier值保存一致
 * @author xuepingjiao
 */
public interface DBQualifiers {
	/**
	 * PMS数据库TransactionManager的Qualifier
	 */
	String PO_TX = "po_tx";
	/**
	 * PMS数据库SessionFactory的Qualifier
	 */
	String PO_SF = "po_sf";
	String PO_DS = "po_ds";
	
	String PROD_DATA_DS = "prod_data_ds";
	String PROD_DATA_TX = "prod_data_tx";
	String PROD_DATA_SF = "prod_data_sf";
	
	String MISC_DATA_TX = "misc_data_tx";
	String MISC_DATA_SF = "misc_data_sf";
	String MISC_DATA_DS = "sec_ds";
	
	String BI_DATA_DS = "bi_data_ds";
	
	String USER_DATA_DS = "user_data_ds";
}
