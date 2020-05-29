/*
 * Copyright (c) 2017-2020 jdjr All Rights Reserved.
 * PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 * Author Email: wangzongyao@jd.com
 */

package resource.aopproxyfactory.aopproxyfactory;

import org.springframework.aop.SpringProxy;
import org.springframework.aop.framework.*;

import java.io.Serializable;
import java.lang.reflect.Proxy;

/**
 * 从名字上可以看出: Aop代理的工厂, 作用当然是创建Aop代理.
 * 根据 AdvisedSupport 中配置的信息来生成合适的 AopProxy,
 * AopProxy 主要分为 基于JDK动态代理的 JdkDynamicAopProxy 与 基于Cglib 的 ObjenesisCglibAopProxy.
 * 就一个实现类: {@link DefaultAopProxyFactory}
 *
 * @author wangzongyao on 2020/5/24
 */
public interface AopProxyFactory {

    AopProxy createAopProxy(AdvisedSupport config) throws AopConfigException;

}

/**
 * 下面是 {@link org.springframework.aop.framework.DefaultAopProxyFactory} 的实现.
 */
class B  implements org.springframework.aop.framework.AopProxyFactory, Serializable {

    /**
     * 根据配置来判断生成JDK代理还是Cglib代理.
     */
    @Override
    public AopProxy createAopProxy(AdvisedSupport config) throws AopConfigException {
        if (config.isOptimize() || config.isProxyTargetClass() || hasNoUserSuppliedProxyInterfaces(config)) {
            Class<?> targetClass = config.getTargetClass();
            if (targetClass == null) {
                throw new AopConfigException("TargetSource cannot determine target class: " +
                        "Either an interface or a target is required for proxy creation.");
            }
            if (targetClass.isInterface() || Proxy.isProxyClass(targetClass)) {
                return new JdkDynamicAopProxy(config);
            }
            return new ObjenesisCglibAopProxy(config);
        }
        else {
            return new JdkDynamicAopProxy(config);
        }
    }

    private boolean hasNoUserSuppliedProxyInterfaces(AdvisedSupport config) {
        Class<?>[] ifcs = config.getProxiedInterfaces();
        return (ifcs.length == 0 || (ifcs.length == 1 && SpringProxy.class.isAssignableFrom(ifcs[0])));
    }
}