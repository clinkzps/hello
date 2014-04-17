package com.newheight.scm.framework.dao.support;

import java.io.Serializable;

import org.hibernate.EmptyInterceptor;
import org.hibernate.type.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExtendedInterceptor extends EmptyInterceptor {

	private static final long serialVersionUID = 1L;
	private Logger logger = LoggerFactory.getLogger(getClass());

	/* (non-Javadoc)
	 * @see org.hibernate.EmptyInterceptor#onFlushDirty(java.lang.Object, java.io.Serializable, java.lang.Object[], java.lang.Object[], java.lang.String[], org.hibernate.type.Type[])
	 */
	@Override
	public boolean onFlushDirty(Object entity, Serializable id,
			Object[] currentState, Object[] previousState,
			String[] propertyNames, Type[] types) {
		/*Object identity = ActionContext.getContext().getSession().get("login.user");
		if (identity != null) {
			 for ( int i=0; i < propertyNames.length; i++ ) {
	                if ( "updatedBy".equals( propertyNames[i] ) ) {
	                    currentState[i] = "junit test";//TODO;
	                }
	                if ( "updatedAt".equals( propertyNames[i] ) ) {
	                    currentState[i] = DateUtil.getNowTime();
	                }
			 }
			 return true;
		}*/
		
		return super.onFlushDirty(entity, id, currentState, previousState, propertyNames, types);
	}

	/* (non-Javadoc)
	 * @see org.hibernate.EmptyInterceptor#onSave(java.lang.Object, java.io.Serializable, java.lang.Object[], java.lang.String[], org.hibernate.type.Type[])
	 */
	@Override
	public boolean onSave(Object entity, Serializable id, Object[] state,
			String[] propertyNames, Type[] types) {
		logger.debug("onSave");
		/*Object identity = ActionContext.getContext().getSession().get("login.user");
		if (identity != null) {
			for (int i = 0; i < propertyNames.length; i++) {
				if ("createdBy".equals(propertyNames[i])) {
					state[i] = "";//TODO;
				}
				if ("createdAt".equals(propertyNames[i])) {
					state[i] = DateUtil.getNowTime();
				}
				if ( "updatedBy".equals( propertyNames[i] ) ) {
					state[i] = "";//TODO;
                }
                if ( "updatedAt".equals( propertyNames[i] ) ) {
                	state[i] = DateUtil.getNowTime();
                }
			}
		}*/
		return super.onSave(entity, id, state, propertyNames, types);
	}
}
