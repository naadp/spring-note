/*
 * Copyright (c) 2017-2020 jdjr All Rights Reserved.
 * PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 * Author Email: wangzongyao@jd.com
 */

package resource.creator;

import org.aopalliance.aop.Advice;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.aop.Advisor;
import org.springframework.aop.Pointcut;
import org.springframework.aop.TargetSource;
import org.springframework.aop.aspectj.*;
import org.springframework.aop.aspectj.AspectJExpressionPointcut;
import org.springframework.aop.aspectj.AspectJPointcutAdvisor;
import org.springframework.aop.aspectj.annotation.*;
import org.springframework.aop.aspectj.autoproxy.AspectJAwareAdvisorAutoProxyCreator;
import org.springframework.aop.framework.AopInfrastructureBean;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.framework.ProxyProcessorSupport;
import org.springframework.aop.framework.adapter.AdvisorAdapterRegistry;
import org.springframework.aop.framework.adapter.GlobalAdvisorAdapterRegistry;
import org.springframework.aop.framework.autoproxy.*;
import org.springframework.aop.target.SingletonTargetSource;
import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.SmartInstantiationAwareBeanPostProcessor;
import org.springframework.util.StringUtils;
import resource.config.ProxyConfig;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Constructor;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 自动代理创建器: 声明式 Aop 编程中非常重要的一个角色. 主要是规定了代理创建的流程.
 * <ol>
 *     <li>
 *         继承了 ProxyProcessorSupport, 所以它具有了ProxyConfig中的配置属性.
 *     </li>
 *     <li>
 *         实现了 SmartInstantiationAwareBeanPostProcessor
 *             包括 实例化 的 前后置函数, 初始化 的 前后置函数, 并进行了实现.
 *     </li>
 *     <li>
 *         实现了 创建代理类的主方法: createProxy().
 *     </li>
 *     <li>
 *         <b>定义了抽象方法 getAdvicesAndAdvisorsForBean: 获取 Bean对应的 Advisor.</b>
 *     </li>
 * </ol>
 * {@link AbstractAutoProxyCreator} 规定了 创建 Aop 对象的主逻辑(模版),
 *   其子类 {@link AbstractAdvisorAutoProxyCreator} 实现了getAdvicesAndAdvisorsForBean 方法,
 *   并且通过工具类 {@link BeanFactoryAdvisorRetrievalHelper#findAdvisorBeans()} 来获取其对应的 Advisor.
 * 它的主要子类如下: ↓
 * <ul>
 *     <li>
 *         {@link BeanNameAutoProxyCreator}: 通过类的名字来判断是否作用(正则匹配).
 *     </li>
 *     <li>
 *         {@link DefaultAdvisorAutoProxyCreator}:
 *             它帅选作用的类时主要是根据其中的 advisorBeanNamePrefix(类名前缀)配置进行判断.
 *     </li>
 *     <li>
 *         {@link AspectJAwareAdvisorAutoProxyCreator}:
 *             通过解析 Aop 命名空间的配置信息时生成的 AdvisorAutoProxyCreator,
 *             主要通过
 *                 ConfigBeanDefinitionParser.parse() ->
 *                 ConfigBeanDefinitionParser.configureAutoProxyCreator() ->
 *                 AopNamespaceUtils.registerAspectJAutoProxyCreatorIfNecessary() ->
 *                 AopNamespaceUtils.registerAspectJAutoProxyCreatorIfNecessary();
 *             与之对应的
 *                 Pointcut 是 {@link AspectJExpressionPointcut},
 *                 Advisor 是 {@link AspectJPointcutAdvisor},
 *                 Advice 则是
 *                     {@link AspectJAfterAdvice},
 *                     {@link AspectJAfterReturningAdvice},
 *                     {@link AspectJAfterThrowingAdvice},
 *                     {@link AspectJAroundAdvice}
 *     </li>
 *     <li>
 *         {@link AnnotationAwareAspectJAutoProxyCreator}:
 *             基于 @AspectJ注解 生成的 切面类的一个 AbstractAutoProxyCreator,
 *             解析的工作交给了 AspectJAutoProxyBeanDefinitionParser, 步骤如下
 *                 AspectJAutoProxyBeanDefinitionParser.parse() ->
 *                 AopNamespaceUtils.registerAspectJAnnotationAutoProxyCreatorIfNecessary() ->
 *                 AopConfigUtils.registerAspectJAnnotationAutoProxyCreatorIfNecessary()
 *     </li>
 * </ul>
 *
 * @see org.springframework.aop.framework.autoproxy.AbstractAutoProxyCreator
 * @author wangzongyao on 2020/5/25
 */
@SuppressWarnings("all")
public abstract class AbstractAutoProxyCreator extends ProxyProcessorSupport
        implements SmartInstantiationAwareBeanPostProcessor, BeanFactoryAware {

    /**
     * Convenience constant for subclasses: Return value for "do not proxy".
     * @see #getAdvicesAndAdvisorsForBean
     */
    protected static final Object[] DO_NOT_PROXY = null;

    /**
     * Convenience constant for subclasses: Return value for
     * "proxy without additional interceptors, just the common ones".
     * @see #getAdvicesAndAdvisorsForBean
     */
    protected static final Object[] PROXY_WITHOUT_ADDITIONAL_INTERCEPTORS = new Object[0];


    /** Logger available to subclasses */
    protected final Log logger = LogFactory.getLog(getClass());

    /** 实现类就是我们熟悉的它: DefaultAdvisorAdapterRegistry */
    private AdvisorAdapterRegistry advisorAdapterRegistry = GlobalAdvisorAdapterRegistry.getInstance();

    /**
     * Indicates whether or not the proxy should be frozen. Overridden from super
     * to prevent the configuration from becoming frozen too early.
     */
    private boolean freezeProxy = false;

    /** 配置 {@link BeanNameAutoProxyCreator} 时 会用到.*/
    private String[] interceptorNames = new String[0];

    /**
     * 是否将 {@link #interceptorNames} 指定的增强放到其他增强的前面.
     * @see #buildAdvisors
     * @see #resolveInterceptorNames()
     */
    private boolean applyCommonInterceptorsFirst = true;

    /**
     * 目标源的创建器, 它有一个方法getTargetSource(Class<?> beanClass, String beanName).
     * 两个实现类: QuickTargetSourceCreator和LazyInitTargetSourceCreator.
     */
    private TargetSourceCreator[] customTargetSourceCreators;

    private BeanFactory beanFactory;

    /**
     * 和 TargetSource 相关的Bean, 会把其 beanName 放进这个 Set中.
     * 这里指的相关并不是说这个Bean是 TargetSource的子类. 而是看你的 TargetSourceCreator能否获得到它.
     */
    private final Set<String> targetSourcedBeans =
            Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>(16));

    /** 保存已经创建过代理对象的cachekey, 避免重复创建. **/
    private final Map<Object, Object> earlyProxyReferences = new ConcurrentHashMap<>(16);

    private final Map<Object, Class<?>> proxyTypes = new ConcurrentHashMap<Object, Class<?>>(16);

    /** 经过了 Aop流程 的  Bean, Value: 是否为其创建了代理. **/
    private final Map<Object, Boolean> advisedBeans = new ConcurrentHashMap<Object, Boolean>(256);

    @Override
    public void setFrozen(boolean frozen) {
        this.freezeProxy = frozen;
    }

    @Override
    public boolean isFrozen() {
        return this.freezeProxy;
    }

    /**
     * 可以自己指定Registry
     * @see org.springframework.aop.framework.adapter.GlobalAdvisorAdapterRegistry
     */
    public void setAdvisorAdapterRegistry(AdvisorAdapterRegistry advisorAdapterRegistry) {
        this.advisorAdapterRegistry = advisorAdapterRegistry;
    }

    /**
     * 可以指定多个.
     */
    public void setCustomTargetSourceCreators(TargetSourceCreator... targetSourceCreators) {
        this.customTargetSourceCreators = targetSourceCreators;
    }

    /**
     * 通用拦截器得名字, 这些Bean必须在当前容器内存在的.
     */
    public void setInterceptorNames(String... interceptorNames) {
        this.interceptorNames = interceptorNames;
    }

    /** 默认值是true. **/
    public void setApplyCommonInterceptorsFirst(boolean applyCommonInterceptorsFirst) {
        this.applyCommonInterceptorsFirst = applyCommonInterceptorsFirst;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) { this.beanFactory = beanFactory; }

    /**
     * Return the owning {@link BeanFactory}.
     * May be {@code null}, as this post-processor doesn't need to belong to a bean factory.
     */
    protected BeanFactory getBeanFactory() { return this.beanFactory; }

    @Override
    public Class<?> predictBeanType(Class<?> beanClass, String beanName) {
        if (this.proxyTypes.isEmpty()) {
            return null;
        }
        Object cacheKey = getCacheKey(beanClass, beanName);
        return this.proxyTypes.get(cacheKey);
    }

    @Override
    public Constructor<?>[] determineCandidateConstructors(Class<?> beanClass, String beanName) throws BeansException {
        return null;
    }

    /**
     * 解决单例bean之间的循环依赖问题，提前将代理对象暴露出去
     */
    @Override
    public Object getEarlyBeanReference(Object bean, String beanName) throws BeansException {
        Object cacheKey = getCacheKey(bean.getClass(), beanName);
        this.earlyProxyReferences.put(cacheKey, bean);
        return wrapIfNecessary(bean, beanName, cacheKey);
    }

    /**
     * 这个很重要.
     * 如果用户使用了自定义的TargetSource对象，则直接使用该对象生成目标对象，而不会使用Spring的默认逻辑生成目标对象
     * 并且这里会判断各个切面逻辑是否可以应用到当前bean上
     */
    @Override
    public Object postProcessBeforeInstantiation(Class<?> beanClass, String beanName) throws BeansException {
        Object cacheKey = getCacheKey(beanClass, beanName);

        /** beanName 无效 或者 targetSourcedBeans 里不包含此 Bean. **/
        if (beanName == null || !this.targetSourcedBeans.contains(beanName)) {
            if (this.advisedBeans.containsKey(cacheKey)) {
                return null;
            }
            if (isInfrastructureClass(beanClass) || shouldSkip(beanClass, beanName)) {
                this.advisedBeans.put(cacheKey, Boolean.FALSE);
                return null;
            }
        }
        if (beanName != null) {
            TargetSource targetSource = getCustomTargetSource(beanClass, beanName);
            if (targetSource != null) {
                this.targetSourcedBeans.add(beanName);
                Object[] specificInterceptors = getAdvicesAndAdvisorsForBean(beanClass, beanName, targetSource);
                Object proxy = createProxy(beanClass, beanName, specificInterceptors, targetSource);
                this.proxyTypes.put(cacheKey, proxy.getClass());
                return proxy;
            }
        }
        return null;
    }

    @Override
    public boolean postProcessAfterInstantiation(Object bean, String beanName) {
        return true;
    }

    @Override
    public PropertyValues postProcessPropertyValues(
            PropertyValues pvs, PropertyDescriptor[] pds, Object bean, String beanName) {

        return pvs;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
        return bean;
    }

    /**
     * earlyProxyReferences缓存：该缓存用于
     * @see #getAdvicesAndAdvisorsForBean
     */
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean != null) {
            Object cacheKey = getCacheKey(bean.getClass(), beanName);
            if (this.earlyProxyReferences.remove(cacheKey) != bean) {
                return wrapIfNecessary(bean, beanName, cacheKey);
            }
        }
        return bean;
    }

    protected Object getCacheKey(Class<?> beanClass, String beanName) {
        if (StringUtils.hasLength(beanName)) {
            return (FactoryBean.class.isAssignableFrom(beanClass) ?
                    BeanFactory.FACTORY_BEAN_PREFIX + beanName : beanName);
        }
        else {
            return beanClass;
        }
    }

    /**
     *
     */
    protected Object wrapIfNecessary(Object bean, String beanName, Object cacheKey) {
        /**
         * 若此Bean已经在targetSourcedBeans里，说明已经被代理过，那就直接返回即可.
         * {@link #shouldSkip(Class, String)}: 子类有覆盖.
         */
        if (isInfrastructureClass(bean.getClass()) || shouldSkip(bean.getClass(), beanName)) {
            this.advisedBeans.put(cacheKey, Boolean.FALSE);
            return bean;
        }
        /**
         * 寻找切面: 空方法, 子类有实现. 根据实现, 分为两类: ↓
         * @see BeanNameAutoProxyCreator#getAdvicesAndAdvisorsForBean(Class, String, TargetSource)
         * @see AbstractAdvisorAutoProxyCreator#getAdvicesAndAdvisorsForBean(Class, String, TargetSource)
         */
        Object[] specificInterceptors = getAdvicesAndAdvisorsForBean(bean.getClass(), beanName, null);
        /** 不代理. **/
        if (specificInterceptors == DO_NOT_PROXY) {
            this.advisedBeans.put(cacheKey, Boolean.FALSE);
            return bean;
        }
        /** 创建代理. **/
        this.advisedBeans.put(cacheKey, Boolean.TRUE);
        Object proxy = createProxy(bean.getClass(), beanName, specificInterceptors, new SingletonTargetSource(bean));
        /** 这里获得的是代理对象所在的类. **/
        this.proxyTypes.put(cacheKey, proxy.getClass());
        return proxy;
    }

    /** 基础组件. **/
    protected boolean isInfrastructureClass(Class<?> beanClass) {
        boolean retVal = Advice.class.isAssignableFrom(beanClass) ||
                Pointcut.class.isAssignableFrom(beanClass) ||
                Advisor.class.isAssignableFrom(beanClass) ||
                AopInfrastructureBean.class.isAssignableFrom(beanClass);
        return retVal;
    }

    protected boolean shouldSkip(Class<?> beanClass, String beanName) {
        return false;
    }

    /**
     * 这个方法也很重要, 若我们自己要实现一个 TargetSourceCreator, 就可议实现我们自定义的逻辑了.
     * 这里条件苛刻：customTargetSourceCreators 必须不为null
     * 并且容器内还必须有这个Bean：beanFactory.containsBean(beanName)
     *   注：此 BeanName 指的 即将需要被代理的 BeanName, 而不是 TargetSourceCreator 的 BeanName.
     */
    protected TargetSource getCustomTargetSource(Class<?> beanClass, String beanName) {
        if (this.customTargetSourceCreators != null &&
                this.beanFactory != null && this.beanFactory.containsBean(beanName)) {
            for (TargetSourceCreator tsc : this.customTargetSourceCreators) {
                TargetSource ts = tsc.getTargetSource(beanClass, beanName);
                if (ts != null) {
                    return ts;
                }
            }
        }
        return null;
    }

    /**
     * 代理真正创建的地方.
     * @param specificInterceptors 作用在这个Bean上的增强器们.
     * @param targetSource
     *     这里需要注意, 入参是 TargetSource, 而不是target. TargetSource 允许你有自己的逻辑.
     *     所以最终代理的是 TargetSource. 每次AOP代理处理方法调用时, 都会用到 TargetSource 实现目标实例的获取.
     */
    protected Object createProxy(Class<?> beanClass, String beanName, Object[] specificInterceptors, TargetSource targetSource) {

        if (this.beanFactory instanceof ConfigurableListableBeanFactory) {
            AutoProxyUtils.exposeTargetClass((ConfigurableListableBeanFactory) this.beanFactory, beanName, beanClass);
        }

        /** ProxyFactory: 创建代理对象的三大方式之一.
         * 复制当前类的相关配置, 因为当前类也是个 {@link ProxyConfig}
         */
        ProxyFactory proxyFactory = new ProxyFactory();
        proxyFactory.copyFrom(this);

        /**
         * 决定走 Cglib 还是 JDK. 走JDK的话, 还要把目标类实现的所有接口全都交给 proxyFactory.
         * proxy-target-class = false, 即 用户配置是 进行JDK接口 方式的代理.
         * 这时还需要去检测能否用JDK代理.
         */
        if (!proxyFactory.isProxyTargetClass()) {
            if (shouldProxyTargetClass(beanClass, beanName)) {
                //从源码看, 这里一般就是 True了. 不过搞不懂这里是做啥的.
                proxyFactory.setProxyTargetClass(true);
            }
            else {
                /**
                 * 检查目标类有没有实现 "合适" 的接口,
                 *   没有: 走Cglib代理, 说白了就是这么一行代码 ======> {@code proxyFactory.setProxyTargetClass(true);}
                 *    有: 走JDK代理(因为默认是false, 所以内部也就没再设置), 并将其实现的全部接口都交给 proxyFactory.
                 *      注意: 是实现的全部接口都交给 proxyFactory, 并非只有那些 "合适" 的接口.
                 */
                evaluateProxyInterfaces(beanClass, proxyFactory);
            }
        }

        /**
         * 整理、合并, 得到最终的advisors
         * 至于调用的先后顺序, 通过 applyCommonInterceptorsFirst 参数可以进行设置，
         * 若 applyCommonInterceptorsFirst 为true, interceptorNames属性指定的Advisor优先调用。默认为true
         */
        Advisor[] advisors = buildAdvisors(beanName, specificInterceptors);
        /**
         * Advisor 交给工厂.
         * 设置 TargetSource.
         * {@link #customizeProxyFactory}: 子类拓展空间, 但是没人鸟他......
         */
        proxyFactory.addAdvisors(advisors);
        proxyFactory.setTargetSource(targetSource);
        customizeProxyFactory(proxyFactory);

        proxyFactory.setFrozen(this.freezeProxy);
        /**
         * 设置 preFiltered属性值, 默认是false. 子类 {@link AbstractAdvisorAutoProxyCreator} 修改为 true了
         * preFiltered: 是否已为特定目标类筛选 Advisor.
         * 这个字段和 DefaultAdvisorChainFactory.getInterceptorsAndDynamicInterceptionAdvice 获取所有的 Advisor 有关
         * CglibAopProxy 和 JdkDynamicAopProxy 都会调用此方法, 然后递归执行所有的 Advisor.
         */
        if (advisorsPreFiltered()) {
            proxyFactory.setPreFiltered(true);
        }

        return proxyFactory.getProxy(getProxyClassLoader());
    }

    /**
     * 将增强进行包装, 转为 Advisor.
     * 如果 存在 interceptorNames, 并且 applyCommonInterceptorsFirst 为true, 则将其放到首部.
     */
    protected Advisor[] buildAdvisors(String beanName, Object[] specificInterceptors) {
        /**
         * 解析 interceptorNames 得到的 Advisor数组.
         * 当你 使用 {@link BeanNameAutoProxyCreator} 创建代理时, 就要指定 interceptorNames属性.
         */
        Advisor[] commonInterceptors = resolveInterceptorNames();

        List<Object> allInterceptors = new ArrayList<Object>();
        if (specificInterceptors != null) {
            allInterceptors.addAll(Arrays.asList(specificInterceptors));
            if (commonInterceptors.length > 0) {
                if (this.applyCommonInterceptorsFirst) {
                    allInterceptors.addAll(0, Arrays.asList(commonInterceptors));
                }
                else {
                    allInterceptors.addAll(Arrays.asList(commonInterceptors));
                }
            }
        }

        Advisor[] advisors = new Advisor[allInterceptors.size()];
        for (int i = 0; i < allInterceptors.size(); i++) {
            advisors[i] = this.advisorAdapterRegistry.wrap(allInterceptors.get(i));
        }
        return advisors;
    }

    protected boolean shouldProxyTargetClass(Class<?> beanClass, String beanName) {
        return (this.beanFactory instanceof ConfigurableListableBeanFactory &&
                AutoProxyUtils.shouldProxyTargetClass((ConfigurableListableBeanFactory) this.beanFactory, beanName));
    }


    protected boolean advisorsPreFiltered() {
        return false;
    }

    /**
     * Resolves the specified interceptor names to Advisor objects.
     * @see #setInterceptorNames
     */
    private Advisor[] resolveInterceptorNames() {
        ConfigurableBeanFactory cbf = (this.beanFactory instanceof ConfigurableBeanFactory ?
                (ConfigurableBeanFactory) this.beanFactory : null);
        List<Advisor> advisors = new ArrayList<Advisor>();
        for (String beanName : this.interceptorNames) {
            /** IoC 中正在创建的 Bean 不是该拦截器. cbf==null, 这个操作看不懂...... **/
            if (cbf == null || !cbf.isCurrentlyInCreation(beanName)) {
                Object next = this.beanFactory.getBean(beanName);
                //适配一下: 将 MethodInterceptor 或者 能转成 MethodInterceptor的Advice, 封装成 DefaultPointcutAdvisor.
                advisors.add(this.advisorAdapterRegistry.wrap(next));
            }
        }
        return advisors.toArray(new Advisor[advisors.size()]);
    }

    /**
     * Subclasses may choose to implement this: for example,
     * to change the interfaces exposed.
     * <p>The default implementation is empty.
     * @param proxyFactory a ProxyFactory that is already configured with
     * TargetSource and interfaces and will be used to create the proxy
     * immediately after this method returns
     */
    protected void customizeProxyFactory(ProxyFactory proxyFactory) {
    }



    protected abstract Object[] getAdvicesAndAdvisorsForBean(
            Class<?> beanClass, String beanName, TargetSource customTargetSource) throws BeansException;

}
