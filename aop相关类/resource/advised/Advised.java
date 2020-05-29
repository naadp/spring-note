/*
 * Copyright (c) 2017-2020 jdjr All Rights Reserved.
 * PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 * Author Email: wangzongyao@jd.com
 */

package resource.advised;

import org.aopalliance.aop.Advice;
import org.springframework.aop.Advisor;
import org.springframework.aop.TargetClassAware;
import org.springframework.aop.TargetSource;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.aop.framework.AopConfigException;
import org.springframework.aop.framework.*;
import org.springframework.beans.factory.FactoryBean;

/**
 * 查看、修改 代理的配置、增强、Advisor
 *
 * 不管是JDK proxy, 还是Cglib proxy, 代理出来的对象都实现了 {@link Advised} 接口.
 * 它的主要实现，就是面向我们创建代理的，非常实用:
 * <ul>
 *     <li>
 *         {@link ProxyFactory}: 编程式 AOP 中最常用的对象.
 *             这个类通过构造函数中的 proxyInterface/interceptor/targetSource 来创建代理对象.
 *     </li>
 *     <li>
 *         {@link ProxyFactoryBean}:
 *             这个类是基于 {@link FactoryBean} 的 Proxy 创建形式,
 *             其通过代理的 Interface, targetSource 与指定的 interceptorNames 来创建对应的AopProxy, 最后生成对应的代理对象.
 *     </li>
 *     <li>
 *         {@link AspectJProxyFactory}: 这个类现在已经很少用了, 但是@Aspect方式常用哦
 *             将一个被 @Aspect 注解标示的类丢入其中, 便创建了对应的代理对象.
 *     </li>
 * </ul>
 *
 *
 *
 *
 *
 *
 *
 *
 *
 * @see org.springframework.aop.framework.Advised
 * @author wangzongyao on 2020/5/24
 */
@SuppressWarnings("all")
public interface Advised extends TargetClassAware {

    /** 这个 frozen 决定是否 AdvisedSupport 里面配置的信息是否改变 .**/
    boolean isFrozen();

    /** 是否代理指定的类, 而不是一些 Interface .**/
    boolean isProxyTargetClass();

    /** 返回代理的接口 .**/
    Class<?>[] getProxiedInterfaces();

    /** 判断这个接口是否是被代理的接口 .**/
    boolean isInterfaceProxied(Class<?> intf);

    /** 设置代理的目标对象 .**/
    void setTargetSource(TargetSource targetSource);

    /** 获取代理的对象 .**/
    TargetSource getTargetSource();

    /** 判断是否需要将 代理的对象暴露到 ThreadLocal中, 而获取对应的代理对象则通过 AopContext 获取 .**/
    void setExposeProxy(boolean exposeProxy);

    /** 返回是否应该暴露 代理对象 .**/
    boolean isExposeProxy();

    /** 设置 Advisor 是否已经在前面过滤过是否匹配 Pointcut (极少用到) .**/
    void setPreFiltered(boolean preFiltered);

    /** 获取 Advisor 是否已经在前面过滤过是否匹配 Pointcut (极少用到) .**/
    boolean isPreFiltered();

    /** 获取所有的 Advisor .**/
    Advisor[] getAdvisors();

    /** 增加 Advisor 到链表的最后 .**/
    void addAdvisor(Advisor advisor) throws AopConfigException;

    /** 在指定位置增加 Advisor .**/
    void addAdvisor(int pos, Advisor advisor) throws AopConfigException;

    /** 删除指定的 Advisor .**/
    boolean removeAdvisor(Advisor advisor);

    /** 删除指定位置的 Advisor .**/
    void removeAdvisor(int index) throws AopConfigException;

    /** 返回 Advisor 所在位置de index .**/
    int indexOf(Advisor advisor);

    /** 将指定的两个 Advisor 进行替换 .**/
    boolean replaceAdvisor(Advisor a, Advisor b) throws AopConfigException;

    /** 增加 Advice <- 这个Advice将会包裹成 DefaultPointcutAdvisor .**/
    void addAdvice(Advice advice) throws AopConfigException;

    /** 在指定 index 增加 Advice <- 这个Advice将会包裹成 DefaultPointcutAdvisor .**/
    void addAdvice(int pos, Advice advice) throws AopConfigException;

    /** 删除给定的 Advice .**/
    boolean removeAdvice(Advice advice);

    /** 获取 Advice 的索引位置 .**/
    int indexOf(Advice advice);

    /** 将 ProxyConfig 通过 String 形式返回 .**/
    String toProxyConfigString();
    
}
