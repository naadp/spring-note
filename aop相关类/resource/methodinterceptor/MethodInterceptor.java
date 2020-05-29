/*
 * Copyright (c) 2017-2020 jdjr All Rights Reserved.
 * PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 * Author Email: wangzongyao@jd.com
 */

package resource.methodinterceptor;

import org.aopalliance.aop.Advice;
import org.aopalliance.intercept.Interceptor;
import org.aopalliance.intercept.MethodInvocation;

/**
 * 最终执行的方法. {@link Advice} 会被适配成 {@link MethodInterceptor} 执行.
 * 个人以为是和Aop联盟的关系吧, 最后都转成了 {@link MethodInterceptor}.
 * @author wangzongyao on 2020/5/24
 */
public interface MethodInterceptor extends Interceptor {

    Object invoke(MethodInvocation invocation) throws Throwable;

}
