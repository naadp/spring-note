/*
 * Copyright (c) 2017-2020 jdjr All Rights Reserved.
 * PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 * Author Email: wangzongyao@jd.com
 */

package resource.creator;

import org.springframework.aop.Advisor;
import org.springframework.aop.aspectj.annotation.AspectJAdvisorFactory;
import org.springframework.aop.aspectj.annotation.BeanFactoryAspectJAdvisorsBuilder;
import org.springframework.aop.aspectj.annotation.ReflectiveAspectJAdvisorFactory;
import org.springframework.aop.aspectj.autoproxy.AspectJAwareAdvisorAutoProxyCreator;
import org.springframework.aop.framework.autoproxy.BeanFactoryAdvisorRetrievalHelper;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.util.Assert;
import org.springframework.beans.factory.BeanFactoryAware;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 解析 @Aspect注解 的创建器.
 * @EnableAspectJAutoProxy 导入的就是这个自动代理创建器.
 * @author wangzongyao on 2020/5/25
 */
public class AnnotationAwareAspectJAutoProxyCreator extends AspectJAwareAdvisorAutoProxyCreator {

    private List<Pattern> includePatterns;

    /**
     * 唯一实现类: ReflectiveAspectJAdvisorFactory.
     * 基于 @Aspect注解 时, 创建 Spring AOP 的 Advice.
     * 里面会对标注这些注解 @Around, @Before, @After, @AfterReturning, @AfterThrowing 的方法进行排序,
     * 然后把他们都变成 Advisor(getAdvisors()方法).
     */
    private AspectJAdvisorFactory aspectJAdvisorFactory;

    /**
     * 工具类: 从 BeanFactory 中获取所有使用了 @AspectJ注解 的 bean
     * 就是这个方法: {@link BeanFactoryAspectJAdvisorsBuilder#buildAspectJAdvisors()}.
     */
    private BeanFactoryAspectJAdvisorsBuilder aspectJAdvisorsBuilder;


    /**
     * 支持我们自定义一个正则的模版,
     * isEligibleAspectBean()该方法使用此模版, 从而决定使用哪些Advisor.
     */
    public void setIncludePatterns(List<String> patterns) {
        this.includePatterns = new ArrayList<>(patterns.size());
        for (String patternText : patterns) {
            this.includePatterns.add(Pattern.compile(patternText));
        }
    }

    /** 可以自己实现一个 AspectJAdvisorFactory, 否则用默认的 ReflectiveAspectJAdvisorFactory. **/
    public void setAspectJAdvisorFactory(AspectJAdvisorFactory aspectJAdvisorFactory) {
        Assert.notNull(aspectJAdvisorFactory, "AspectJAdvisorFactory must not be null");
        this.aspectJAdvisorFactory = aspectJAdvisorFactory;
    }

    /**
     * {@link AbstractAutoProxyCreator} 实现了 {@link BeanFactoryAware}接口, 在 setBeanFactory() 方法中, 调用了 此方法.
     *
     * 父类 {@link AbstractAdvisorAutoProxyCreator} 中初始化了 {@link BeanFactoryAdvisorRetrievalHelperAdapter}: 从 Bean工厂 检索出 Advisor.
     *      this.advisorRetrievalHelper = new BeanFactoryAdvisorRetrievalHelperAdapter(beanFactory);
     *      其继承了 {@link BeanFactoryAdvisorRetrievalHelper}
     * 此处添加了 {@link BeanFactoryAspectJAdvisorsBuilderAdapter}, 从加了 @Aspect注解 的类中查找 Advisor.
     *
     * @see #findCandidateAdvisors(): 先调用父类, 再调自己的实现. 其实也就是分别调用下那两个委托类去查找.
     */
    @Override
    protected void initBeanFactory(ConfigurableListableBeanFactory beanFactory) {
        super.initBeanFactory(beanFactory);
        if (this.aspectJAdvisorFactory == null) {
            this.aspectJAdvisorFactory = new ReflectiveAspectJAdvisorFactory(beanFactory);
        }
        this.aspectJAdvisorsBuilder =
                new BeanFactoryAspectJAdvisorsBuilderAdapter(beanFactory, this.aspectJAdvisorFactory);
    }

    /**
     * 拿到所有的候选的 Advisor.
     *   拿到Advisor时会用 {@link #isEligibleAdvisorBean(String)} 和 {@link #isEligibleAspectBean(String)} 筛选,
     *   此方法外部也会根据 PointCut进行筛选.
     *
     * 先调用父类的 super.findCandidateAdvisors(), 去容器里找出来一些,
     * 然后, 然后自己又通过 aspectJAdvisorsBuilder.buildAspectJAdvisors(): 解析 @Aspect 的方法得到一些 Advisor.
     */
    @Override
    protected List<Advisor> findCandidateAdvisors() {
        List<Advisor> advisors = super.findCandidateAdvisors();
        advisors.addAll(this.aspectJAdvisorsBuilder.buildAspectJAdvisors());
        return advisors;
    }

    /**
     * 加了种类型: 如果该Bean自己本身就是一个@Aspect， 那也认为是基础主键，不要切了
     */
    @Override
    protected boolean isInfrastructureClass(Class<?> beanClass) {
        return (super.isInfrastructureClass(beanClass) || this.aspectJAdvisorFactory.isAspect(beanClass));
    }

    /**
     * 拿传入的正则模版进行匹配(没传就返回true: 所有的Advisor都会生效).
     * 对 查询到的 Advisor 进行筛选过滤, 因为 findCandidateAdvisors 会查找全部的 Advisor, 需要再进行一次筛选.
     *   此方法筛选的是当前类根据 @Aspect注解解析出来的 Advisor.
     *   父类从 IoC 拿到的 Advisor 是由父类的 {@link #isEligibleAdvisorBean(String)}筛选的.
     *
     *
     * 注: 其实很像的.
     * {@link AspectJAwareAdvisorAutoProxyCreator} 通过 {@link BeanFactoryAdvisorRetrievalHelper} 去 IoC 中查找 Advisor
     *   找到全部的 Advisor 后, 由   {@link BeanFactoryAdvisorRetrievalHelper#isEligibleBean(String)}再进行过滤.
     *   但其子类重写了 "isEligibleBean(String)"方法, 内部调用 当前创建器的 "isEligibleAdvisorBean(String)"方法.
     *
     * 再看当前类: ↓
     * {@link AnnotationAwareAspectJAutoProxyCreator} 通过 {@link BeanFactoryAspectJAdvisorsBuilder} 去查找加了 @Aspect 注解的类,从中解析出 Advisor
     * Advisor 的过滤交给了当前创建器的 {@link #isEligibleAspectBean}方法.
     */
    protected boolean isEligibleAspectBean(String beanName) {
        if (this.includePatterns == null) {
            return true;
        }
        else {
            for (Pattern pattern : this.includePatterns) {
                if (pattern.matcher(beanName).matches()) {
                    return true;
                }
            }
            return false;
        }
    }

    private class BeanFactoryAspectJAdvisorsBuilderAdapter extends BeanFactoryAspectJAdvisorsBuilder {

        public BeanFactoryAspectJAdvisorsBuilderAdapter(
                ListableBeanFactory beanFactory, AspectJAdvisorFactory advisorFactory) {

            super(beanFactory, advisorFactory);
        }

        @Override
        protected boolean isEligibleBean(String beanName) {
            return isEligibleAspectBean(beanName);
        }
    }

}
