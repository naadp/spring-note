/*
 * Copyright (c) 2017-2020 jdjr All Rights Reserved.
 * PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 * Author Email: wangzongyao@jd.com
 */

package resource.advised.util;

import org.springframework.aop.framework.Advised;
import org.aopalliance.intercept.MethodInterceptor;
import java.lang.reflect.Method;
import java.util.List;

/**
 * @author wangzongyao on 2020/5/28
 */
public interface AdvisorChainFactory {

    /**
     * 返回 {@link MethodInterceptor} 列表.
     */
    List<Object> getInterceptorsAndDynamicInterceptionAdvice(Advised config, Method method, Class<?> targetClass);

}
