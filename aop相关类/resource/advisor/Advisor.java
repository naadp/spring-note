/*
 * Copyright (c) 2017-2020 jdjr All Rights Reserved.
 * PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 * Author Email: wangzongyao@jd.com
 */

package resource.advisor;

import org.aopalliance.aop.Advice;
import org.springframework.aop.aspectj.*;
import org.springframework.aop.config.ConfigBeanDefinitionParser;
import org.springframework.cache.interceptor.BeanFactoryCacheOperationSourceAdvisor;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.BeanFactoryTransactionAttributeSourceAdvisor;
import org.springframework.transaction.interceptor.TransactionAttributeSourceAdvisor;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.aop.aspectj.annotation.*;
import org.springframework.transaction.interceptor.TransactionProxyFactoryBean;
import resource.pointcut.Pointcut;

/**
 * Pointcut 与 Advice 的组合.
 * Advice 是执行的方法, Advice 与 Pointcut 组合在一起, 才能知道方法何时执行. 这就诞生了 Advisor 这个类.
 *
 * 打个比方: ↓
 *     Advice表示建议
 *     Pointcut表示建议的地点
 *     Advisor表示建议者(它知道去哪建议, 且知道是什么建议)
 * 常见的实现: ↓
 * <ul>
 *     <li>
 *         {@link AspectJPointcutAdvisor}: 这个是 Spring 解析 aop 命名空间时生成的 Advisor.
 *         与之对应的 {@link Advice} 分别是
 *             {@link AspectJMethodBeforeAdvice}
 *             {@link AspectJAfterAdvice}
 *             {@link AspectJAfterReturningAdvice},
 *             {@link AspectJAfterThrowingAdvice}
 *             {@link AspectJAroundAdvice},
 *         {@link Pointcut} 则是 {@link AspectJExpressionPointcut}, 对于这个类的解析是在 {@link ConfigBeanDefinitionParser}
 *     </li>
 *     <li>
 *         {@link InstantiationModelAwarePointcutAdvisorImpl}:
 *         这个 Advisor 是 Spring 在解析被 @AspectJ 注解修饰的类时生成的 Advisor,
 *         这个 Advisor 中的 Pointcut 与 Advice 都是由 ReflectiveAspectJAdvisorFactory 来解析生成的.
 *         与之对应的 Advice 是
 *             {@link AspectJMethodBeforeAdvice}
 *             {@link AspectJAfterAdvice}
 *             {@link AspectJAfterReturningAdvice},
 *             {@link AspectJAfterThrowingAdvice}
 *             {@link AspectJAroundAdvice},
 *         {@link Pointcut} 则是 {@link AspectJExpressionPointcut},
 *         解析的步骤是:
 *             {@link AnnotationAwareAspectJAutoProxyCreator#findCandidateAdvisors()}
 *             -> BeanFactoryAspectJAdvisorsBuilder.buildAspectJAdvisors()
 *             -> ReflectiveAspectJAdvisorFactory.getAdvisors()
 *             -> ReflectiveAspectJAdvisorFactory.getAdvisor().
 *         最终生成了 InstantiationModelAwarePointcutAdvisorImpl
 *             当然包括里面的 Pointcut 与 Adivce 也都是由 ReflectiveAspectJAdvisorFactory 解析生成的)
 *     </li>
 *     <li>
 *         {@link TransactionAttributeSourceAdvisor}:
 *         基于 MethodInterceptor(其实是 {@link TransactionInterceptor}) 与 TransactionAttributeSourcePointcut 的Advisor,
 *         这个类最常与 {@link TransactionProxyFactoryBean} 使用.
 *     </li>
 *     <li>
 *         {@link BeanFactoryTransactionAttributeSourceAdvisor}:
 *         在 注解式事务编程 时, 主要是由
 *             {@link AnnotationTransactionAttributeSource}: 主要解析方法上的 @Transactional注解
 *             {@link TransactionInterceptor}: 是个 MethodInterceptor, 正真操作事务的地方
 *         而 {@link BeanFactoryTransactionAttributeSourceAdvisor} 其实起着组合它们的作用 <- 与之相似的还有
 *            {@link BeanFactoryCacheOperationSourceAdvisor}.
 *     </li>
 * </ul>
 *
 * @author wangzongyao on 2020/5/24
 */
@SuppressWarnings("all")
public interface Advisor {

    Advice getAdvice();

    boolean isPerInstance();

}
