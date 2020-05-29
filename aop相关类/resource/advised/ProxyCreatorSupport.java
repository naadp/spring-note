/*
 * Copyright (c) 2017-2020 jdjr All Rights Reserved.
 * PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 * Author Email: wangzongyao@jd.com
 */

package resource.advised;

import org.springframework.aop.framework.*;
import org.springframework.aop.framework.AdvisedSupport;
import org.springframework.util.Assert;

import java.util.LinkedList;
import java.util.List;

/**
 * @author wangzongyao on 2020/5/28
 */
public class ProxyCreatorSupport extends AdvisedSupport {

    private AopProxyFactory aopProxyFactory;

    private final List<AdvisedSupportListener> listeners = new LinkedList<>();

    /** Set to true when the first AOP proxy has been created */
    private boolean active = false;

    public ProxyCreatorSupport() {
        this.aopProxyFactory = new DefaultAopProxyFactory();
    }

    public ProxyCreatorSupport(AopProxyFactory aopProxyFactory) {
        Assert.notNull(aopProxyFactory, "AopProxyFactory must not be null");
        this.aopProxyFactory = aopProxyFactory;
    }

    public void setAopProxyFactory(AopProxyFactory aopProxyFactory) {
        Assert.notNull(aopProxyFactory, "AopProxyFactory must not be null");
        this.aopProxyFactory = aopProxyFactory;
    }

    public AopProxyFactory getAopProxyFactory() { return this.aopProxyFactory; }

    public void addListener(AdvisedSupportListener listener) {
        Assert.notNull(listener, "AdvisedSupportListener must not be null");
        this.listeners.add(listener);
    }

    public void removeListener(AdvisedSupportListener listener) {
        Assert.notNull(listener, "AdvisedSupportListener must not be null");
        this.listeners.remove(listener);
    }

    protected final synchronized AopProxy createAopProxy() {
        if (!this.active) {
            activate();
        }
        return getAopProxyFactory().createAopProxy(this);
    }

    private void activate() {
        this.active = true;
        for (AdvisedSupportListener listener : this.listeners) {
            listener.activated(this);
        }
    }

    @Override
    protected void adviceChanged() {
        super.adviceChanged();
        synchronized (this) {
            if (this.active) {
                for (AdvisedSupportListener listener : this.listeners) {
                    listener.adviceChanged(this);
                }
            }
        }
    }

    protected final synchronized boolean isActive() {
        return this.active;
    }

}
