/*
 * Copyright (c) 2017-2020 jdjr All Rights Reserved.
 * PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 * Author Email: wangzongyao@jd.com
 */

package resource.targetsource;

import org.springframework.aop.TargetSource;
import org.springframework.util.Assert;

import java.io.Serializable;

/**
 * 单例对象 的 TargetSource.
 * @author wangzongyao on 2020/5/24
 */
public class SingletonTargetSource implements TargetSource , Serializable {

    private static final long serialVersionUID = 9031246629662423738L;

    /**
     * 使用它来保存目标对象: 就是最原始的那个玩应.
     */
    private final Object target;

    public SingletonTargetSource(Object target) {
        Assert.notNull(target, "Target object must not be null");
        this.target = target;
    }

    @Override
    public Class<?> getTargetClass() {
        return this.target.getClass();
    }

    @Override
    public Object getTarget() {
        return this.target;
    }

    @Override
    public void releaseTarget(Object target) {}

    /**
     * 标识静态的, 后面获取走Spring缓存.
     * @return
     */
    @Override
    public boolean isStatic() {
        return true;
    }
}
