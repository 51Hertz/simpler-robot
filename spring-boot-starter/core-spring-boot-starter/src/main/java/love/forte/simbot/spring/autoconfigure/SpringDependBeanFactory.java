/*
 * Copyright (c) 2020. ForteScarlet All rights reserved.
 * Project  parent
 * File     SpringDependBeanFactory.java
 *
 * You can contact the author through the following channels:
 * github https://github.com/ForteScarlet
 * gitee  https://gitee.com/ForteScarlet
 * email  ForteScarlet@163.com
 * QQ     1149159218
 */

package love.forte.simbot.spring.autoconfigure;

import love.forte.common.ioc.DependBeanFactory;
import love.forte.common.ioc.exception.DependException;
import love.forte.common.ioc.exception.NoSuchDependException;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 *
 * spring的 {@link DependBeanFactory} 实现。
 *
 * @author <a href="https://github.com/ForteScarlet"> ForteScarlet </a>
 */
@Configuration
public class SpringDependBeanFactory implements DependBeanFactory {

    private final ListableBeanFactory beanFactory;

    public SpringDependBeanFactory(ListableBeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }

    @Override
    public <T> T get(Class<T> type) {
        try {
            return beanFactory.getBean(type);
        } catch (NoSuchBeanDefinitionException e) {
            throw new NoSuchDependException(e);
        }

    }

    @Override
    public <T> T get(Class<T> type, String name) {
        try {
            return beanFactory.getBean(type, name);
        } catch (NoSuchBeanDefinitionException e) {
            throw new NoSuchDependException(e);
        }
    }

    @Override
    public Object get(String name) {
        try {
            return beanFactory.getBean(name);
        } catch (NoSuchBeanDefinitionException e) {
            throw new NoSuchDependException(e);
        }

    }

    @Override
    public <T> T getOrNull(Class<T> type) {
        try {
            return beanFactory.getBean(type);
        } catch (NoSuchBeanDefinitionException e) {
            return null;
        }
    }

    @Override
    public <T> T getOrNull(Class<T> type, String name) {
        try {
            return beanFactory.getBean(type, name);
        } catch (NoSuchBeanDefinitionException e) {
            return null;
        }
    }

    @Override
    public Object getOrNull(String name) {
        try {
            return beanFactory.getBean(name);
        } catch (NoSuchBeanDefinitionException e) {
            return null;
        }
    }

    @Override
    public <T> T getOrThrow(Class<T> type, Function<NoSuchDependException, DependException> exceptionCompute) {
        try {
            return beanFactory.getBean(type);
        } catch (NoSuchBeanDefinitionException e) {
            throw exceptionCompute.apply(new NoSuchDependException(e));
        }
    }

    @Override
    public <T> T getOrThrow(Class<T> type, String name, Function<NoSuchDependException, DependException> exceptionCompute) {
        try {
            return beanFactory.getBean(type, name);
        } catch (NoSuchBeanDefinitionException e) {
            throw exceptionCompute.apply(new NoSuchDependException(e));
        }
    }

    @Override
    public Object getOrThrow(String name, Function<NoSuchDependException, DependException> exceptionCompute) {
        try {
            return beanFactory.getBean(name);
        } catch (NoSuchBeanDefinitionException e) {
            throw exceptionCompute.apply(new NoSuchDependException(e));
        }
    }

    @Override
    public Set<String> getAllBeans() {
        final String[] beanDefinitionNames = beanFactory.getBeanDefinitionNames();
        return Arrays.stream(beanDefinitionNames).collect(Collectors.toSet());
    }

    @Override
    public Class<?> getType(String name) {
        try {
            return beanFactory.getType(name);
        } catch (NoSuchBeanDefinitionException e) {
             return null;
        }
    }
}
