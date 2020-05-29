/*
 * Copyright (c) 2017-2020 jdjr All Rights Reserved.
 * PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 * Author Email: wangzongyao@jd.com
 */

package resource.advised;

import org.aopalliance.aop.Advice;
import org.springframework.aop.*;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.framework.*;
import org.springframework.aop.support.DefaultIntroductionAdvisor;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.aop.target.EmptyTargetSource;
import org.springframework.aop.target.SingletonTargetSource;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.aop.framework.autoproxy.AbstractAutoProxyCreator;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 对 Advised 的接口方法进行了实现,
 * 最主要的是, 它有一个方法:
 * @see #getInterceptorsAndDynamicInterceptionAdvice(Method, Class)}.
 * @author wangzongyao on 2020/5/28
 */
public class AdvisedSupport extends ProxyConfig implements Advised {

    private static final long serialVersionUID = 2651364800145442165L;

    /** 一个空的实现, targetClass、targetObject 都返回 null. **/
    public static final TargetSource EMPTY_TARGET_SOURCE = EmptyTargetSource.INSTANCE;

    TargetSource targetSource = EMPTY_TARGET_SOURCE;

    private boolean preFiltered = false;

    AdvisorChainFactory advisorChainFactory = new DefaultAdvisorChainFactory();

    private transient Map<MethodCacheKey, List<Object>> methodCache;

    /**
     * 当前代理类实现的接口.
     * {@link AbstractAutoProxyCreator#createProxy()} 中的 evaluateProxyInterfaces()方法,
     * 将当前目标类实现的全部接口都添加到这里来了.
     * @see #addInterface(Class)
     */
    private List<Class<?>> interfaces = new ArrayList<>();


    private List<Advisor> advisors = new ArrayList<>();

    private Advisor[] advisorArray = new Advisor[0];


    public AdvisedSupport() {
        this.methodCache = new ConcurrentHashMap<>(32);
    }

    public AdvisedSupport(Class<?>... interfaces) {
        this.methodCache = new ConcurrentHashMap<>(32);
        setInterfaces(interfaces);
    }

    @Override
    public void setTargetSource(TargetSource targetSource) {
        this.targetSource = (targetSource != null ? targetSource : EMPTY_TARGET_SOURCE);
    }

    @Override
    public TargetSource getTargetSource() {
        return this.targetSource;
    }

    @Override
    public Class<?> getTargetClass() {
        return this.targetSource.getTargetClass();
    }

    @Override
    public void setPreFiltered(boolean preFiltered) {
        this.preFiltered = preFiltered;
    }

    @Override
    public boolean isPreFiltered() {
        return this.preFiltered;
    }

    @Override
    public Class<?>[] getProxiedInterfaces() {
        return ClassUtils.toClassArray(this.interfaces);
    }

    @Override
    public boolean isInterfaceProxied(Class<?> intf) {
        for (Class<?> proxyIntf : this.interfaces) {
            if (intf.isAssignableFrom(proxyIntf)) {
                return true;
            }
        }
        return false;
    }


    @Override
    public final Advisor[] getAdvisors() {
        return this.advisorArray;
    }

    @Override
    public void addAdvisor(Advisor advisor) {
        int pos = this.advisors.size();
        addAdvisor(pos, advisor);
    }

    @Override
    public void addAdvisor(int pos, Advisor advisor) throws AopConfigException {
        if (advisor instanceof IntroductionAdvisor) {
            validateIntroductionAdvisor((IntroductionAdvisor) advisor);
        }
        addAdvisorInternal(pos, advisor);
    }

    @Override
    public boolean removeAdvisor(Advisor advisor) {
        int index = indexOf(advisor);
        if (index == -1) {
            return false;
        }
        else {
            removeAdvisor(index);
            return true;
        }
    }

    @Override
    public void removeAdvisor(int index) throws AopConfigException {
        if (isFrozen()) {
            throw new AopConfigException("Cannot remove Advisor: Configuration is frozen.");
        }
        if (index < 0 || index > this.advisors.size() - 1) {
            throw new AopConfigException("Advisor index " + index + " is out of bounds: " +
                    "This configuration only has " + this.advisors.size() + " advisors.");
        }

        Advisor advisor = this.advisors.get(index);
        if (advisor instanceof IntroductionAdvisor) {
            IntroductionAdvisor ia = (IntroductionAdvisor) advisor;
            // We need to remove introduction interfaces.
            for (int j = 0; j < ia.getInterfaces().length; j++) {
                removeInterface(ia.getInterfaces()[j]);
            }
        }

        this.advisors.remove(index);
        updateAdvisorArray();
        adviceChanged();
    }

    @Override
    public int indexOf(Advisor advisor) {
        Assert.notNull(advisor, "Advisor must not be null");
        return this.advisors.indexOf(advisor);
    }

    @Override
    public boolean replaceAdvisor(Advisor a, Advisor b) throws AopConfigException {
        Assert.notNull(a, "Advisor a must not be null");
        Assert.notNull(b, "Advisor b must not be null");
        int index = indexOf(a);
        if (index == -1) {
            return false;
        }
        removeAdvisor(index);
        addAdvisor(index, b);
        return true;
    }

    @Override
    public void addAdvice(Advice advice) throws AopConfigException {
        int pos = this.advisors.size();
        addAdvice(pos, advice);
    }

    /**
     * Cannot add introductions this way unless the advice implements IntroductionInfo.
     */
    @Override
    public void addAdvice(int pos, Advice advice) throws AopConfigException {
        Assert.notNull(advice, "Advice must not be null");
        if (advice instanceof IntroductionInfo) {
            // We don't need an IntroductionAdvisor for this kind of introduction:
            // It's fully self-describing.
            addAdvisor(pos, new DefaultIntroductionAdvisor(advice, (IntroductionInfo) advice));
        }
        else if (advice instanceof DynamicIntroductionAdvice) {
            // We need an IntroductionAdvisor for this kind of introduction.
            throw new AopConfigException("DynamicIntroductionAdvice may only be added as part of IntroductionAdvisor");
        }
        else {
            addAdvisor(pos, new DefaultPointcutAdvisor(advice));
        }
    }

    @Override
    public boolean removeAdvice(Advice advice) throws AopConfigException {
        int index = indexOf(advice);
        if (index == -1) {
            return false;
        }
        else {
            removeAdvisor(index);
            return true;
        }
    }

    @Override
    public int indexOf(Advice advice) {
        Assert.notNull(advice, "Advice must not be null");
        for (int i = 0; i < this.advisors.size(); i++) {
            Advisor advisor = this.advisors.get(i);
            if (advisor.getAdvice() == advice) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public String toProxyConfigString() {
        return toString();
    }

    public void setTarget(Object target) { setTargetSource(new SingletonTargetSource(target)); }

    public void setTargetClass(Class<?> targetClass) {
        this.targetSource = EmptyTargetSource.forClass(targetClass);
    }

    public void setAdvisorChainFactory(AdvisorChainFactory advisorChainFactory) {
        Assert.notNull(advisorChainFactory, "AdvisorChainFactory must not be null");
        this.advisorChainFactory = advisorChainFactory;
    }

    /**
     * Return the advisor chain factory to use (never {@code null}).
     */
    public AdvisorChainFactory getAdvisorChainFactory() {
        return this.advisorChainFactory;
    }


    public void setInterfaces(Class<?>... interfaces) {
        Assert.notNull(interfaces, "Interfaces must not be null");
        this.interfaces.clear();
        for (Class<?> ifc : interfaces) {
            addInterface(ifc);
        }
    }

    public void addInterface(Class<?> intf) {
        Assert.notNull(intf, "Interface must not be null");
        if (!intf.isInterface()) {
            throw new IllegalArgumentException("[" + intf.getName() + "] is not an interface");
        }
        if (!this.interfaces.contains(intf)) {
            this.interfaces.add(intf);
            adviceChanged();
        }
    }

    public boolean removeInterface(Class<?> intf) {
        return this.interfaces.remove(intf);
    }

    public void addAdvisors(Advisor... advisors) {
        addAdvisors(Arrays.asList(advisors));
    }

    public void addAdvisors(Collection<Advisor> advisors) {
        if (isFrozen()) {
            throw new AopConfigException("Cannot add advisor: Configuration is frozen.");
        }
        if (!CollectionUtils.isEmpty(advisors)) {
            for (Advisor advisor : advisors) {
                if (advisor instanceof IntroductionAdvisor) {
                    validateIntroductionAdvisor((IntroductionAdvisor) advisor);
                }
                Assert.notNull(advisor, "Advisor must not be null");
                this.advisors.add(advisor);
            }
            updateAdvisorArray();
            adviceChanged();
        }
    }

    private void validateIntroductionAdvisor(IntroductionAdvisor advisor) {
        advisor.validateInterfaces();
        // If the advisor passed validation, we can make the change.
        Class<?>[] ifcs = advisor.getInterfaces();
        for (Class<?> ifc : ifcs) {
            addInterface(ifc);
        }
    }

    private void addAdvisorInternal(int pos, Advisor advisor) throws AopConfigException {
        Assert.notNull(advisor, "Advisor must not be null");
        if (isFrozen()) {
            throw new AopConfigException("Cannot add advisor: Configuration is frozen.");
        }
        if (pos > this.advisors.size()) {
            throw new IllegalArgumentException(
                    "Illegal position " + pos + " in advisor list with size " + this.advisors.size());
        }
        this.advisors.add(pos, advisor);
        updateAdvisorArray();
        adviceChanged();
    }

    /**
     * Bring the array up to date with the list.
     */
    protected final void updateAdvisorArray() {
        this.advisorArray = this.advisors.toArray(new Advisor[this.advisors.size()]);
    }

    protected final List<Advisor> getAdvisorsInternal() {
        return this.advisors;
    }

    public boolean adviceIncluded(Advice advice) {
        if (advice != null) {
            for (Advisor advisor : this.advisors) {
                if (advisor.getAdvice() == advice) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 有多少个 Advisor 是 指定类型的.
     */
    public int countAdvicesOfType(Class<?> adviceClass) {
        int count = 0;
        if (adviceClass != null) {
            for (Advisor advisor : this.advisors) {
                if (adviceClass.isInstance(advisor.getAdvice())) {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * 这里传 方法这个参数的原因: 一个切面的增强方法, 可能只做用于目标类的部分方法, 甚至一个方法.
     * 例如 增强A、B 都可以作用于这个类, 但是 A 只作用于 a()方法, B 只作用于 b()方法. 所以这里需要根据 Method参数进行筛讯.
     */
    public List<Object> getInterceptorsAndDynamicInterceptionAdvice(Method method, Class<?> targetClass) {
        MethodCacheKey cacheKey = new MethodCacheKey(method);
        List<Object> cached = this.methodCache.get(cacheKey);
        if (cached == null) {
            cached = this.advisorChainFactory.getInterceptorsAndDynamicInterceptionAdvice(
                    this, method, targetClass);
            this.methodCache.put(cacheKey, cached);
        }
        return cached;
    }

    protected void adviceChanged() {
        this.methodCache.clear();
    }

    protected void copyConfigurationFrom(AdvisedSupport other) {
        copyConfigurationFrom(other, other.targetSource, new ArrayList<Advisor>(other.advisors));
    }

    /**
     * Copy the AOP configuration from the given AdvisedSupport object,
     * but allow substitution of a fresh TargetSource and a given interceptor chain.
     * @param other the AdvisedSupport object to take proxy configuration from
     * @param targetSource the new TargetSource
     * @param advisors the Advisors for the chain
     */
    protected void copyConfigurationFrom(AdvisedSupport other, TargetSource targetSource, List<Advisor> advisors) {
        copyFrom(other);
        this.targetSource = targetSource;
        this.advisorChainFactory = other.advisorChainFactory;
        this.interfaces = new ArrayList<>(other.interfaces);
        for (Advisor advisor : advisors) {
            if (advisor instanceof IntroductionAdvisor) {
                validateIntroductionAdvisor((IntroductionAdvisor) advisor);
            }
            Assert.notNull(advisor, "Advisor must not be null");
            this.advisors.add(advisor);
        }
        updateAdvisorArray();
        adviceChanged();
    }

    /**
     * Build a configuration-only copy of this AdvisedSupport,
     * replacing the TargetSource.
     */
    AdvisedSupport getConfigurationOnlyCopy() {
        AdvisedSupport copy = new AdvisedSupport();
        copy.copyFrom(this);
        copy.targetSource = EmptyTargetSource.forClass(getTargetClass(), getTargetSource().isStatic());
        copy.advisorChainFactory = this.advisorChainFactory;
        copy.interfaces = this.interfaces;
        copy.advisors = this.advisors;
        copy.updateAdvisorArray();
        return copy;
    }


    //---------------------------------------------------------------------
    // Serialization support
    //---------------------------------------------------------------------

    private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        // Rely on default serialization; just initialize state after deserialization.
        ois.defaultReadObject();

        this.methodCache = new ConcurrentHashMap<>(32);
    }




    /**
     * For debugging/diagnostic use.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(getClass().getName());
        sb.append(": ").append(this.interfaces.size()).append(" interfaces ");
        sb.append(ClassUtils.classNamesToString(this.interfaces)).append("; ");
        sb.append(this.advisors.size()).append(" advisors ");
        sb.append(this.advisors).append("; ");
        sb.append("targetSource [").append(this.targetSource).append("]; ");
        sb.append(super.toString());
        return sb.toString();
    }


    /**
     * Simple wrapper class around a Method. Used as the key when
     * caching methods, for efficient equals and hashCode comparisons.
     */
    private static final class MethodCacheKey implements Comparable<MethodCacheKey> {

        private final Method method;

        private final int hashCode;

        public MethodCacheKey(Method method) {
            this.method = method;
            this.hashCode = method.hashCode();
        }

        @Override
        public boolean equals(Object other) {
            return (this == other || (other instanceof MethodCacheKey &&
                    this.method == ((MethodCacheKey) other).method));
        }

        @Override
        public int hashCode() {
            return this.hashCode;
        }

        @Override
        public String toString() {
            return this.method.toString();
        }

        @Override
        public int compareTo(MethodCacheKey other) {
            int result = this.method.getName().compareTo(other.method.getName());
            if (result == 0) {
                result = this.method.toString().compareTo(other.method.toString());
            }
            return result;
        }
    }

}
