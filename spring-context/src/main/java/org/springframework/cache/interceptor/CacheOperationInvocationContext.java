/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.cache.interceptor;

import java.lang.reflect.Method;

/**
 * Representation of the context of the invocation of a cache operation.
 *
 * <p>The cache operation is static and independent of a particular invocation;
 * this interface gathers the operation and a particular invocation.
 *
 * @author Stephane Nicoll
 * @since 4.1
 *
 * 缓存执行时的上下文
 * 注意泛型O extends BasicOperation
 */
public interface CacheOperationInvocationContext<O extends BasicOperation> {

	/**
	 * 	获取缓存操作属性CacheOperation
	 * Return the cache operation.
	 */
	O getOperation();

	/**
	 * 目标类
	 * Return the target instance on which the method was invoked.
	 */
	Object getTarget();

	/**
	 * 目标方法
	 * Return the method which was invoked.
	 */
	Method getMethod();

	/**
	 * 方法入参
	 * Return the argument list used to invoke the method.
	 */
	Object[] getArgs();

}
