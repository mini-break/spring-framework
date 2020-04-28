/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.expression;

import java.util.List;

import org.springframework.lang.Nullable;

/**
 * Expressions are executed in an evaluation context. It is in this context that
 * references are resolved when encountered during expression evaluation.
 *
 * <p>There is a default implementation of this EvaluationContext interface:
 * {@link org.springframework.expression.spel.support.StandardEvaluationContext}
 * which can be extended, rather than having to implement everything manually.
 *
 * @author Andy Clement
 * @author Juergen Hoeller
 * @since 3.0
 */
public interface EvaluationContext {

	/**
	 * 上下文可以持有一个根对象
	 * Return the default root context object against which unqualified
	 * properties/methods/etc should be resolved. This can be overridden
	 * when evaluating an expression.
	 */
	TypedValue getRootObject();

	/**
	 * 返回属性访问器列表，这些访问器将依次被要求读取/写入属性  注意此处的属性访问器是el包自己的，不是bean包下的~~~
	 * ReflectivePropertyAccessor（DataBindingPropertyAccessor）：通过反射读/写对象的属性~
	 * BeanFactoryAccessor：这个属性访问器让支持bean从bean工厂里获取
	 * EnvironmentAccessor：可以从环境中.getProperty(name)
	 * BeanExpressionContextAccessor：和BeanExpressionContext相关
	 * MapAccessor：可以从map中获取值
	 *
	 * Return a list of accessors that will be asked in turn to read/write a property.
	 */
	List<PropertyAccessor> getPropertyAccessors();

	/**
	 * ConstructorResolver它只有一个实现：ReflectiveConstructorResolver
	 *
	 * Return a list of resolvers that will be asked in turn to locate a constructor.
	 */
	List<ConstructorResolver> getConstructorResolvers();

	/**
	 * 它的实现：ReflectiveMethodResolver/DataBindingMethodResolver
	 *
	 * Return a list of resolvers that will be asked in turn to locate a method.
	 */
	List<MethodResolver> getMethodResolvers();

	/**
	 * 返回一个处理器：它能够通过beanName找到bean
	 * BeanResolver：唯一实现 BeanFactoryResolver  它内部持有BeanFactory的引用
	 *
	 * Return a bean resolver that can look up beans by name.
	 */
	@Nullable
	BeanResolver getBeanResolver();

	/**
	 * 返回一个类型定位器，该定位器可用于通过短名称或完全限定名称查找类型 唯一实现：StandardTypeLocator
	 *
	 * Return a type locator that can be used to find types, either by short or
	 * fully qualified name.
	 */
	TypeLocator getTypeLocator();

	/**
	 * TypeConverter：唯一实现为StandardTypeConverter  其实还是依赖DefaultConversionService的
	 *
	 * Return a type converter that can convert (or coerce) a value from one type to another.
	 */
	TypeConverter getTypeConverter();

	/**
	 * Return a type comparator for comparing pairs of objects for equality.
	 */
	TypeComparator getTypeComparator();

	/**
	 * Return an operator overloader that may support mathematical operations
	 * between more than the standard set of types.
	 */
	OperatorOverloader getOperatorOverloader();

	/**
	 * Set a named variable within this evaluation context to a specified value.
	 * @param name variable to set
	 * @param value value to be placed in the variable
	 */
	void setVariable(String name, @Nullable Object value);

	/**
	 * Look up a named variable within this evaluation context.
	 * @param name variable to lookup
	 * @return the value of the variable, or {@code null} if not found
	 */
	@Nullable
	Object lookupVariable(String name);

}
