/*
 * Copyright (c) 2017-2020 jdjr All Rights Reserved.
 * PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 * Author Email: wangzongyao@jd.com
 */

package resource.creator;

import org.springframework.aop.Advisor;
import org.springframework.aop.TargetSource;
import org.springframework.aop.aspectj.autoproxy.AspectJAwareAdvisorAutoProxyCreator;
import org.springframework.aop.framework.autoproxy.AbstractAutoProxyCreator;
import org.springframework.aop.framework.autoproxy.BeanFactoryAdvisorRetrievalHelper;
import org.springframework.aop.framework.autoproxy.ProxyCreationContext;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;

import java.util.List;

/**
 * 基于 Advisor 的 代理创建器. 创建器继承图如下: ↓
 * {@link org.springframework.aop.framework.autoproxy.AbstractAutoProxyCreator}
 *     {@link org.springframework.aop.framework.autoproxy.BeanNameAutoProxyCreator}: 基于BeanName的(允许通配符方式).
 *     {@link org.springframework.aop.framework.autoproxy.AbstractAdvisorAutoProxyCreator}: 基于 Advisor 的.
 *         {@link org.springframework.aop.framework.autoproxy.DefaultAdvisorAutoProxyCreator}
 *         {@link org.springframework.aop.framework.autoproxy.InfrastructureAdvisorAutoProxyCreator}
 *         {@link org.springframework.aop.aspectj.autoproxy.AspectJAwareAdvisorAutoProxyCreator}
 *             {@link org.springframework.aop.aspectj.annotation.AnnotationAwareAspectJAutoProxyCreator}
 * 当前类主要实现了 {@link #getAdvicesAndAdvisorsForBean(Class, String, TargetSource)}: 寻找合适的增强, 方法内部自己就进行了过滤.
 * @author wangzongyao on 2020/5/25
 */
public abstract class AbstractAdvisorAutoProxyCreator extends AbstractAutoProxyCreator {

    private BeanFactoryAdvisorRetrievalHelper advisorRetrievalHelper;


    /**
     * 重写了setBeanFactory方法, 保证bean工厂必须是ConfigurableListableBeanFactory.
     *
     *
     */
    @Override
    public void setBeanFactory(BeanFactory beanFactory) {
        super.setBeanFactory(beanFactory);
        if (!(beanFactory instanceof ConfigurableListableBeanFactory)) {
            throw new IllegalArgumentException(".........");
        }
        initBeanFactory((ConfigurableListableBeanFactory) beanFactory);
    }

    /**
     * 对 Helper 进行初始化: Advisor的寻找最终就是委托给了他.
     * BeanFactoryAdvisorRetrievalHelperAdapter 继承自 BeanFactoryAdvisorRetrievalHelper, 为私有内部类,
     * 主要重写了 isEligibleBean() 方法, 调用 .this.isEligibleAdvisorBean(beanName) 方法.
     */
    protected void initBeanFactory(ConfigurableListableBeanFactory beanFactory) {
        this.advisorRetrievalHelper = new BeanFactoryAdvisorRetrievalHelperAdapter(beanFactory);
    }


    /**
     * 找到作用到这个 Bean 的切点方法
     * 当然, 最终事委托给 {@link # BeanFactoryAdvisorRetrievalHelper} 去做的
     */
    @Override
    protected Object[] getAdvicesAndAdvisorsForBean(Class<?> beanClass, String beanName, TargetSource targetSource) {
        List<Advisor> advisors = findEligibleAdvisors(beanClass, beanName);
        return advisors.isEmpty() ? DO_NOT_PROXY : advisors.toArray();
    }

    /**
     * 找到有资格的. 流程分两步走: ↓
     * <ul>
     *     <li>获取全部的 Advisor: 内部调用 {@link #findCandidateAdvisors},委托给 {@link BeanFactoryAdvisorRetrievalHelper}.</li>
     *     <li>对 Advisor进行筛选, 去掉不匹配的.</li>
     *     <li>对筛选之后的 Advisor 进行排序, 返回.</li>
     * </ul>
     */
    protected List<Advisor> findEligibleAdvisors(Class<?> beanClass, String beanName) {
        /** 找出所有 Advisors. **/
        List<Advisor> candidateAdvisors = findCandidateAdvisors();

        /**
         * 对找到的 Advisors进行过滤操作, 看看 Advisor 能否被用在 Bean上(根据Advisor的PointCut判断).
         * 主要依赖于 {@link AopUtils#findAdvisorsThatCanApply(List, Class)} 方法. 逻辑简单概述为: ↓
         * 	 根据ClassFilter和MethodMatcher等等各种匹配。（但凡只有有一个方法被匹配上了，就会给他创建代理类了）
         * 	 方法用的 ReflectionUtils.getAllDeclaredMethods,
         * 	   因此 <b>哪怕是私有方法, 匹配上都会给创建的代理对象, 这点务必要特别特别的注意.</b>
         */
        List<Advisor> eligibleAdvisors = findAdvisorsThatCanApply(candidateAdvisors, beanClass, beanName);

        /** 拓展: 对eligibleAdvisors进行处理（增加/删除/修改等等） **/
        extendAdvisors(eligibleAdvisors);
        if (!eligibleAdvisors.isEmpty()) {
            /**
             * 默认排序方式: {@link AnnotationAwareOrderComparator#sort(List)}: 这个排序和Order接口有关.
             * 但是子类：{@link AspectJAwareAdvisorAutoProxyCreator} 有复写此排序方法，需要特别注意~~~
             */
            eligibleAdvisors = sortAdvisors(eligibleAdvisors);
        }
        return eligibleAdvisors;
    }

    protected List<Advisor> findCandidateAdvisors() {
        return this.advisorRetrievalHelper.findAdvisorBeans();
    }

    protected List<Advisor> findAdvisorsThatCanApply(List<Advisor> candidateAdvisors, Class<?> beanClass, String beanName) {
        ProxyCreationContext.setCurrentProxiedBeanName(beanName);
        try {
            return AopUtils.findAdvisorsThatCanApply(candidateAdvisors, beanClass);
        }
        finally {
            ProxyCreationContext.setCurrentProxiedBeanName(null);
        }
    }

    /** DefaultAdvisorAutoProxyCreator 和 InfrastructureAdvisorAutoProxyCreator 有复写. **/
    protected boolean isEligibleAdvisorBean(String beanName) {
        return true;
    }

    protected List<Advisor> sortAdvisors(List<Advisor> advisors) {
        AnnotationAwareOrderComparator.sort(advisors);
        return advisors;
    }

    protected void extendAdvisors(List<Advisor> candidateAdvisors) {
    }

    @Override
    protected boolean advisorsPreFiltered() {
        return true;
    }



    private class BeanFactoryAdvisorRetrievalHelperAdapter extends BeanFactoryAdvisorRetrievalHelper {

        public BeanFactoryAdvisorRetrievalHelperAdapter(ConfigurableListableBeanFactory beanFactory) {
            super(beanFactory);
        }

        @Override
        protected boolean isEligibleBean(String beanName) {
            return isEligibleAdvisorBean(beanName);
        }
    }

}
