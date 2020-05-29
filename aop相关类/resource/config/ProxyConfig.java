/*
 * Copyright (c) 2017-2020 jdjr All Rights Reserved.
 * PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 * Author Email: wangzongyao@jd.com
 */

package resource.config;

import org.springframework.aop.framework.AdvisedSupport;
import org.springframework.aop.framework.DefaultAopProxyFactory;
import org.springframework.util.Assert;

import java.io.Serializable;

/**
 * Aop配置.
 * @author wangzongyao on 2020/5/24
 */
public class ProxyConfig implements Serializable {

    /**
     * 是否直接对目标类进行代理: CGLib
     * 而不是通过接口产生代理: JDK.
     */
    private boolean proxyTargetClass = false;

    /**
     * 标记代理对象是否应该被 Aop框架 通过 AopContext 以 ThreadLocal 的形式暴露出去.
     * <b>
     *     当一个代理对象需要调用它自己的另外一个代理方法时, 这个属性将非常有用.
     *     默认是是false, 以避免不必要的拦截.
     * </b>
     */
    boolean exposeProxy = false;

    /**
     * 汉译: 使最优化.
     * 是否对代理进行优化。
     * 启动优化通常意味着在代理对象被创建后, 增强的修改将不会生效, 因此默认值为false.
     * 如果 exposeProxy设置为true, 即使optimize为true也会被忽略.
     * 这个值看源码的话, 在 {@link DefaultAopProxyFactory#createAopProxy(AdvisedSupport)} 中有使用, 作为创建JDK代理还是AOP代理的判断条件.
     */
    private boolean optimize = false;

    /**
     * 汉译: 不透明的.
     * 是否需要阻止通过该配置创建的代理对象转换为Advised类型.
     * 默认 false, 表示代理对象可以被转换为Advised类型.
     */
    boolean opaque = false;

    /**
     * opaque属性设为false时, 我们可以将代理对象转换为Advised类型, 进而对代理对象的一些属性进行查询和修改.
     * frozen则用来标记是否需要冻结代理对象: 在代理对象生成之后, 是否允许对其进行修改. 默认为false.
     */
    private boolean frozen = false;

    /** 下面是这5个属性的setter、getter, 让我省略了. **/

    /**
     * 将另一个代理配置对象赋值给当前对象.
     * @param other
     */
    public void copyFrom(ProxyConfig other) {
        Assert.notNull(other, "Other ProxyConfig object must not be null");
        this.proxyTargetClass = other.proxyTargetClass;
        this.optimize = other.optimize;
        this.exposeProxy = other.exposeProxy;
        this.frozen = other.frozen;
        this.opaque = other.opaque;
    }

}
