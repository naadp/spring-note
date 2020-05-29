/*
 * Copyright (c) 2017-2020 jdjr All Rights Reserved.
 * PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 * Author Email: wangzongyao@jd.com
 */

package resource.targetsource;

import org.springframework.aop.TargetClassAware;
import org.springframework.aop.framework.autoproxy.AbstractAutoProxyCreator;
import org.springframework.aop.target.CommonsPool2TargetSource;
import org.springframework.aop.target.PrototypeTargetSource;
import org.springframework.aop.target.ThreadLocalTargetSource;

/**
 * 该接口代表一个目标对象，在aop调用目标对象的时候，使用该接口返回真实的对象.
 * 有很多实现: ↓
 * @see org.springframework.aop.target.SingletonTargetSource 一般是在这个.
 * @see PrototypeTargetSource
 * @see CommonsPool2TargetSource
 * @see ThreadLocalTargetSource
 *
 * {@link AbstractAutoProxyCreator#postProcessBeforeInstantiation}
 *     允许我们使用自己的TargetSource来生成原始对象, 之后会再加入增强.
 *     但是我没做成功哈, 有个TargetSourceCreators 不知道怎么给它设置值, 也不知道它有什么意义.
 *     所以就先略过了.
 * @author wangzongyao on 2020/5/24
 */
public interface TargetSource extends TargetClassAware {

    /**
     * 返回目标bean的Class类型.
     */
    @Override
    Class<?> getTargetClass();

    /**
     * 返回当前bean是否为静态的: 常见的单例bean就是静态的, 而prototype就是动态的.
     * 对于静态的bean，spring是会对其进行缓存的, 在多次使用TargetSource,
     * 获取目标bean对象的时候，其获取的总是同一个对象, 通过这种方式提高效率.
     *
     * 看源码的调用位置, 只在方法调用完毕判断是否 releaseTarget时有判断调用.
     */
    boolean isStatic();

    /**
     * 获取目标bean对象，这里可以根据业务需要进行自行定制.
     */
    Object getTarget() throws Exception;

    /**
     * Spring在完目标bean之后会调用这个方法释放目标bean对象，对于一些需要池化的对象，这个方法是必须
     * 要实现的，这个方法默认不进行任何处理
     */
    void releaseTarget(Object target) throws Exception;

}
