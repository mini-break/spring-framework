/*
 * Copyright 2002-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.beans.factory.annotation;

import java.lang.annotation.Annotation;
import java.util.Set;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.core.Ordered;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;

/**
 * A {@link org.springframework.beans.factory.config.BeanFactoryPostProcessor}
 * implementation that allows for convenient registration of custom autowire
 * qualifier types.
 *
 * <pre class="code">
 * &lt;bean id="customAutowireConfigurer" class="org.springframework.beans.factory.annotation.CustomAutowireConfigurer"&gt;
 *   &lt;property name="customQualifierTypes"&gt;
 *     &lt;set&gt;
 *       &lt;value&gt;mypackage.MyQualifier&lt;/value&gt;
 *     &lt;/set&gt;
 *   &lt;/property&gt;
 * &lt;/bean&gt;</pre>
 *
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @since 2.5
 * @see org.springframework.beans.factory.annotation.Qualifier
 *
 * 配置自定义自动装配类，允许方便地注册自定义autowire限定符类型。即使没有使用Spring的@Qualifier注解
 */
public class CustomAutowireConfigurer implements BeanFactoryPostProcessor, BeanClassLoaderAware, Ordered {

	private int order = Ordered.LOWEST_PRECEDENCE;  // default: same as non-Ordered

	/**
	 * 用户自定义装配注解（Annotation）
	 */
	@Nullable
	private Set<?> customQualifierTypes;

	@Nullable
	private ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();


	public void setOrder(int order) {
		this.order = order;
	}

	@Override
	public int getOrder() {
		return this.order;
	}

	@Override
	public void setBeanClassLoader(@Nullable ClassLoader beanClassLoader) {
		this.beanClassLoader = beanClassLoader;
	}

	/**
	 * Register custom qualifier annotation types to be considered
	 * when autowiring beans. Each element of the provided set may
	 * be either a Class instance or a String representation of the
	 * fully-qualified class name of the custom annotation.
	 * <p>Note that any annotation that is itself annotated with Spring's
	 * {@link org.springframework.beans.factory.annotation.Qualifier}
	 * does not require explicit registration.
	 * @param customQualifierTypes the custom types to register
	 */
	public void setCustomQualifierTypes(Set<?> customQualifierTypes) {
		this.customQualifierTypes = customQualifierTypes;
	}


	@Override
	@SuppressWarnings("unchecked")
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		if (this.customQualifierTypes != null) {
			// 如果 beanFactory 没有继承自 DefaultListableBeanFactory，抛出异常
			if (!(beanFactory instanceof DefaultListableBeanFactory)) {
				throw new IllegalStateException(
						"CustomAutowireConfigurer needs to operate on a DefaultListableBeanFactory");
			}
			// 将 beanFactory 强制转为 DefaultListableBeanFactory 类型
			DefaultListableBeanFactory dlbf = (DefaultListableBeanFactory) beanFactory;
			/**
			 * 如果 dlbf 中的用于检查 bean 定义的 autowire 候选者解析器 不是 QualifierAnnotationAutowireCandidateResolver 子类
			 * 设置它为 QualifierAnnotationAutowireCandidateResolver 类型
			 */
			if (!(dlbf.getAutowireCandidateResolver() instanceof QualifierAnnotationAutowireCandidateResolver)) {
				// 设置 bean 工厂用于检查bean定义是否为 autowired 候选者的解析器
				dlbf.setAutowireCandidateResolver(new QualifierAnnotationAutowireCandidateResolver());
			}
			QualifierAnnotationAutowireCandidateResolver resolver =
					(QualifierAnnotationAutowireCandidateResolver) dlbf.getAutowireCandidateResolver();
			// 遍历自定义限定符类型（ Class<? extends Annotation> 或 String 类型）
			for (Object value : this.customQualifierTypes) {
				Class<? extends Annotation> customType = null;
				// 如果是 Class 类型，强转为 Class<? extends Annotation>
				if (value instanceof Class) {
					customType = (Class<? extends Annotation>) value;
				}
				else if (value instanceof String) {
					String className = (String) value;
					// 通过类名得到类的字节码
					customType = (Class<? extends Annotation>) ClassUtils.resolveClassName(className, this.beanClassLoader);
				}
				else {
					throw new IllegalArgumentException(
							"Invalid value [" + value + "] for custom qualifier type: needs to be Class or String.");
				}
				// 如果 customType 不是 Annotation 类型，抛出异常
				if (!Annotation.class.isAssignableFrom(customType)) {
					throw new IllegalArgumentException(
							"Qualifier type [" + customType.getName() + "] needs to be annotation type");
				}
				// 将 customType 添加到用于检查bean定义是否为 autowire 候选者的解析器中
				resolver.addQualifierType(customType);
			}
		}
	}

}
