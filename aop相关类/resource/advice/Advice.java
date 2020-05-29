/*
 * Copyright (c) 2017-2020 jdjr All Rights Reserved.
 * PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 * Author Email: wangzongyao@jd.com
 */
package resource.advice;

import org.aopalliance.intercept.MethodInterceptor;
import org.springframework.aop.*;
import org.springframework.aop.aspectj.*;
import org.aspectj.lang.annotation.Before;
import org.springframework.aop.framework.CglibAopProxy;
import org.springframework.aop.framework.JdkDynamicAopProxy;
import org.springframework.aop.framework.adapter.*;
import org.springframework.aop.interceptor.PerformanceMonitorInterceptor;
import org.springframework.transaction.interceptor.TransactionInterceptor;

/**
 * 增强.
 * Aop联盟的接口.
 * 在 Spring Aop 中, Advice 表示 在 Pointcut 点上应该执行的方法.
 * 而这些方法可以在目标方法之前、之后、包裹、抛出异常等等任何地方执行.
 *
 * Advice: 其主要分成两类
 * <ul>
 *     <li>普通advice</li>
 *     <li>Interceptor/MethodInterceptor</li>
 * </ul>
 * 普通Advice: ↓
 * <ol>
 *     <li>
 *         {@link MethodBeforeAdvice}: 在目标方法之前执行.
 *         主要实现为: {@link AspectJMethodBeforeAdvice}.
 *             这是通过解析被 {@link Before} 注解注释的方法时解析成的Advice
 *     </li>
 *     <li>
 *         {@link AfterReturningAdvice}: 在目标方法之后执行(无论是否有异常).
 *         主要实现:
 *         <ol>
 *             <li>
 *                 {@link AspectJAfterAdvice}: 解析 AspectJ 中的 @After 注解来生成的 Advice
 *                 在java中的实现其实就是在 finally 方法中调用一下对应要执行的方法.
 *             </li>
 *             <li>
 *                 {@link AspectJAfterReturningAdvice}: 解析 AspectJ 中的@AfterReturning 属性来生成的 Advice
 *                 若切面方法抛出异常, 则这里的方法就将不执行
 *             </li>
 *             <li>
 *                 {@link AspectJAfterThrowingAdvice}: 解析 AspectJ 中的 @AfterThrowing 属性来生成的 Advice
 *                 若切面方法抛出异常, 则这里的方法就执行
 *             </li>
 *             <li>
 *                 {@link AspectJAroundAdvice}: 将执行类 MethodInvocation(MethodInvocation其实就是Pointcut) 进行包裹起来,
 *                 并控制其执行的 Advice (其中 Jdk中中 Proxy 使用ReflectiveMethodInvocation, 而 Cglib 则使用 CglibMethodInvocation)
 *             </li>
 *         </ol>
 *     </li>
 * </ol>
 * <b>
 *     注意: 在Proxy中最终执行的其实都是 MethodInterceptor,
 *     因此这些Advice最终都是交给 {@link AdvisorAdapter} -> 将 {@link  Advice} 适配成 {@link MethodInterceptor}
 * </b>
 *
 * 注意: <b>{@link MethodInterceptor}: {@link AspectJAfterAdvice} 与 {@link AspectJAroundAdvice} 其自身就是 {@link MethodInterceptor}.<b/>
 * 下面看常见的实现: ↓
 * <ul>
 *     <li>
 *         {@link PerformanceMonitorInterceptor}: 记录每个方法运行的时长，还是比较有用的.
 *     </li>
 *     <li>
 *         {@link AfterReturningAdviceInterceptor}: AfterReturningAdvice 对应的 MethodInterceptor 适配类,
 *         对应的适配工作由 {@link AfterReturningAdviceAdapter} 完成.
 *     </li>
 *     <li>
 *         {@link MethodBeforeAdviceInterceptor}: MethodBeforeAdvice 对应的 MethodInterceptor 的适配类,
 *         对应的适配工作由 {@link MethodBeforeAdviceAdapter} 完成.
 *     </li>
 *     <li>
 *         {@link ThrowsAdviceInterceptor}: ThrowsAdvice 对应的 MethodInterceptor 的适配类,
 *         对应的适配工作由 {@link ThrowsAdviceAdapter} 完成.
 *     </li>
 *     <li>
 *         {@link TransactionInterceptor}: 大名鼎鼎的注解式事务的工具类,
 *         通过获取注解在方法上的 @Transactional 注解的信息来决定是否开启事务的 MethodInterceptor
 *     </li>
 * </ul>
 * <b>
 *     注意:
 *         1、无论通过aop命名空间/AspectJ注解注释的方法, 其最终都将解析成对应的 Advice
 *         2、所有解析的 Advice 最终都将适配成 MethodInterceptor, 并在 {@link JdkDynamicAopProxy} / {@link CglibAopProxy} 中进行统一调用.
 * </b>
 *
 * @author wangzongyao on 2020/5/24
 */
@SuppressWarnings("all")
public interface Advice {

}
