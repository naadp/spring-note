/*
 * Copyright (c) 2017-2020 jdjr All Rights Reserved.
 * PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 * Author Email: wangzongyao@jd.com
 */

package resource.creator;

import com.sun.istack.internal.Nullable;
import org.springframework.aop.TargetSource;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.util.Assert;
import org.springframework.util.PatternMatchUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * 顶层的代理创建器为 {@link org.springframework.aop.framework.autoproxy.AbstractAutoProxyCreator}, 规定了创建代理的模板, 然后下面实现分为两类:
 *   {@link org.springframework.aop.framework.autoproxy.BeanNameAutoProxyCreator}: 和 Advisor 无关, 只和 BeanName 有关(只有名字匹配上了, 都会给创建一个代理类), beanName 支持通配符的方式.
 *   {@link org.springframework.aop.framework.autoproxy.AbstractAdvisorAutoProxyCreator}: 基于 Advisor 的 自动代理.
 * @see org.springframework.aop.framework.autoproxy.BeanNameAutoProxyCreator
 *
 * @since 10.10.2003 这个类出现得非常早.
 * @author wangzongyao on 2020/5/25
 */
public class BeanNameAutoProxyCreator extends AbstractAutoProxyCreator {
    @Nullable
    private List<String> beanNames;

    public void setBeanNames(String... beanNames) {
        Assert.notEmpty(beanNames, "'beanNames' must not be empty");
        this.beanNames = new ArrayList<>(beanNames.length);
        for (String mappedName : beanNames) {
            // 对mappedName做取出空白处理
            this.beanNames.add(StringUtils.trimWhitespace(mappedName));
        }
    }
    // simpleMatch并不是完整的正则。但是支持*这种通配符，其余的不支持哦
    protected boolean isMatch(String beanName, String mappedName) {
        return PatternMatchUtils.simpleMatch(mappedName, beanName);
    }


    // 这里面注意一点：BeanNameAutoProxyCreator的此方法并没有去寻找Advisor，所以需要拦截的话
    // 只能依靠：setInterceptorNames()来指定拦截器。它是根据名字去Bean容器里取的
    @Override
    @Nullable
    protected Object[] getAdvicesAndAdvisorsForBean(
            Class<?> beanClass, String beanName, @Nullable TargetSource targetSource) {

        if (this.beanNames != null) {
            for (String mappedName : this.beanNames) {
                // 显然这里面，如果你针对的是FactoryBean,也是兼容的~~~
                if (FactoryBean.class.isAssignableFrom(beanClass)) {
                    if (!mappedName.startsWith(BeanFactory.FACTORY_BEAN_PREFIX)) {
                        continue;
                    }
                    // 对BeanName进行处理，去除掉第一个字符
                    mappedName = mappedName.substring(BeanFactory.FACTORY_BEAN_PREFIX.length());
                }

                // 匹配就返回PROXY_WITHOUT_ADDITIONAL_INTERCEPTORS 而不是再返回null了
                if (isMatch(beanName, mappedName)) {
                    return PROXY_WITHOUT_ADDITIONAL_INTERCEPTORS;
                }

                // 这里需要注意的是，如国存在Bean工厂，哪怕任意一个alias匹配都是可以的~~~
                BeanFactory beanFactory = getBeanFactory();
                if (beanFactory != null) {
                    String[] aliases = beanFactory.getAliases(beanName);
                    for (String alias : aliases) {
                        if (isMatch(alias, mappedName)) {
                            return PROXY_WITHOUT_ADDITIONAL_INTERCEPTORS;
                        }
                    }
                }
            }
        }
        return DO_NOT_PROXY;
    }
}

/**
 配置: ↓
 <bean class="org.springframework.aop.framework.autoproxy.BeanNameAutoProxyCreator">
    <property name="beanNames">
        <value>*Test</value>
    </property>
    <property name="interceptorNames">
        <list>
            <value>myInterceptor</value>
        </list>
    </property>
 </bean>
 需要注意的是: 你自己的 Interceptor要实现 具体的 Advice接口 或者 Advisor接口.
 */


