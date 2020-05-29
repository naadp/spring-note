/*
 * Copyright (c) 2017-2020 jdjr All Rights Reserved.
 * PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 * Author Email: wangzongyao@jd.com
 */

package resource.methodinvocation;

import org.aopalliance.intercept.Invocation;
import org.aopalliance.intercept.Joinpoint;
import org.aopalliance.intercept.MethodInterceptor;
import org.springframework.aop.ProxyMethodInvocation;
import org.springframework.aop.framework.*;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Method;

/**
 * 调用 目标方法 以及 {@link MethodInterceptor} 的类. 它是逻辑组织类, {@link MethodInterceptor} 与 目标方法的调用逻辑 就是由他来完成的.
 * 主要分成
 *     JDK动态代理 的 {@link ReflectiveMethodInvocation}, 与用于
 *     Cglib 的 {@link CglibMethodInvocation}.
 * 它继承了 {@link Invocation}, {@link Invocation} 又继承了 {@link Joinpoint}
 * @author wangzongyao on 2020/5/24
 */
public interface MethodInvocation {

    /**
     * 当前类的方法: 获取目标方法.
     */
    Method getMethod();

    /**
     * {@link Invocation} 的方法.
     */
    Object[] getArguments();

    /** 以下为 {@link Joinpoint} 的方法. **/

    /**
     * 执行当前 {@link MethodInvocation} 的逻辑, 例如 {@link ReflectiveMethodInvocation}.
     */
    Object proceed() throws Throwable;

    /**
     * 获取目标对象.
     * @see ReflectiveMethodInvocation#getProxy(): 获取代理.
     * @see ReflectiveMethodInvocation#getMethod(): 目标方法.
     * @see ProxyMethodInvocation
     * {@link ReflectiveMethodInvocation} 实现 了 {@link ProxyMethodInvocation}
     */
    Object getThis();

    /**
     * 不知道啥意思. 哈哈哈 O(∩_∩)O~
     */
    AccessibleObject getStaticPart();

}
