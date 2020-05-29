/*
 * Copyright (c) 2017-2020 jdjr All Rights Reserved.
 * PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 * Author Email: wangzongyao@jd.com
 */

package resource.pointcut;

import org.springframework.aop.ClassFilter;
import org.springframework.aop.MethodMatcher;
import org.springframework.aop.TruePointcut;

/**
 * 定义切面的匹配点. 简单的说就是我去切哪些类、哪些方法.
 * 在 Spring Aop 中匹配的点主要是 class 与 method 这两个方面, 分别为 ClassFilter 与 MethodFilter.
 *
 * 有很多实现, 主要有这么几个: ↓
 * <ul>
 *     <li>JdkRegexpMethodPointcut: 通过 正则表达式来匹配方法(PS: ClassFilter.TRUE)</li>
 *     <li>AspectJExpressionPointcut: 通过 AspectJ 包中的组件进行方法的匹配(切点表达式)</li>
 *     <li>
 *         TransactionAttributeSourcePointcut:
 *         通过 TransactionAttributeSource 在 类的方法上提取事务注解的属性 @Transactional 来判断是否匹配,
 *         提取到则说明匹配, 提取不到则说明匹配不成功.
 *     </li>
 * </ul>
 */
public interface Pointcut {

    /** 类过滤器, 可以知道哪些类需要拦截. **/
    ClassFilter getClassFilter();

    /** 方法匹配器, 可以知道哪些方法需要拦截. **/
    MethodMatcher getMethodMatcher();


    /** 匹配所有对象的 Pointcut. **/
    org.springframework.aop.Pointcut TRUE = TruePointcut.INSTANCE;

}
