/*
 * Copyright (c) 2017-2020 jdjr All Rights Reserved.
 * PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 * Author Email: wangzongyao@jd.com
 */

package resource.util.advisor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.aop.Advisor;
import org.springframework.aop.framework.autoproxy.*;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.util.Assert;
import org.springframework.aop.aspectj.autoproxy.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 帮助 {@link AspectJAwareAdvisorAutoProxyCreator} 从 IoC 中查找 Advisor.class 类型的通知.
 * @author wangzongyao on 2020/5/25
 */
public class BeanFactoryAdvisorRetrievalHelper {

    private static final Log logger = LogFactory.getLog(org.springframework.aop.framework.autoproxy.BeanFactoryAdvisorRetrievalHelper.class);

    private final ConfigurableListableBeanFactory beanFactory;

    /**
     * 缓存: 存储所有从 Ioc 中根据 Advisor.class 类型拿到的所有的 BeanName.
     * 注意: 是所有的啊.
     */
    private volatile String[] cachedAdvisorBeanNames;

    public BeanFactoryAdvisorRetrievalHelper(ConfigurableListableBeanFactory beanFactory) {
        Assert.notNull(beanFactory, "ListableBeanFactory must not be null");
        this.beanFactory = beanFactory;
    }

    /**
     * 核心方法.
     */
    public List<Advisor> findAdvisorBeans() {
        // Determine list of advisor bean names, if not cached already.
        String[] advisorNames = this.cachedAdvisorBeanNames;
        if (advisorNames == null) {
            /**
             * 也会从祖先容器中拿到 Advisor类型 的 BeanName, 但是不会去实例化它们.
             * 而是先判断是否合格: {@link #isEligibleBean(String)}, 只有合格的才会再通过 {@link BeanFactory#getBean(String)} 去实例化.
             * 因为不合格的你在这里实例化了也没有, 那你实例化它干啥呢 ?
             */
            advisorNames = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(
                    this.beanFactory, Advisor.class, true, false);
            this.cachedAdvisorBeanNames = advisorNames;
        }
        if (advisorNames.length == 0) {
            return new ArrayList<>();
        }

        List<Advisor> advisors = new ArrayList<>();
        for (String name : advisorNames) {
            /** 检验这个bean是否是合格的. **/
            if (isEligibleBean(name)) {
                if (this.beanFactory.isCurrentlyInCreation(name)) {
                    /** 要创建的 Bean 就是 当前Bean: 啥也不干. 要不你想自己切你自己吗 ? **/
                }
                else {
                    /** 从 IoC 中拿出来, 实例化. **/
                    advisors.add(this.beanFactory.getBean(name, Advisor.class));
                }
            }
        }
        return advisors;
    }

    /**
     * 检验这个bean是否是合格的, 这里默认返回 true.
     * 当前类只有一个子类, 是 {@link AbstractAdvisorAutoProxyCreator}里面的内部类: , 对 此方法 进行了重写,
     * 其实现就是调用 {@link AbstractAdvisorAutoProxyCreator#isEligibleAdvisorBean(String)}: 默认返回 True.
     * 此基于 Advisor的创建器有很多子类, 其中两个子类进行了重写: ↓
     *     @see InfrastructureAdvisorAutoProxyCreator
     *     @see DefaultAdvisorAutoProxyCreator
     */
    protected boolean isEligibleBean(String beanName) {
        return true;
    }

}
