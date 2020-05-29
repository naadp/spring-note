/*
 * Copyright (c) 2017-2020 jdjr All Rights Reserved.
 * PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 * Author Email: wangzongyao@jd.com
 */

package resource.adapter;

import org.aopalliance.aop.Advice;
import org.aopalliance.intercept.MethodInterceptor;
import org.springframework.aop.Advisor;
import org.springframework.aop.framework.adapter.*;
import org.springframework.aop.support.DefaultPointcutAdvisor;

import java.util.ArrayList;
import java.util.List;

/**
 * 这里类里面存在着
 *     MethodBeforeAdviceAdapter,
 *     AfterReturningAdviceAdapter,
 *     ThrowsAdviceAdapter
 * 这三个类是将 Advice 适配成 MethodInterceptor 的适配类.
 * 两个作用:
 *     1. 将 Advisor 中的 Advice 封装成 MethodInterceptor.
 *     2. 将 MethodInterceptor 或者 能转成 MethodInterceptor的Advice, 封装成 {@link DefaultPointcutAdvisor}.
 * 备注: MethodInterceptor 接口 继承了 Advice接口.
 * @author wangzongyao on 2020/5/24
 */
public class DefaultAdvisorAdapterRegistry {
    private final List<AdvisorAdapter> adapters = new ArrayList<>(3);

    public DefaultAdvisorAdapterRegistry() {
        registerAdvisorAdapter(new MethodBeforeAdviceAdapter());
        registerAdvisorAdapter(new AfterReturningAdviceAdapter());
        registerAdvisorAdapter(new ThrowsAdviceAdapter());
    }

    public MethodInterceptor[] getInterceptors(Advisor advisor) throws UnknownAdviceTypeException {
        List<MethodInterceptor> interceptors = new ArrayList<MethodInterceptor>(3);
        Advice advice = advisor.getAdvice();
        if (advice instanceof MethodInterceptor) {
            interceptors.add((MethodInterceptor) advice);
        }
        for (AdvisorAdapter adapter : this.adapters) {
            if (adapter.supportsAdvice(advice)) {
                interceptors.add(adapter.getInterceptor(advisor));
            }
        }
        if (interceptors.isEmpty()) {
            throw new UnknownAdviceTypeException(advisor.getAdvice());
        }
        return interceptors.toArray(new MethodInterceptor[interceptors.size()]);
    }

    public org.springframework.aop.Advisor wrap(Object adviceObject) throws UnknownAdviceTypeException {
        if (adviceObject instanceof org.springframework.aop.Advisor) {
            return (org.springframework.aop.Advisor) adviceObject;
        }
        if (!(adviceObject instanceof Advice)) {
            throw new UnknownAdviceTypeException(adviceObject);
        }
        Advice advice = (Advice) adviceObject;
        if (advice instanceof MethodInterceptor) {
            // So well-known it doesn't even need an adapter.
            return new DefaultPointcutAdvisor(advice);
        }
        for (AdvisorAdapter adapter : this.adapters) {
            // Check that it is supported.
            if (adapter.supportsAdvice(advice)) {
                return new DefaultPointcutAdvisor(advice);
            }
        }
        throw new UnknownAdviceTypeException(advice);
    }



    public void registerAdvisorAdapter(AdvisorAdapter adapter) {
        this.adapters.add(adapter);
    }
}
