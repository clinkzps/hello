package com.newheight.scm.framework.dao.support;

import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;


/**
 * 该类为Msic_data2数据库的BaseDAO。<br/>
 * 注意：如果应用需要访问多个数据库，则需要为每个数据库分别定义SessionFactory，这时可以给每个数据库定义一个BaseDAO，
 * 然后通过{@link #initSessionFactory}方法注入需要的sessionFactory对象，其它自定义的DAO继承特定的BaseDAO<br/>
 * 存在多个数据库时，除配置不同的SessionFactory外，还需要配置不同的TransactionManager。
 * @author 陈玉龙
 * @param <T> 操作的实体类型
 */
public class MsicDataBaseDAO<T extends BaseEntity> extends HibernateBaseDAO<T>{
	
	/**
	 * 通过该方法初始化sessionFactory，当存在多个sessionFactory时，通过@Qualifier注解确定具体的sessionFactory
	 * @param sessionFactory
	 */
	@Autowired
	public void initSessionFactory(@Qualifier(DBQualifiers.MISC_DATA_SF) SessionFactory sessionFactory){
		super.setSessionFactory(sessionFactory);
	}

}
