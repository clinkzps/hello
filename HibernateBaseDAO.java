package com.newheight.scm.framework.dao.support;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.ClassUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.reflect.MethodUtils;
import org.hibernate.Criteria;
import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.Example;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.ProjectionList;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Property;
import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.Subqueries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;
import org.springframework.util.CollectionUtils;

import com.newheight.scm.framework.IConstant;
import com.newheight.scm.framework.dto.PageDTO;
import com.newheight.scm.framework.query.support.ExistSubSelectModel;
import com.newheight.scm.framework.query.support.annotation.DisjunctionOperation;
import com.newheight.scm.framework.query.support.annotation.ExistSubSelect;
import com.newheight.scm.framework.query.support.annotation.FieldAlias;
import com.newheight.scm.framework.query.support.annotation.FieldsAlias;
import com.newheight.scm.framework.query.support.annotation.Operation;
import com.newheight.scm.framework.query.support.annotation.OperationType;
import com.newheight.scm.framework.query.support.filter.BaseQueryFilter;
import com.newheight.scm.framework.util.CollectionUtil;
import com.newheight.scm.framework.util.StringUtil;

/**
 * dao基础类，提供了常用的方法
 * 
 * @author xuepingjiao
 * 
 * @param <T>
 */
public class HibernateBaseDAO<T extends BaseEntity> extends HibernateDaoSupport {

    protected Logger log = LoggerFactory.getLogger(HibernateBaseDAO.class);

    /**
     * 根据filter查询条件查找符合条件的所有记录,不分页
     * 
     * @param filter
     * @return List<T>
     */
    public List<T> findByFilter(BaseQueryFilter filter) {
        DetachedCriteria queryCriteria = null;
        List<ExistSubSelectModel> subSelectModels = parseExistSubSelectModel(filter);
        if (CollectionUtils.isEmpty(subSelectModels)) {
            queryCriteria = buildCriteriaByFilter(filter, null);
        } else {
            queryCriteria = buildCriteriaByFilter(filter, "o");
        }
        injectSubSelect(queryCriteria, null, filter);
        appendOrder(queryCriteria, filter);
        return findByCriteria(queryCriteria);
    }

    /**
     * 分页查找记录
     * 
     * @param startIndex
     * @param pageSize
     * @return
     */
    public PageDTO findRange(final int startIndex, final int pageSize) {
    	return getHibernateTemplate().execute(new HibernateCallback<PageDTO>() {

			@Override
			public PageDTO doInHibernate(Session session)
					throws HibernateException, SQLException {
		        String hql = "from " + getEntityName();
		        @SuppressWarnings("unchecked")
				List<T> result = session.createQuery(hql).setFirstResult(startIndex).setMaxResults(pageSize).list();
		        PageDTO dataPage = new PageDTO();
		        dataPage.setData(result);
		
		        hql = "select count(id) from " + getEntityName();
		        int number = ((Number) session.createQuery(hql).uniqueResult()).intValue();
		        dataPage.setTotalCount(number);
		        return dataPage;
			}
		});
    }

    /**
     * 根据查询条件分页查找记录，查询结果为只读。
     * 
     * @param filter
     * @param startIndex
     * @param pageSize
     * @return
     */
    public PageDTO findRangeByFilter(int startIndex, int pageSize, BaseQueryFilter filter) {
        DetachedCriteria queryCriteria = null;
        DetachedCriteria countCriteria = null;
        List<ExistSubSelectModel> subSelectModels = parseExistSubSelectModel(filter);
        if (CollectionUtils.isEmpty(subSelectModels)) {
            queryCriteria = buildCriteriaByFilter(filter, null);
            countCriteria = buildCriteriaByFilter(filter, null);
        } else {
            queryCriteria = buildCriteriaByFilter(filter, "o");
            countCriteria = buildCriteriaByFilter(filter, "o");
        }
        injectSubSelect(queryCriteria, countCriteria, filter);
        appendOrder(queryCriteria, filter);
        return findByCriteria(queryCriteria, countCriteria, startIndex, pageSize, true);
    }

    /**
     * 根据filter统计满足条件的记录数
     * 
     * @param filter
     * @return
     */
    public Integer countByFilter(BaseQueryFilter filter) {
        DetachedCriteria countCriteria = null;
        List<ExistSubSelectModel> subSelectModels = parseExistSubSelectModel(filter);
        if (CollectionUtils.isEmpty(subSelectModels)) {
            countCriteria = buildCriteriaByFilter(filter, null);
        } else {
            countCriteria = buildCriteriaByFilter(filter, "o");
        }
        injectSubSelect(countCriteria, null, filter);
        countCriteria.setProjection(Projections.count("id"));
        return ((Number)getHibernateTemplate().findByCriteria(countCriteria, 0, 1).get(0)).intValue();
    }

    /**
     * 根据hql语句执行查询
     * 
     * @param hql
     * @param params
     * @return
     */
    public List<?> executeQuery(String hql, Object... params) {
        return getHibernateTemplate().find(hql, params);
    }

    public Object getByQuery(final String hql, final Object... params) {
    	return getHibernateTemplate().execute(new HibernateCallback<Object>() {

			@Override
			public Object doInHibernate(Session session)
					throws HibernateException, SQLException {
				 Query query = session.createQuery(hql);
			     prepareParameter(query, params);
			     query.setMaxResults(1);
			     return query.uniqueResult();
			}
    	});
       
    }

    public List<?> executeSqlQuery(final String sql, final Object... params) {
    	return getHibernateTemplate().executeFind(new HibernateCallback<List<?>>(){
			@Override
			public List<?> doInHibernate(Session session)
					throws HibernateException, SQLException {
				Query sqlQuery = session.createSQLQuery(sql);
		        prepareParameter(sqlQuery, params);
		        return sqlQuery.list();
			}
    	});
    }

    public Object getBySqlQuery(final String sql, final Object... params) {
    	return getHibernateTemplate().execute(new HibernateCallback<Object>(){
			@Override
			public Object doInHibernate(Session session)
					throws HibernateException, SQLException {
				  Query sqlQuery = session.createSQLQuery(sql);
			      prepareParameter(sqlQuery, params);
			      sqlQuery.setMaxResults(1);
			      return sqlQuery.uniqueResult();
			}
    	});
    }

    public int bulkUpdate(String hql) {
        return this.getHibernateTemplate().bulkUpdate(hql);
    }

    public int bulkUpdate(String hql, Object... params) {
        return this.getHibernateTemplate().bulkUpdate(hql, params);
    }

    protected PageDTO executeQuery(final String hql, final String countHQL, final int startRow, 
    		final int pageSize, final Object... params) {
    	return getHibernateTemplate().execute(new HibernateCallback<PageDTO>() {

			@Override
			public PageDTO doInHibernate(Session session)
					throws HibernateException, SQLException {

		        String countHql = countHQL;
		        Query query = session.createQuery(hql);
		        if (StringUtil.isEmpty(countHql)) {
		            countHql = "select count(*) " + hql;
		        }
		        Query countQuery = session.createQuery(countHql);
		        for (int i = 0; i < params.length; i++) {
		            if (params[i] instanceof String) {
		                ((String) params[i]).trim();
		            }
		            query.setParameter(i, params[i]);
		            countQuery.setParameter(i, params[i]);
		        }
		        if (pageSize > 0) {
		            query.setFirstResult(startRow);
		            query.setMaxResults(pageSize);
		        }
		        List<?> list = query.list();
		        Long cnt = (Long) countQuery.uniqueResult();
		        PageDTO dataPage = new PageDTO();
		        dataPage.setData(list);
		        dataPage.setTotalCount(cnt.intValue());
		        return dataPage;
			}
    	});
    }

    /**
     * 查找实体对应的表的所有记录
     * 
     * @return List<T>
     */
    public List<T> findAll() {
        return findAll(false);
    }

    /**
     * 
     * @param isReadOnly
     *            是否是只读查询
     * @return
     */
    @SuppressWarnings("unchecked")
    public List<T> findAll(final boolean isReadOnly) {
        return getHibernateTemplate().executeFind(new HibernateCallback<List<T>>() {

            @Override
            public List<T> doInHibernate(Session session) throws HibernateException, SQLException {
                Query query = session.createQuery("from " + getEntityName());
                query.setReadOnly(isReadOnly);
                return query.list();
            }

        });

    }

    /**
     * 查询指定属性的值
     * 
     * @param <P>
     *            属性类型
     * @param filter
     *            Map类型的查询条件，key为条件中的属性名，value为条件中的属性值，value为集合或数组时将使用in查询，
     *            value为null时将使用is null查询。
     * @param propertyName
     *            要查询的属性
     * @param type
     *            属性类型，决定了返回的集合中的元素类型
     * @return
     */
    @SuppressWarnings("unchecked")
	public <P> List<P> findProperty(Map<String, Object> filter, String propertyName, Class<P> type) {
        DetachedCriteria dc = buildCriteriaByProperties(filter);
        dc.setProjection(Projections.property(propertyName));
        return getHibernateTemplate().findByCriteria(dc, 0, 1000);
    }

    /**
     * 查询1个或多个指定属性的值
     * 
     * @param filter
     *            map类型的查询条件，key为条件中的属性名，value为条件中的属性值，value为集合或数组时将使用in查询，
     *            value为null时将使用is null查询。
     * @param propertyNames
     *            需要查询的属性。传入的顺序与返回的值中的顺序保持一致
     * @return 返回的List中的元素为数组。数组中的第1个元素对应参数propertyNames中的第一个属性的值。
     */
    @SuppressWarnings("unchecked")
	public List<Object[]> findProperties(Map<String, Object> filter, String... propertyNames) {
        if (propertyNames == null || propertyNames.length == 0) {
            return Collections.emptyList();
        }
        DetachedCriteria dc = buildCriteriaByProperties(filter);
        if (propertyNames.length == 1) {
            dc.setProjection(Projections.property(propertyNames[0]));
        } else {
            ProjectionList pl = Projections.projectionList();
            for (String prop : propertyNames) {
                pl.add(Projections.property(prop));
            }
            dc.setProjection(pl);
        }
        return getHibernateTemplate().findByCriteria(dc, 0, 1000);
    }

    protected Query createQuery(String hql) {
        return getSession().createQuery(hql);
    }

    /**
     * 
     * @param entity
     * @throws HibernateException
     */
    public void remove(T entity) throws HibernateException {
        getHibernateTemplate().delete(entity);
    }

    /**
     * 
     * @param id
     */
    public void remove(Long id) throws HibernateException {
        this.deleteById(id);
    }

    /**
     * 
     * @param id
     * @return
     * @throws HibernateException
     */
    public T get(Long id) throws HibernateException {
        if (id == null) {
            return null;
        }
        return getHibernateTemplate().get(getEntityClass(), id);
    }

    /**
     * 根据主键,批量查询数据,需要对应的filter从实现IBatchFilter接口
     * 
     * @param keys
     *            List
     * @return List<T> 按照keys的顺序输出查询结果的数据
     */
    public List<T> getByKeys(List<Long> keys) {
        return this.getByKeys(keys, false);
    }

    /**
     * 根据主键,批量查询数据,需要对应的filter从实现IBatchFilter接口
     * 
     * @param keys
     *            List
     * @param isReadOnly
     *            返回的数据是否只读
     * @return List<T> 按照keys的顺序输出查询结果的数据
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
	public List<T> getByKeys(final List<Long> keys, final boolean isReadOnly) {
    	 if (CollectionUtil.isNullOrEmpty(keys)) {
             return Collections.emptyList();
         }
    	return getHibernateTemplate().executeFind(new HibernateCallback<List<T>>() {
            @Override
            public List<T> doInHibernate(Session session) throws HibernateException, SQLException {
       
				List<Long> uniqueKeys = CollectionUtil.uniqueList(keys);// 排除重复记录
		        // List<T> result = new ArrayList<T>(keys.size());
		
		        List<T> datas = null;
		
		        if (uniqueKeys.size() == 1) {// 如果只有一个主键的时候,自动是用get方法查询
		            datas = new ArrayList<T>(1);
		            T o = get(uniqueKeys.get(0));
		            if (o != null) {
		                datas.add(o);
		            }
		        } else {
		            String className = getEntityName();
		            datas = new ArrayList<T>(uniqueKeys.size());
		
		            String hql = "from " + className + " o where o.id in ( :id )";
		            Query query = session.createQuery(hql);
		            if (isReadOnly) {
		                query.setReadOnly(true);
		            }
		            // 每次查询指定batchSize行的数据
		            int batchSize = 100;
		            Iterator<List> subList = (Iterator<List>) CollectionUtil.getBatchIterator(uniqueKeys, batchSize);
		            while (subList.hasNext()) {
		                List list = subList.next();
		                query.setParameterList("id", list);
		                // 查询
						List<T> values = (List<T>) query.list();
		                for (T o : values) {
		                    datas.add(o);
		                }
		            }
		        }
		        /*
		         * try { fillWhenNull(uniqueKeys, datas, result); } catch (Exception e)
		         * { log.error("error:", e); }
		         */
		        return datas;
            }
        });

    }

    /**
     * 返回指定Id对应的所有对象，如果没有查询到这个对象，则创建一个新的。
     * 
     * @param idList
     *            等查询的Id列表
     * @param findResult
     *            从数据库中查询的数据
     * @param result
     *            最终的结果
     * @throws Exception
     */
    protected void fillWhenNull(List<Long> idList, List<T> findResult, List<T> result) throws Exception {

        Method idMethod = ClassUtils.getPublicMethod(getEntityClass(), "getId", null);

        Map<Object, T> dataMap = new HashMap<Object, T>((int) (idList.size() / 0.75) + 1);
        for (T t : findResult) {
            dataMap.put(idMethod.invoke(t), t);
        }
        T t = null;
        for (Object s : idList) {
            t = (T) dataMap.get(s);
            if (t == null) {
                t = (T) getEntityClass().newInstance();// 确保不返回null
            }
            result.add(t);
        }
    }

    /**
     * 
     * @param id
     * @return
     * @throws HibernateException
     */
    public T load(Long id) throws HibernateException {
        return getHibernateTemplate().load(getEntityClass(), id);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.jboss.seam.framework.HibernateEntityController#merge(java.lang.Object
     * )
     */
    @SuppressWarnings("hiding")
    public <T> T merge(T entity) throws HibernateException {
        return getHibernateTemplate().merge(entity);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.jboss.seam.framework.HibernateEntityController#persist(java.lang.
     * Object)
     */
    public void persist(Object entity) throws HibernateException {
        getHibernateTemplate().persist(entity);
    }

    public void saveOrUpdate(Object entity) {
        getHibernateTemplate().saveOrUpdate(entity);
    }

    /**
     * 保存指定的实体对象列表
     * 
     * @param entities
     *            实体对象列表
     */
    public void saveOrUpdateAll(Collection<?> entities) {
        for (Object entity : entities) {
            saveOrUpdate(entity);
        }
    }

    public void update(Object entity) {
        getHibernateTemplate().update(entity);
    }

    public void save(Object entity) {
        getHibernateTemplate().save(entity);
    }

    /**
     * 取得实体的名字
     * 
     * @return
     */
    protected String getEntityName() {
        return getEntityClass().getName();
    }

    /**
     * 
     * @return 查询的实体类
     */
    @SuppressWarnings("unchecked")
    public Class<T> getEntityClass() {
        ParameterizedType pt = (ParameterizedType) this.getClass().getGenericSuperclass();

        return (Class<T>) pt.getActualTypeArguments()[0];
    }

    /**
     * 根据条件判断是有数据存在
     * 
     * @param propertyName
     *            属性的名称
     * @param propertyValue
     *            属性的值
     * @param idValue
     *            要判断的实体的ID值，默认ID属性的名称为“id”, 若值不为空，则排除ID为idValue的实体，否则包括实体本身
     * @return
     */
    public boolean isExists(String propertyName, Object propertyValue, Serializable idValue) {
        Map<String, Object> params = new HashMap<String, Object>(2);
        params.put(propertyName, propertyValue);
        return this.isExists(params, idValue);
    }

    /**
     * 根据条件判断是否存在对应记录
     * 
     * @param propertyName
     *            属性的名称
     * @param propertyValue
     *            属性的值
     * @param idProperty
     *            ID属性的名称
     * @param idValue
     *            ID属性的值，若值不为空，则排除ID为idValue的实体，否则包括实体本身
     * @return
     */
    public boolean isExists(String propertyName, Object propertyValue, String idProperty, Serializable idValue) {
        Map<String, Object> params = new HashMap<String, Object>(2);
        params.put(propertyName, propertyValue);
        return isExists(params, idProperty, idValue);
    }

    /**
     * 根据条件判断是否存在对应的数据
     * 
     * @param params
     *            判断条件的key-value对，key为条件的属性名称，值为属性的值
     * @param idValue
     *            判断实体的ID值，若值不为空，则排除ID为idValue的实体，否则包括实体本身
     * @return
     */
    public boolean isExists(Map<String, Object> params, Serializable idValue) {
        return this.isExists(params, "id", idValue);
    }

    /**
     * 根据条件,判断是否存在数据
     * 
     * @param params
     *            属性名和值的映射，如果值为null，将使用is null进行判断
     * @param idProperty
     *            ID属性的名称
     * @param idValue
     *            判断实体的ID值，若值不为空，则排除ID为idValue的实体，否则包括实体本身
     * @return
     */
    public boolean isExists(Map<String, Object> params, String idProperty, Serializable idValue) {
        if (params == null || params.isEmpty()) {
            return false;
        }
        DetachedCriteria cri = DetachedCriteria.forClass(getEntityClass());
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            String propertyName = entry.getKey();
            if (StringUtil.isEmpty(propertyName)) {
                throw new IllegalArgumentException();
            }
            Object value = entry.getValue();
            if (value == null) {
                cri.add(Restrictions.isNull(propertyName));
            } else {
                cri.add(Restrictions.eq(propertyName, entry.getValue()));
            }
        }
        if (idValue != null && !StringUtil.isEmpty(idProperty)) {
            cri.add(Restrictions.ne(idProperty, idValue));
        }
        cri.setProjection(Projections.count("id"));
        return ((Number) (getHibernateTemplate().findByCriteria(cri).get(0))).intValue() > 0;
    }

    protected void prepareParameter(Query query, Object... params) {
        this.prepareParameter(params, query, null);
    }

    protected void prepareParameter(List<Object> paramList, Query query, Query countQuery) {
        this.prepareParameter(paramList.toArray(), query, countQuery);
    }

    private void prepareParameter(Object[] params, Query query, Query countQuery) {
    	if(params == null) {
    		return;
    	}
        for (int i = 0; i < params.length; i++) {
            query.setParameter(i, params[i]);
            if (countQuery != null) {
                countQuery.setParameter(i, params[i]);
            }
        }
    }

    /**
     * 根据Id值删除实体
     * 
     * @param id
     */
    public void deleteById(final Long id) {
        getHibernateTemplate().delete(load(id));
    }

    /**
     * 传入删除条件创建删除对象的Query。 条件中请对修改的对象使用别名"o"<br/>
     * <li>Eg: o.id = :id<br/> <li>Eg: createDeleteQuery(session,
     * "o.orderNo = :orderNo").setString("orderNo", "DO120").executeUpdate();
     * <br/>
     * 
     * @param session
     * @param whereCondition
     *            删除的条件语句，请用HQL Query。不需要加关键字"where"。<br/>
     * @return 返回基于本地化查询语句生成的Query对象
     */
    protected Query createDeleteQueryByCondition(Session session, String whereCondition) {
        StringBuilder builder = new StringBuilder("delete from ").append(this.getEntityName()).append(" o where ")
                .append(whereCondition);
        Query query = session.createQuery(builder.toString());
        return query;
    }

    /**
     * 按Criteria进行查询
     * 
     * @param queryCri
     *            包含查询条件的Criteria
     * @param countCri
     *            包含统计行数条件的Criteria
     * @param startIndex
     *            分页开始记录
     * @param pageSize
     *            查询记录数
     * @return
     */
    public PageDTO findByCriteria(DetachedCriteria queryCri, DetachedCriteria countCri, int startIndex, int pageSize) {
        return this.findByCriteria(queryCri, countCri, startIndex, pageSize, false);
    }

    public PageDTO findByCriteria(final DetachedCriteria queryCri, final DetachedCriteria countCri, 
    		final int startIndex, final int pageSize,
            final boolean readOnly) {
    	return getHibernateTemplate().execute(new HibernateCallback<PageDTO>() {

			@Override
			public PageDTO doInHibernate(Session session)
					throws HibernateException, SQLException {
		        PageDTO dataPage = new PageDTO();
		        Criteria cri = queryCri.getExecutableCriteria(session);
		        if (readOnly) {
		            cri.setReadOnly(true);
		        }
		        if (pageSize > 0) {
		            cri.setFirstResult(startIndex);
		            cri.setMaxResults(pageSize);
		        }
		        @SuppressWarnings("unchecked")
		        List<T> result = (List<T>) cri.list();
		
		        dataPage.setData(result);
		
		        countCri.setProjection(Projections.count("id"));
		        int totalCount = ((Number) (countCri.getExecutableCriteria(session).uniqueResult())).intValue();
		        dataPage.setTotalCount(totalCount);
		        return dataPage;
			}
		});

    }

    @SuppressWarnings("unchecked")
    public List<T> findByCriteria(DetachedCriteria queryCri) {
        return getHibernateTemplate().findByCriteria(queryCri);
    }

    @SuppressWarnings("unchecked")
    public <X> List<X> findByCriteria(DetachedCriteria queryCri, Class<X> type) {
        return getHibernateTemplate().findByCriteria(queryCri);
    }

    @SuppressWarnings("unchecked")
    public List<T> findByCriteria(DetachedCriteria queryCri, int firstResult, int maxResults) {
        return getHibernateTemplate().findByCriteria(queryCri, firstResult, maxResults);

    }

    @SuppressWarnings("unchecked")
    public List<T> findByExample(Example example) {
        return getHibernateTemplate().findByExample(example);
    }

    @SuppressWarnings("unchecked")
    public List<T> findByProperties(Map<String, Object> property) {
        if (property == null || property.isEmpty()) {
            return Collections.emptyList();
        }
        return getHibernateTemplate().findByCriteria(buildCriteriaByProperties(property));
    }

    @SuppressWarnings("unchecked")
    public PageDTO findRangeByProperties(int startIndex, int pageSize, Map<String, Object> property) {
        if (property == null || property.isEmpty()) {
            return this.findRange(startIndex, pageSize);
        }
        PageDTO page = new PageDTO();
        List<T> data = getHibernateTemplate().findByCriteria(buildCriteriaByProperties(property, true), startIndex,
                pageSize);
        page.setData(data);
        DetachedCriteria dc = buildCriteriaByProperties(property);
        dc.setProjection(Projections.rowCount());
        int count = ((Number) getHibernateTemplate().findByCriteria(dc).get(0)).intValue();
        page.setTotalCount(count);
        return page;
    }

    @SuppressWarnings("unchecked")
    public T getByProperties(Map<String, Object> property) {
        if (property == null || property.isEmpty()) {
            return null;
        }
        List<T> list = getHibernateTemplate().findByCriteria(buildCriteriaByProperties(property), 0, 1);
        return list.isEmpty() ? null : list.get(0);
    }

    protected DetachedCriteria buildCriteriaByProperties(Map<String, Object> property) {
        return this.buildCriteriaByProperties(property, false);
    }

    protected DetachedCriteria buildCriteriaByProperties(Map<String, Object> property, boolean containsSort) {
        DetachedCriteria cri = DetachedCriteria.forClass(getEntityClass());
        for (Map.Entry<String, Object> entry : property.entrySet()) {
            String propertyName = entry.getKey();
            if (StringUtil.isEmpty(propertyName)) {
                throw new IllegalArgumentException();
            }
            Object value = entry.getValue();
            if (propertyName.equals(IConstant.SORT_NAME_KEY)) {
                if (!containsSort) {
                    continue;
                }
                String[] sort = (String[]) value;
                if ("ASC".equalsIgnoreCase(sort[1])) {
                    cri.addOrder(Order.asc(sort[0]));
                } else {
                    cri.addOrder(Order.desc(sort[0]));
                }
                continue;
            }
            if (value == null) {
                cri.add(Restrictions.isNull(propertyName));
            } else if (value instanceof Collection) {
                cri.add(Restrictions.in(propertyName, (Collection<?>) value));
            } else if (value.getClass().isArray()) {
                cri.add(Restrictions.in(propertyName, (Object[]) value));
            } else {
                cri.add(Restrictions.eq(propertyName, value));
            }
        }
        return cri;
    }

    protected DetachedCriteria buildCriteriaByProperty(String propertyName, Object propertyValue) {
        DetachedCriteria cri = DetachedCriteria.forClass(getEntityClass());
        if (StringUtil.isEmpty(propertyName)) {
            throw new IllegalArgumentException();
        }
        if (propertyValue == null) {
            cri.add(Restrictions.isNull(propertyName));
        } else {
            cri.add(Restrictions.eq(propertyName, propertyValue));
        }
        return cri;
    }

    @SuppressWarnings("unchecked")
    public List<T> findByProperty(String propertyName, Object propertyValue) {
        return getHibernateTemplate().findByCriteria(buildCriteriaByProperty(propertyName, propertyValue));
    }

    /**
     * 根据属性值使用like查询对象
     * 
     * @param propertyName
     * @param propertyValue
     * @param likeModel
     * @return
     */
    @SuppressWarnings("unchecked")
    public List<T> findByPropertyLike(String propertyName, Object propertyValue, MatchMode likeModel) {
        DetachedCriteria cri = DetachedCriteria.forClass(getEntityClass());
        cri.add(Restrictions.like(propertyName, String.valueOf(propertyValue), likeModel));
        return getHibernateTemplate().findByCriteria(cri);
    }

    /**
     * 根据属性值使用like查询对象，忽略大小写
     * 
     * @param propertyName
     * @param propertyValue
     * @param likeModel
     * @return
     */
    @SuppressWarnings("unchecked")
    public List<T> findByPropertyILike(String propertyName, Object propertyValue, MatchMode likeModel) {
        DetachedCriteria cri = DetachedCriteria.forClass(getEntityClass());
        cri.add(Restrictions.ilike(propertyName, String.valueOf(propertyValue), likeModel));
        return getHibernateTemplate().findByCriteria(cri);
    }

    @SuppressWarnings("unchecked")
    public T getByProperty(String propertyName, Object propertyValue) {
        List<T> list = getHibernateTemplate()
                .findByCriteria(buildCriteriaByProperty(propertyName, propertyValue), 0, 1);
        return list.isEmpty() ? null : list.get(0);
    }

    @SuppressWarnings("unchecked")
    public List<T> findByNamedQuery(String queryName, Object... parameters) {
        return getHibernateTemplate().findByNamedQuery(queryName, parameters);
    }

    public void init(Object o) {
        Hibernate.initialize(o);
    }

    public void flush() {
        getSession(false).flush();
    }

    public void clear() {
        getSession(false).clear();
    }

    private DetachedCriteria appendCondition(DetachedCriteria criteria, String fieldName, OperationType type,
            Object param) {
        if (param == null || "".equals(param)) {
            criteria.add(Restrictions.isNull(fieldName));
            return criteria;
        }
        switch (type) {
            case EQUAL:
                criteria.add(Restrictions.eq(fieldName, param));
                break;
            case NOT_EQUAL:
                criteria.add(Restrictions.ne(fieldName, param));
                break;
            case LESS_THAN:
                if (param instanceof Date) {
                    param = injectHoursAndMinuts((Date) param);
                }
                criteria.add(Restrictions.lt(fieldName, param));
                break;
            case NOT_LESS_THAN:
                criteria.add(Restrictions.ge(fieldName, param));
                break;
            case GREAT_THAN:
                criteria.add(Restrictions.gt(fieldName, param));
                break;
            case NOT_GREAT_THAN:
                if (param instanceof Date) {
                    param = injectHoursAndMinuts((Date) param);
                }
                criteria.add(Restrictions.le(fieldName, param));
                break;
            case LIKE:
                criteria.add(Restrictions.like(fieldName, String.valueOf(param), MatchMode.ANYWHERE));
                break;
            case LEFT_LIKE:
                criteria.add(Restrictions.like(fieldName, String.valueOf(param), MatchMode.START));
                break;
            case RIGHT_LIKE:
                criteria.add(Restrictions.like(fieldName, String.valueOf(param), MatchMode.END));
                break;
            case NOT_LIKE:
                criteria.add(Restrictions.not(Restrictions.like(fieldName, param)));
                break;
            case IN:
                if (param instanceof Collection) {
                    criteria.add(Restrictions.in(fieldName, (Collection<?>) param));
                } else if (param.getClass().isArray()) {
                    criteria.add(Restrictions.in(fieldName, (Object[]) param));
                } else {
                    criteria.add(Restrictions.in(fieldName, new Object[] { param }));
                }
                break;
            case NOTIN:
                if (param instanceof Collection) {
                    criteria.add(Restrictions.not(Restrictions.in(fieldName, (Collection<?>) param)));
                } else if (param.getClass().isArray()) {
                    criteria.add(Restrictions.not(Restrictions.in(fieldName, (Object[]) param)));
                } else {
                    criteria.add(Restrictions.not(Restrictions.in(fieldName, new Object[] { param })));
                }
                break;
            default:
                criteria.add(Restrictions.eq(fieldName, param));
        }
        return criteria;
    }

    protected Disjunction appendCondition(Disjunction disjunction, String fieldName, OperationType type, Object param) {
        if (param == null || "".equals(param)) {
            disjunction.add(Restrictions.isNull(fieldName));
            return disjunction;
        }
        switch (type) {
            case EQUAL:
                disjunction.add(Restrictions.eq(fieldName, param));
                break;
            case NOT_EQUAL:
                disjunction.add(Restrictions.ne(fieldName, param));
                break;
            case LESS_THAN:
                if (param instanceof Date) {
                    param = injectHoursAndMinuts((Date) param);
                }
                disjunction.add(Restrictions.lt(fieldName, param));
                break;
            case NOT_LESS_THAN:
                disjunction.add(Restrictions.ge(fieldName, param));
                break;
            case GREAT_THAN:
                disjunction.add(Restrictions.gt(fieldName, param));
                break;
            case NOT_GREAT_THAN:
                if (param instanceof Date) {
                    param = injectHoursAndMinuts((Date) param);
                }
                disjunction.add(Restrictions.le(fieldName, param));
                break;
            case LIKE:
                disjunction.add(Restrictions.like(fieldName, String.valueOf(param), MatchMode.ANYWHERE));
                break;
            case LEFT_LIKE:
                disjunction.add(Restrictions.like(fieldName, String.valueOf(param), MatchMode.START));
                break;
            case RIGHT_LIKE:
                disjunction.add(Restrictions.like(fieldName, String.valueOf(param), MatchMode.END));
                break;
            case NOT_LIKE:
                disjunction.add(Restrictions.not(Restrictions.like(fieldName, param)));
                break;
            case IN:
                if (param instanceof Collection) {
                    disjunction.add(Restrictions.in(fieldName, (Collection<?>) param));
                } else if (param.getClass().isArray()) {
                    disjunction.add(Restrictions.in(fieldName, (Object[]) param));
                } else {
                    disjunction.add(Restrictions.in(fieldName, new Object[] { param }));
                }
                break;
            case NOTIN:
                if (param instanceof Collection) {
                    disjunction.add(Restrictions.not(Restrictions.in(fieldName, (Collection<?>) param)));
                } else if (param.getClass().isArray()) {
                    disjunction.add(Restrictions.not(Restrictions.in(fieldName, (Object[]) param)));
                } else {
                    disjunction.add(Restrictions.not(Restrictions.in(fieldName, new Object[] { param })));
                }
                break;
            default:
                disjunction.add(Restrictions.eq(fieldName, param));
        }
        return disjunction;
    }

    private Date injectHoursAndMinuts(Date date) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        if (c.get(Calendar.HOUR_OF_DAY) == 0 && c.get(Calendar.MINUTE) == 0) {
            c.set(Calendar.HOUR_OF_DAY, 23);
            c.set(Calendar.MINUTE, 59);
        }
        return c.getTime();
    }

    protected void appendOrder(DetachedCriteria criteria, BaseQueryFilter filter) {
        Map<String, String> orders = filter.getOrderByMap();
        if (orders == null || orders.isEmpty()) {
            return;
        }
        Set<Map.Entry<String, String>> orderEntrys = orders.entrySet();
        for (Map.Entry<String, String> orderEntry : orderEntrys) {
            String name = orderEntry.getKey();
            if (name.startsWith("o.")) {
                name = name.substring(2);
            }
            if ("ASC".equalsIgnoreCase(orderEntry.getValue())) {
                criteria.addOrder(Order.asc(name));
            } else {
                criteria.addOrder(Order.desc(name));
            }
        }
    }

    /**
     * 根据filter组装DetachedCriteria
     * 
     * @param filter
     * @param entityAlias
     *            实体的别名
     * @return
     */
    protected DetachedCriteria buildCriteriaByFilter(BaseQueryFilter filter, String entityAlias) {
        DetachedCriteria criteria = null;
        if (StringUtils.isEmpty(entityAlias)) {
            criteria = DetachedCriteria.forClass(getEntityClass());
        } else {
            criteria = DetachedCriteria.forClass(getEntityClass(), entityAlias);
        }
        parseAlias(filter, criteria, entityAlias);
        Method[] methods = filter.getClass().getMethods();
        for (Method method : methods) {
            if ((!method.getName().startsWith("is")) && (!method.getName().startsWith("get"))) {
                continue;
            }
            if (method.getParameterTypes().length > 0) {
                continue;
            }
            Operation operation = method.getAnnotation(Operation.class);
            if (operation != null && operation.operationType() == OperationType.IGNORE) {
                continue;
            }
            DisjunctionOperation co = null;
            if (operation == null) {
                co = method.getAnnotation(DisjunctionOperation.class);
                if (co == null) {
                    continue;
                }
            }
            try {
                Object param = method.invoke(filter);
                if (param == null || "".equals(param)) {
                    if (operation != null && !operation.includeNullValues()) {
                        continue;
                    }
                }
                if (operation != null) {
                    String fieldName = operation.fieldName();
                    if (fieldName.startsWith("#")) {
                        fieldName = readFieldNameByMethod(filter, fieldName.substring(1));
                        if (StringUtils.isEmpty(fieldName)) {
                            continue;
                        }
                    }
                    fieldName = getFieldNameWithEntityAlias(fieldName, entityAlias);
                    appendCondition(criteria, fieldName, operation.operationType(), param);
                } else if (co != null) {
                    if (param == null || ((Object[]) param).length == 0) {
                        continue;
                    }
                    appendCompoundOrCondition(criteria, co, (Object[]) param, entityAlias);
                }
            } catch (Exception e) {
                logger.error("appendCondition error", e);
            }
        }
        return criteria;
    }

    private String readFieldNameByMethod(Object obj, String property) {
        if (obj == null || StringUtils.isEmpty(property)) {
            return null;
        }
        try {
            return (String) (MethodUtils.invokeExactMethod(obj, "get" + StringUtils.capitalize(property), null));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected void appendCompoundOrCondition(DetachedCriteria criteria, DisjunctionOperation operation, Object[] params,
            String entityAlias) {
        String[] fieldNames = operation.fieldNames();
        OperationType[] operationTypes = operation.operationTypes();
        Disjunction disjunction = Restrictions.disjunction();
        for (int index = 0; index < fieldNames.length; index++) {
            String fieldName = getFieldNameWithEntityAlias(fieldNames[index], entityAlias);
            appendCondition(disjunction, fieldName, operationTypes[index], params[index]);
        }
        criteria.add(disjunction);
    }

    protected void parseAlias(BaseQueryFilter filter, DetachedCriteria criteria, String entityAlias) {
        FieldAlias fieldAlias = filter.getClass().getAnnotation(FieldAlias.class);
        if (fieldAlias != null) {
            criteria.createAlias(getFieldNameWithEntityAlias(fieldAlias.field(), entityAlias), fieldAlias.alias());
            return;
        }
        FieldsAlias fieldsAlias = filter.getClass().getAnnotation(FieldsAlias.class);
        if (fieldsAlias == null || fieldsAlias.value() == null) {
            return;
        }
        for (FieldAlias a : fieldsAlias.value()) {
            criteria.createAlias(getFieldNameWithEntityAlias(a.field(), entityAlias), a.alias());
        }
    }

    protected String getFieldNameWithEntityAlias(String fieldName, String entityAlias) {
        if (StringUtils.isEmpty(entityAlias)) {
            if (fieldName.startsWith("o.")) {
                fieldName = fieldName.substring(2);
            }
            return fieldName;
        }
        // has alias
        if (entityAlias.equals("o")) {
            if (fieldName.startsWith("o.") || fieldName.indexOf('.') != -1) {
                return fieldName;
            }
            return "o." + fieldName;
        } else {
            if (fieldName.startsWith("o.")) {
                fieldName = fieldName.substring(2);
            } else if (fieldName.indexOf('.') != -1) {
                return fieldName;
            }
            return entityAlias + "." + fieldName;
        }
    }

    protected void injectSubSelect(DetachedCriteria queryCriteria, DetachedCriteria countCriteria,
            BaseQueryFilter filter) {
        List<ExistSubSelectModel> subSelectModels = parseExistSubSelectModel(filter);
        if (CollectionUtils.isEmpty(subSelectModels)) {
            return;
        }
        for (ExistSubSelectModel model : subSelectModels) {
            DetachedCriteria criteria = getExistSubSelectCriteria(model, filter);
            if (model.isExistType()) {
                queryCriteria.add(Subqueries.exists(criteria));
                if (countCriteria != null) {
                    countCriteria.add(Subqueries.exists(criteria));
                }
            } else {
                queryCriteria.add(Subqueries.notExists(criteria));
                if (countCriteria != null) {
                    countCriteria.add(Subqueries.notExists(criteria));
                }
            }
        }
    }

    private DetachedCriteria getExistSubSelectCriteria(ExistSubSelectModel model, BaseQueryFilter filter) {
        DetachedCriteria criteria = DetachedCriteria.forClass(model.getInnerClass(), "inner");
        criteria.setProjection(Projections.property("inner.id"));
        List<ExistSubSelect> configs = model.getConfigs();
        List<Object> params = model.getParams();
        Map<String, String> joinConditions = new HashMap<String, String>();
        for (int i = 0; i < configs.size(); i++) {
            ExistSubSelect config = configs.get(i);
            Object param = params.get(i);
            appendCondition(criteria, getFieldNameWithEntityAlias(config.property(), "inner"), config.operationType(),
                    param);
            joinConditions.put(config.innerJoinProperty(), config.outProperty());
        }
        for (Map.Entry<String, String> entry : joinConditions.entrySet()) {
            String outterProperty = entry.getValue();
            if (!outterProperty.startsWith("o.")) {
                outterProperty = "o." + outterProperty;
            }
            criteria.add(Property.forName(getFieldNameWithEntityAlias(entry.getKey(), "inner")).eqProperty(
                    outterProperty));
        }
        return criteria;
    }

    protected List<ExistSubSelectModel> parseExistSubSelectModel(BaseQueryFilter filter) {
        Map<String, ExistSubSelectModel> models = new HashMap<String, ExistSubSelectModel>();
        for (Method method : filter.getClass().getMethods()) {
            ExistSubSelect config = method.getAnnotation(ExistSubSelect.class);
            if (config == null) {
                continue;
            }
            Object param = null;
            try {
                if ((param = method.invoke(filter)) == null) {
                    continue;
                }
            } catch (Exception e) {
                e.printStackTrace();
                continue;
            }
            ExistSubSelectModel model = models.get(config.innerClass().toString() + "_" + config.existType());
            if (model == null) {
                model = new ExistSubSelectModel();
                model.setInnerClass(config.innerClass());
                model.setExistType(config.existType());
                models.put(config.innerClass() + "_" + config.existType(), model);
            }
            model.setExistType(config.existType());
            model.getConfigs().add(config);
            model.getParams().add(param);
        }
        return new ArrayList<ExistSubSelectModel>(models.values());
    }

    /**
     * 更新对象状态
     * 
     * @param entity
     *            实体
     */
    public void refresh(Object entity) {
        this.getHibernateTemplate().refresh(entity);
    }

    /**
     * <pre>
	 * 根据指定的hql、参数、startIndex、endIndex 来取部分数据
	 * 示例:
	 *		String hql = "from Cust as cust where cust.age > ?";
	 * 		List&lt;Cust&gt; custs = dao.findPartial(hql, 0, 10, null, 0);
	 * </pre>
     * 
     * @param <E>
     *            实体类型
     * @param hql
     *            hql语句
     * @param startIndex
     *            起始位置,从0开始
     * @param endIndex
     *            结束位置
     * @param orders
     *            Order对象，作为排序依据，如果为null，则不进行排序
     * @param values
     *            参数值
     * @return List&lt;T&gt; 分页查询的结果列表
     */
    @SuppressWarnings("unchecked")
	public <E> List<E> findPartial(final String hql1, final int startIndex, final int endIndex, 
			final List<? extends Order> orders,
            final Object... values) {
        if (startIndex < 0 || endIndex <= startIndex) {
            return null;
        }

        final String hql = getNamedHql(hql1, values);
        return getHibernateTemplate().executeFind(new HibernateCallback<List<?>>(){
			@Override
			public List<?> doInHibernate(Session session)
					throws HibernateException, SQLException {
				Query queryObject = null;
		        if (orders != null && !orders.isEmpty()) {
		            StringBuilder pageHql = new StringBuilder(hql);
		
		            pageHql.append(" order by ");
		            for (Order order : orders) {
		                pageHql.append(order.toString());
		                pageHql.append(", ");
		            }
		            String hql1 = hql.toLowerCase();
		            int fromIndex = hql1.indexOf("from");
		            int selectIndex = hql1.indexOf("select");
		            if (fromIndex != -1 && selectIndex != -1 && selectIndex < fromIndex) {
		                String prefix = hql.substring(selectIndex + 6, fromIndex).trim();
		                String[] ss = prefix.split("\\s*,\\s*");
		                for (String s : ss) {
		                    String[] sss = s.split("\\s+");
		                    pageHql.append(sss[sss.length - 1].trim());
		                    pageHql.append(".id, ");
		                }
		                int len = pageHql.length();
		                pageHql.delete(len - 2, len);
		            } else {
		                pageHql.append("id");
		            }
		
		            queryObject = session.createQuery(pageHql.toString());
		        } else {
		            queryObject = session.createQuery(hql);
		        }
		
		        if (values != null) {
		            for (int i = 0; i < values.length; i++) {
		                queryObject.setParameter(Integer.toString(i), values[i]);
		            }
		        }
		
		        queryObject.setFirstResult(startIndex);
		        queryObject.setMaxResults(endIndex - startIndex);
		
		        return queryObject.list();
			}
		});
    }

    /**
     * <pre>
	 * 根据指定的hql和参数列表，查询唯一对象
	 * 如果查询的结果多于1条,则抛出 NonUniqueResultException
	 * 	示例：
	 * 		String hql = "from Cust as cust where cust.id = ? ";
	 * 		Cust cust = dao.uniqueResult(hql, 1L);
	 * </pre>
     * 
     * @param <E>
     *            实体类型
     * @param hql
     *            指定的hql语句
     * @param values
     *            参数列表
     * @return E 唯一对象
     */
    public <E> E uniqueResult(final String hql, final Object... values) {
        return this.getHibernateTemplate().execute(new HibernateCallback<E>() {

            @SuppressWarnings("unchecked")
            public E doInHibernate(Session session) throws HibernateException {
                Query queryObject = session.createQuery(hql);
                if (values != null) {
                    for (int i = 0; i < values.length; i++) {
                        queryObject.setParameter(i, values[i]);
                    }
                }
                return (E) queryObject.uniqueResult();
            }
        });
    }

    /**
     * <pre>
	 * 根据指定的sql语句和sql参数列表，执行insert/delete/update操作的sql语句，返回受影响的行数
	 * 	示例：
	 * 		String sql = "update cust set name = ?, password = ? where id = ?"
	 * 		dao.executeSQL(sql, "007", "123456", 1);
	 * </pre>
     * 
     * @param sql
     *            sql语句
     * @param values
     *            sql参数列表
     * @return int 受影响的行数
     */
    public int executeSQL(final String sql, final Object... values) {

        return this.getHibernateTemplate().executeWithNativeSession(new HibernateCallback<Integer>() {

            public Integer doInHibernate(Session session) throws HibernateException {
                SQLQuery queryObject = session.createSQLQuery(sql);
                if (values != null) {
                    for (int i = 0; i < values.length; i++) {
                        queryObject.setParameter(i, values[i]);
                    }
                }
                return queryObject.executeUpdate();
            }
        });
    }

    /**
     * 获取JPA-style hql
     * 
     * @param hql
     *            position based hql
     * @param values
     *            参数
     * @return name based hql
     */
    private String getNamedHql(String hql, Object... values) {
        if (values == null || hql.contains("?0")) {
            return hql;
        }

        StringBuilder sb = new StringBuilder();
        int start = 0;
        for (int i = 0; i < values.length; i++) {
            int end = hql.indexOf('?', start);
            sb.append(hql.substring(start, end));
            sb.append('?');
            sb.append(i);

            start = end + 1;
        }
        sb.append(hql.substring(start, hql.length()));

        return sb.toString();
    }

}
