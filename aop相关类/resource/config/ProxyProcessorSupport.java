/*
 * Copyright (c) 2017-2020 jdjr All Rights Reserved.
 * PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 * Author Email: wangzongyao@jd.com
 */

package resource.config;

import org.springframework.aop.framework.AopInfrastructureBean;
import org.springframework.aop.framework.ProxyConfig;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.Aware;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;

import java.io.Closeable;

/**
 * 继承 ProxyConfig, 标志着它也可以是一个配置.
 * 为代理创建器提供了一些公共方法实现(因为代理创建器会继承它).
 * AopInfrastructureBean: 标识性接口, 表示当前类是AOP的基础组件, 该类不应该被AOP代理, 即使能被切面切进去.
 * @author wangzongyao on 2020/5/24
 */
public class ProxyProcessorSupport extends ProxyConfig implements Ordered, BeanClassLoaderAware, AopInfrastructureBean {

    /**
     * AOP的自动代理创建器必须在其他的别的processors之后执行, 以确保它可以代理到所有的小伙伴们(即使对方也是后置处理器)
     * 后置处理器的执行流程可以去看下:
     *     {@link AbstractApplicationContext#refresh()}, refresh()内部的 registerBeanPostProcessors().
     */
    private int order = Ordered.LOWEST_PRECEDENCE;

    private ClassLoader proxyClassLoader = ClassUtils.getDefaultClassLoader();

    private boolean classLoaderConfigured = false;

    public void setOrder(int order) {
        this.order = order;
    }

    @Override
    public int getOrder() {
        return this.order;
    }

    public void setProxyClassLoader(ClassLoader classLoader) {
        this.proxyClassLoader = classLoader;
        this.classLoaderConfigured = (classLoader != null);
    }

    protected ClassLoader getProxyClassLoader() {
        return this.proxyClassLoader;
    }

    @Override
    public void setBeanClassLoader(ClassLoader classLoader) {
        if (!this.classLoaderConfigured) {
            this.proxyClassLoader = classLoader;
        }
    }

    /**
     * 最为核心的方法: 这里决定了如果目标类没有实现接口, 那就直接是Cglib代理.
     * 	检查给定beanClass上的接口们，并交给proxyFactory处理.
     */
    protected void evaluateProxyInterfaces(Class<?> beanClass, ProxyFactory proxyFactory) {
        /** 找到该类实现的所有接口. **/
        Class<?>[] targetInterfaces = ClassUtils.getAllInterfacesForClass(beanClass, getProxyClassLoader());
        /** 标记: 是否有存在【合理的】接口. **/
        boolean hasReasonableProxyInterface = false;
        for (Class<?> ifc : targetInterfaces) {
            /** 不是回调的接口、{@link #isInternalLanguageInterface(Class)}、接口内部还要有方法, 不能是标识性接口. **/
            if (!isConfigurationCallbackInterface(ifc) && !isInternalLanguageInterface(ifc) && ifc.getMethods().length > 0) {
                hasReasonableProxyInterface = true;
                break;
            }
        }
        if (hasReasonableProxyInterface) {
            /** Spring Doc 特别强调: 不能值只把合理的接口设置进去, 而是都得加入进去. **/
            for (Class<?> ifc : targetInterfaces) {
                proxyFactory.addInterface(ifc);
            }
        }
        else {
            /** 这个很明显设置true，表示使用CGLIB得方式去创建代理了. **/
            proxyFactory.setProxyTargetClass(true);
        }
    }

    /**
     * 判断此接口类型是否属于 容器去回调的类型: 初始化、销毁、自动刷新、自动关闭、Aware感知等等
     * InitializingBean、DisposableBean、Closeable、AutoCloseable、Aware系列
     */
    protected boolean isConfigurationCallbackInterface(Class<?> ifc) {
        return (InitializingBean.class == ifc || DisposableBean.class == ifc ||
                Closeable.class == ifc || "java.lang.AutoCloseable".equals(ifc.getName()) ||
                ObjectUtils.containsElement(ifc.getInterfaces(), Aware.class));
    }

    /** 是否是如下通用的接口。若实现的是这些接口也会排除，不认为它是实现了接口的类. **/
    protected boolean isInternalLanguageInterface(Class<?> ifc) {
        return (ifc.getName().equals("groovy.lang.GroovyObject") ||
                ifc.getName().endsWith(".cglib.proxy.Factory") ||
                ifc.getName().endsWith(".bytebuddy.MockAccess"));
    }

}
