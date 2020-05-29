/*
 * Copyright (c) 2017-2020 jdjr All Rights Reserved.
 * PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 * Author Email: wangzongyao@jd.com
 */

package resource.util.aspect;

import org.aspectj.lang.reflect.PerClauseKind;
import org.springframework.aop.Advisor;
import org.springframework.aop.aspectj.annotation.*;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.util.Assert;
import resource.util.advisor.BeanFactoryAdvisorRetrievalHelper;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 帮助 {@link AnnotationAwareAspectJAutoProxyCreator} 查找加了@Aspect注解的类, 并解析出其中的增强.
 * @see BeanFactoryAdvisorRetrievalHelper 这两个类是非常很像的, 无论是作用, 还是检验 eligible 的 Bean 的方式.
 * @see AnnotationAwareAspectJAutoProxyCreator
 * @author wangzongyao on 2020/5/25
 */
@SuppressWarnings("all")
public class BeanFactoryAspectJAdvisorsBuilder {

    private final ListableBeanFactory beanFactory;

    private final AspectJAdvisorFactory advisorFactory;

    /** 保存加了 @Aspect注解 的 Bean. **/
    private volatile List<String> aspectBeanNames;

    /**
     * 缓存: ↓
     *   K: 加了 @Aspect 注解的 Bean 的 beanName
     *   V: 从这个 Bean 中解析出来的 Advisor列表
     */
    private final Map<String, List<Advisor>> advisorsCache = new ConcurrentHashMap<>();

    private final Map<String, MetadataAwareAspectInstanceFactory> aspectFactoryCache = new ConcurrentHashMap<>();

    public BeanFactoryAspectJAdvisorsBuilder(ListableBeanFactory beanFactory) {
        this(beanFactory, new ReflectiveAspectJAdvisorFactory(beanFactory));
    }

    public BeanFactoryAspectJAdvisorsBuilder(ListableBeanFactory beanFactory, AspectJAdvisorFactory advisorFactory) {
        Assert.notNull(beanFactory, "ListableBeanFactory must not be null");
        Assert.notNull(advisorFactory, "AspectJAdvisorFactory must not be null");
        this.beanFactory = beanFactory;
        this.advisorFactory = advisorFactory;
    }

    /**
     * 《Spring源码深度解析—第四版》177页码 上有讲解此方法.
     *
     *
     */
    public List<Advisor> buildAspectJAdvisors() {
        List<String> aspectNames = this.aspectBeanNames;

        if (aspectNames == null) {
            synchronized (this) {
                aspectNames = this.aspectBeanNames;
                if (aspectNames == null) {
                    /** 从这里开始, 从IoC中拿到全部BeanName, 并拿到其类型. **/
                    List<Advisor> advisors = new LinkedList<>();
                    aspectNames = new LinkedList<String>();
                    String[] beanNames = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(
                            this.beanFactory, Object.class, true, false);
                    for (String beanName : beanNames) {
                        if (!isEligibleBean(beanName)) {
                            continue;
                        }
                        Class<?> beanType = this.beanFactory.getType(beanName);
                        if (beanType == null) {
                            continue;
                        }
                        /**
                         * 筛选出类中加 @Aspect注解 的 Bean.
                         * 根据 type 和 beanName 创建 {@link AspectMetadata}.
                         * isAspect(): 类(或父类)上要加了@Aspect注解或者 属性名不是以非 "ajc$"开头的, 估计是Aspect编译器.
                         */
                        if (this.advisorFactory.isAspect(beanType)) {
                            aspectNames.add(beanName);
                            AspectMetadata amd = new AspectMetadata(beanType, beanName);
                            if (amd.getAjType().getPerClause().getKind() == PerClauseKind.SINGLETON) {
                                MetadataAwareAspectInstanceFactory factory = new BeanFactoryAspectInstanceFactory(this.beanFactory, beanName);
                                List<Advisor> classAdvisors = this.advisorFactory.getAdvisors(factory);
                                if (this.beanFactory.isSingleton(beanName)) {
                                    this.advisorsCache.put(beanName, classAdvisors);
                                }
                                else {
                                    this.aspectFactoryCache.put(beanName, factory);
                                }
                                advisors.addAll(classAdvisors);
                            }
                            else {
                                // Per target or per this.
                                if (this.beanFactory.isSingleton(beanName)) {
                                    throw new IllegalArgumentException("Bean with name '" + beanName +
                                            "' is a singleton, but aspect instantiation model is not singleton");
                                }
                                MetadataAwareAspectInstanceFactory factory =
                                        new PrototypeAspectInstanceFactory(this.beanFactory, beanName);
                                this.aspectFactoryCache.put(beanName, factory);
                                advisors.addAll(this.advisorFactory.getAdvisors(factory));
                            }
                        }
                    }
                    this.aspectBeanNames = aspectNames;
                    return advisors;
                }
            }
        }

        if (aspectNames.isEmpty()) {
            return Collections.emptyList();
        }
        /** 最终返回的结果. **/
        List<Advisor> advisors = new LinkedList<>();
        for (String aspectName : aspectNames) {
            List<Advisor> cachedAdvisors = this.advisorsCache.get(aspectName);
            if (cachedAdvisors != null) {
                advisors.addAll(cachedAdvisors);
            }
            else {
                MetadataAwareAspectInstanceFactory factory = this.aspectFactoryCache.get(aspectName);
                advisors.addAll(this.advisorFactory.getAdvisors(factory));
            }
        }
        return advisors;
    }

    /**
     * 很像.
     *
     */
    protected boolean isEligibleBean(String beanName) {
        return true;
    }

}
