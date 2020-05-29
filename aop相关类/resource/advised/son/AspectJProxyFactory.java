/*
 * Copyright (c) 2017-2020 jdjr All Rights Reserved.
 * PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 * Author Email: wangzongyao@jd.com
 */

package resource.advised.son;

import org.aspectj.lang.reflect.PerClauseKind;
import org.springframework.aop.Advisor;
import org.springframework.aop.aspectj.AspectJProxyUtils;
import org.springframework.aop.aspectj.annotation.*;
import org.springframework.aop.framework.AdvisedSupport;
import org.springframework.aop.framework.AopConfigException;
import org.springframework.aop.framework.ProxyCreatorSupport;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Object
 *   ProxyConfig
 *     {@link AdvisedSupport}:
 *       ProxyCreatorSupport
 *         ProxyFactoryBean: 基于 FactoryBean, 可以将AOP和IOC融合起来.
 *         ProxyFactory: 基于硬编码, 一般是Spring自己用.
 *         AspectJProxyFactory: 这个牛逼.
 * <-------------------------------------->
 * {@link AdvisedSupport}
 *     1. 配置当前代理的Adivsiors
 *     2. 配置当前代理的目标对象
 *     3. 配置当前代理的接口
 *     4. 提供getInterceptorsAndDynamicInterceptionAdvice方法用来获取对应代理方法对应有效的拦截器链
 * <-------------------------------------->
 *
 * <-------------------------------------->
 *
 * <-------------------------------------->
 *
 * <-------------------------------------->
 *
 * <-------------------------------------->
 * @author wangzongyao on 2020/5/25
 */
public class AspectJProxyFactory extends ProxyCreatorSupport {

    /** Cache for singleton aspect instances */
    private static final Map<Class<?>, Object> aspectCache = new ConcurrentHashMap<Class<?>, Object>();

    private final AspectJAdvisorFactory aspectFactory = new ReflectiveAspectJAdvisorFactory();


    /**
     * Create a new AspectJProxyFactory.
     */
    public AspectJProxyFactory() {
    }

    /**
     * Create a new AspectJProxyFactory.
     * <p>Will proxy all interfaces that the given target implements.
     * @param target the target object to be proxied
     */
    public AspectJProxyFactory(Object target) {
        Assert.notNull(target, "Target object must not be null");
        setInterfaces(ClassUtils.getAllInterfaces(target));
        setTarget(target);
    }

    /**
     * Create a new {@code AspectJProxyFactory}.
     * No target, only interfaces. Must add interceptors.
     */
    public AspectJProxyFactory(Class<?>... interfaces) {
        setInterfaces(interfaces);
    }


    /**
     * Add the supplied aspect instance to the chain. The type of the aspect instance
     * supplied must be a singleton aspect. True singleton lifecycle is not honoured when
     * using this method - the caller is responsible for managing the lifecycle of any
     * aspects added in this way.
     * @param aspectInstance the AspectJ aspect instance
     */
    public void addAspect(Object aspectInstance) {
        Class<?> aspectClass = aspectInstance.getClass();
        String aspectName = aspectClass.getName();
        AspectMetadata am = createAspectMetadata(aspectClass, aspectName);
        if (am.getAjType().getPerClause().getKind() != PerClauseKind.SINGLETON) {
            throw new IllegalArgumentException(
                    "Aspect class [" + aspectClass.getName() + "] does not define a singleton aspect");
        }
        addAdvisorsFromAspectInstanceFactory(
                new SingletonMetadataAwareAspectInstanceFactory(aspectInstance, aspectName));
    }

    /**
     * Add an aspect of the supplied type to the end of the advice chain.
     * @param aspectClass the AspectJ aspect class
     */
    public void addAspect(Class<?> aspectClass) {
        String aspectName = aspectClass.getName();
        AspectMetadata am = createAspectMetadata(aspectClass, aspectName);
        MetadataAwareAspectInstanceFactory instanceFactory = createAspectInstanceFactory(am, aspectClass, aspectName);
        addAdvisorsFromAspectInstanceFactory(instanceFactory);
    }


    /**
     * Add all {@link Advisor Advisors} from the supplied {@link MetadataAwareAspectInstanceFactory}
     * to the current chain. Exposes any special purpose {@link Advisor Advisors} if needed.
     * @see AspectJProxyUtils#makeAdvisorChainAspectJCapableIfNecessary(List)
     */
    private void addAdvisorsFromAspectInstanceFactory(MetadataAwareAspectInstanceFactory instanceFactory) {
        List<Advisor> advisors = this.aspectFactory.getAdvisors(instanceFactory);
        advisors = AopUtils.findAdvisorsThatCanApply(advisors, getTargetClass());
        AspectJProxyUtils.makeAdvisorChainAspectJCapableIfNecessary(advisors);
        AnnotationAwareOrderComparator.sort(advisors);
        addAdvisors(advisors);
    }

    /**
     * Create an {@link AspectMetadata} instance for the supplied aspect type.
     */
    private AspectMetadata createAspectMetadata(Class<?> aspectClass, String aspectName) {
        AspectMetadata am = new AspectMetadata(aspectClass, aspectName);
        if (!am.getAjType().isAspect()) {
            throw new IllegalArgumentException("Class [" + aspectClass.getName() + "] is not a valid aspect type");
        }
        return am;
    }

    /**
     * Create a {@link MetadataAwareAspectInstanceFactory} for the supplied aspect type. If the aspect type
     * has no per clause, then a {@link SingletonMetadataAwareAspectInstanceFactory} is returned, otherwise
     * a {@link PrototypeAspectInstanceFactory} is returned.
     */
    private MetadataAwareAspectInstanceFactory createAspectInstanceFactory(
            AspectMetadata am, Class<?> aspectClass, String aspectName) {

        MetadataAwareAspectInstanceFactory instanceFactory;
        if (am.getAjType().getPerClause().getKind() == PerClauseKind.SINGLETON) {
            // Create a shared aspect instance.
            Object instance = getSingletonAspectInstance(aspectClass);
            instanceFactory = new SingletonMetadataAwareAspectInstanceFactory(instance, aspectName);
        }
        else {
            // Create a factory for independent aspect instances.
            instanceFactory = new SimpleMetadataAwareAspectInstanceFactory(aspectClass, aspectName);
        }
        return instanceFactory;
    }

    /**
     * Get the singleton aspect instance for the supplied aspect type. An instance
     * is created if one cannot be found in the instance cache.
     */
    private Object getSingletonAspectInstance(Class<?> aspectClass) {
        // Quick check without a lock...
        Object instance = aspectCache.get(aspectClass);
        if (instance == null) {
            synchronized (aspectCache) {
                // To be safe, check within full lock now...
                instance = aspectCache.get(aspectClass);
                if (instance == null) {
                    try {
                        instance = aspectClass.newInstance();
                        aspectCache.put(aspectClass, instance);
                    }
                    catch (InstantiationException ex) {
                        throw new AopConfigException(
                                "Unable to instantiate aspect class: " + aspectClass.getName(), ex);
                    }
                    catch (IllegalAccessException ex) {
                        throw new AopConfigException(
                                "Could not access aspect constructor: " + aspectClass.getName(), ex);
                    }
                }
            }
        }
        return instance;
    }


    /**
     * Create a new proxy according to the settings in this factory.
     * <p>Can be called repeatedly. Effect will vary if we've added
     * or removed interfaces. Can add and remove interceptors.
     * <p>Uses a default class loader: Usually, the thread context class loader
     * (if necessary for proxy creation).
     * @return the new proxy
     */
    @SuppressWarnings("unchecked")
    public <T> T getProxy() {
        return (T) createAopProxy().getProxy();
    }

    /**
     * Create a new proxy according to the settings in this factory.
     * <p>Can be called repeatedly. Effect will vary if we've added
     * or removed interfaces. Can add and remove interceptors.
     * <p>Uses the given class loader (if necessary for proxy creation).
     * @param classLoader the class loader to create the proxy with
     * @return the new proxy
     */
    @SuppressWarnings("unchecked")
    public <T> T getProxy(ClassLoader classLoader) {
        return (T) createAopProxy().getProxy(classLoader);
    }

}
