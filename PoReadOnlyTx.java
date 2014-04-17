package com.newheight.scm.framework.dao.support;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.transaction.annotation.Transactional;
/**
 * 该标记用于简化对PMS数据库@Transactional的配置，将事务标志为readOnly，当需要更复杂的配置时，可以在应用中直接使用@Transactional
 * @author xuepingjiao
 * @see Transactional
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Transactional(value=DBQualifiers.PO_TX, readOnly=true)
public @interface PoReadOnlyTx {
}
