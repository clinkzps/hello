package com.newheight.scm.framework.dao.support;

import java.io.Serializable;

import com.newheight.scm.framework.dto.PageDTO;

/**
 * 为jqGrid查询提供的dao
 * @author xuepingjiao
 *
 */
public interface JqGridQueryDAO {
	public PageDTO query(@SuppressWarnings("rawtypes") Class entity, int startIndex, int pageSize, Serializable filter);
}
