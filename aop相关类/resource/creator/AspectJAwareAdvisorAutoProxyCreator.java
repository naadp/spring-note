/*
 * Copyright (c) 2017-2020 jdjr All Rights Reserved.
 * PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 * Author Email: wangzongyao@jd.com
 */

package resource.creator;

import org.aopalliance.aop.Advice;
import org.aspectj.util.PartialOrder;
import org.springframework.aop.Advisor;
import org.springframework.aop.aspectj.AbstractAspectJAdvice;
import org.springframework.aop.aspectj.AspectJPointcutAdvisor;
import org.springframework.aop.aspectj.AspectJProxyUtils;
import org.springframework.aop.aspectj.autoproxy.AspectJPrecedenceComparator;
import org.springframework.aop.framework.autoproxy.AbstractAdvisorAutoProxyCreator;
import org.springframework.aop.interceptor.ExposeInvocationInterceptor;
import org.springframework.core.Ordered;
import org.springframework.util.ClassUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * @author wangzongyao on 2020/5/25
 */
public class AspectJAwareAdvisorAutoProxyCreator extends AbstractAdvisorAutoProxyCreator {

    /** 默认的排序器，它就不是根据Order来了，而是根据@Afeter @Before类似的标注来排序. **/
    private static final Comparator<Advisor> DEFAULT_PRECEDENCE_COMPARATOR = new AspectJPrecedenceComparator();

    /**
     * 核心逻辑: 它重写了排序
     * 	这个排序和`org.aspectj.util`提供的PartialOrder和PartialComparable有关 具体不详叙了
     * 	比较复杂, 控制着最终的执行顺序, 没看, 哈哈哈.
     */
    @Override
    protected List<Advisor> sortAdvisors(List<Advisor> advisors) {
        List<PartiallyComparableAdvisorHolder> partiallyComparableAdvisors =
                new ArrayList<>(advisors.size());
        for (Advisor element : advisors) {
            partiallyComparableAdvisors.add(
                    new PartiallyComparableAdvisorHolder(element, DEFAULT_PRECEDENCE_COMPARATOR));
        }
        List<PartiallyComparableAdvisorHolder> sorted =
                PartialOrder.sort(partiallyComparableAdvisors);
        if (sorted != null) {
            List<Advisor> result = new ArrayList<Advisor>(advisors.size());
            for (PartiallyComparableAdvisorHolder pcAdvisor : sorted) {
                result.add(pcAdvisor.getAdvisor());
            }
            return result;
        }
        else {
            return super.sortAdvisors(advisors);
        }
    }


    /**
     * 在找到全部的增强并进行筛选过滤之后, 调用此方法.
     * 功能: 若存在 AspectJ 的 Advice, 就会在 advisors 的第一个位置加入 {@link ExposeInvocationInterceptor#ADVISOR} 这个advisor
     * AspectJProxyUtils这个工具类只有这一个方法(其实每次addAspect()的时候, 都会调用此方法).
     * Capable: 能干的, 有才华的.
     *
     * ExposeInvocationInterceptor 的作用是用于暴露 MethodInvocation 对象到 ThreadLocal 中, 其名字也体现出了这一点.
     * 如果其他地方需要当前的 MethodInvocation 对象，可以通过调用静态方法 ExposeInvocationInterceptor.currentInvocation 取出
     * AspectJExpressionPointcut#matches就有用到
     */
    @Override
    protected void extendAdvisors(List<Advisor> candidateAdvisors) {
        AspectJProxyUtils.makeAdvisorChainAspectJCapableIfNecessary(candidateAdvisors);
    }

    /**
     * 这个相当于AspectJPointcutAdvisor的子类不要拦截、AspectJ切面自己的所有方法不要去拦截......
     */
    @Override
    protected boolean shouldSkip(Class<?> beanClass, String beanName) {
        /** 得到全部的增强. **/
        List<Advisor> candidateAdvisors = findCandidateAdvisors();
        for (Advisor advisor : candidateAdvisors) {
            if (advisor instanceof AspectJPointcutAdvisor) {
                if (((AbstractAspectJAdvice) advisor.getAdvice()).getAspectName().equals(beanName)) {
                    return true;
                }
            }
        }
        /** 父类默认返回 false. **/
        return super.shouldSkip(beanClass, beanName);
    }

    private static class PartiallyComparableAdvisorHolder implements PartialOrder.PartialComparable {

        private final Advisor advisor;

        private final Comparator<Advisor> comparator;

        public PartiallyComparableAdvisorHolder(Advisor advisor, Comparator<Advisor> comparator) {
            this.advisor = advisor;
            this.comparator = comparator;
        }

        @Override
        public int compareTo(Object obj) {
            Advisor otherAdvisor = ((PartiallyComparableAdvisorHolder) obj).advisor;
            return this.comparator.compare(this.advisor, otherAdvisor);
        }

        @Override
        public int fallbackCompareTo(Object obj) {
            return 0;
        }

        public Advisor getAdvisor() {
            return this.advisor;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            Advice advice = this.advisor.getAdvice();
            sb.append(ClassUtils.getShortName(advice.getClass()));
            sb.append(": ");
            if (this.advisor instanceof Ordered) {
                sb.append("order ").append(((Ordered) this.advisor).getOrder()).append(", ");
            }
            if (advice instanceof AbstractAspectJAdvice) {
                AbstractAspectJAdvice ajAdvice = (AbstractAspectJAdvice) advice;
                sb.append(ajAdvice.getAspectName());
                sb.append(", declaration order ");
                sb.append(ajAdvice.getDeclarationOrder());
            }
            return sb.toString();
        }
    }

}
