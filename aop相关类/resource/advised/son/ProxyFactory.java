/*
 * Copyright (c) 2017-2020 jdjr All Rights Reserved.
 * PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 * Author Email: wangzongyao@jd.com
 */

package resource.advised.son;

import org.aopalliance.intercept.Interceptor;
import org.springframework.aop.TargetSource;
import org.springframework.aop.framework.ProxyCreatorSupport;
import org.springframework.util.ClassUtils;

/**
 * @author wangzongyao on 2020/5/28
 */
public class ProxyFactory extends ProxyCreatorSupport {


    public ProxyFactory() {
    }

    public ProxyFactory(Object target) {
        setTarget(target);
        setInterfaces(ClassUtils.getAllInterfaces(target));
    }

    public ProxyFactory(Class<?>... proxyInterfaces) {
        setInterfaces(proxyInterfaces);
    }

    /**
     * Create a new ProxyFactory f handles all calls itself rather than
     * delegating to a target, like in the case of remoting proxies.
     * @param proxyInterface the interface that the proxy should implement
     * @param interceptor the interceptor that the proxy should invoke
     */
    public ProxyFactory(Class<?> proxyInterface, Interceptor interceptor) {
        addInterface(proxyInterface);
        addAdvice(interceptor);
    }

    /**
     * Create a ProxyFactory for the specified {@code TargetSource},
     * making the proxy implement the specified interface.
     * @param proxyInterface the interface that the proxy should implement
     * @param targetSource the TargetSource that the proxy should invoke
     */
    public ProxyFactory(Class<?> proxyInterface, TargetSource targetSource) {
        addInterface(proxyInterface);
        setTargetSource(targetSource);
    }


    /**
     * Create a new proxy according to the settings in this factory.
     * <p>Can be called repeatedly. Effect will vary if we've added
     * or removed interfaces. Can add and remove interceptors.
     * <p>Uses a default class loader: Usually, the thread context class loader
     * (if necessary for proxy creation).
     * @return the proxy object
     */
    public Object getProxy() {
        return createAopProxy().getProxy();
    }

    /**
     * Create a new proxy according to the settings in this factory.
     * <p>Can be called repeatedly. Effect will vary if we've added
     * or removed interfaces. Can add and remove interceptors.
     * <p>Uses the given class loader (if necessary for proxy creation).
     * @param classLoader the class loader to create the proxy with
     * (or {@code null} for the low-level proxy facility's default)
     * @return the proxy object
     */
    public Object getProxy(ClassLoader classLoader) {
        return createAopProxy().getProxy(classLoader);
    }


    /**
     * Create a new proxy for the given interface and interceptor.
     * <p>Convenience method for creating a proxy for a single interceptor,
     * assuming that the interceptor handles all calls itself rather than
     * delegating to a target, like in the case of remoting proxies.
     * @param proxyInterface the interface that the proxy should implement
     * @param interceptor the interceptor that the proxy should invoke
     * @return the proxy object
     * @see #ProxyFactory(Class, org.aopalliance.intercept.Interceptor)
     */
    @SuppressWarnings("unchecked")
    public static <T> T getProxy(Class<T> proxyInterface, Interceptor interceptor) {
        return (T) new org.springframework.aop.framework.ProxyFactory(proxyInterface, interceptor).getProxy();
    }

    /**
     * Create a proxy for the specified {@code TargetSource},
     * implementing the specified interface.
     * @param proxyInterface the interface that the proxy should implement
     * @param targetSource the TargetSource that the proxy should invoke
     * @return the proxy object
     * @see #ProxyFactory(Class, org.springframework.aop.TargetSource)
     */
    @SuppressWarnings("unchecked")
    public static <T> T getProxy(Class<T> proxyInterface, TargetSource targetSource) {
        return (T) new org.springframework.aop.framework.ProxyFactory(proxyInterface, targetSource).getProxy();
    }

    /**
     * Create a proxy for the specified {@code TargetSource} that extends
     * the target class of the {@code TargetSource}.
     * @param targetSource the TargetSource that the proxy should invoke
     * @return the proxy object
     */
    public static Object getProxy(TargetSource targetSource) {
        if (targetSource.getTargetClass() == null) {
            throw new IllegalArgumentException("Cannot create class proxy for TargetSource with null target class");
        }
        org.springframework.aop.framework.ProxyFactory proxyFactory = new org.springframework.aop.framework.ProxyFactory();
        proxyFactory.setTargetSource(targetSource);
        proxyFactory.setProxyTargetClass(true);
        return proxyFactory.getProxy();
    }

}
