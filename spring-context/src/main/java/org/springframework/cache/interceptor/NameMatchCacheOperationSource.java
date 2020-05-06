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

package org.springframework.cache.interceptor;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;
import org.springframework.util.PatternMatchUtils;

/**
 * Simple {@link CacheOperationSource} implementation that allows attributes to be matched
 * by registered name.
 *
 * @author Costin Leau
 * @since 3.1
 *
 * 根据方法名称来匹配作用在此方法上的缓存操作有哪些
 */
@SuppressWarnings("serial")
public class NameMatchCacheOperationSource implements CacheOperationSource, Serializable {

	/**
	 * Logger available to subclasses.
	 * <p>Static for optimal serialization.
	 */
	protected static final Log logger = LogFactory.getLog(NameMatchCacheOperationSource.class);


	/** Keys are method names; values are TransactionAttributes */
	private Map<String, Collection<CacheOperation>> nameMap = new LinkedHashMap<>();


	/**
	 * 配置的时候，可以调用此方法。这里使用的也是add方法
	 * 先拦截这个方法，然后看看这个方法有木有匹配的缓存操作，有点想AOP的配置
	 *
	 * Set a name/attribute map, consisting of method names
	 * (e.g. "myMethod") and CacheOperation instances
	 * (or Strings to be converted to CacheOperation instances).
	 * @see CacheOperation
	 */
	public void setNameMap(Map<String, Collection<CacheOperation>> nameMap) {
		nameMap.forEach(this::addCacheMethod);
	}

	/**
	 * Add an attribute for a cacheable method.
	 * <p>Method names can be exact matches, or of the pattern "xxx*",
	 * "*xxx" or "*xxx*" for matching multiple methods.
	 * @param methodName the name of the method
	 * @param ops operation associated with the method
	 */
	public void addCacheMethod(String methodName, Collection<CacheOperation> ops) {
		if (logger.isDebugEnabled()) {
			logger.debug("Adding method [" + methodName + "] with cache operations [" + ops + "]");
		}
		this.nameMap.put(methodName, ops);
	}

	@Override
	@Nullable
	public Collection<CacheOperation> getCacheOperations(Method method, @Nullable Class<?> targetClass) {
		// look for direct name match
		String methodName = method.getName();
		Collection<CacheOperation> ops = this.nameMap.get(methodName);

		if (ops == null) {
			// Look for most specific name match.
			String bestNameMatch = null;
			// 遍历所有外部已经制定进来了的方法名
			for (String mappedName : this.nameMap.keySet()) {
				/**
				 * isMatch就是Ant风格匹配
				 * 第一步：光Ant匹配上了还不算
				 * 第二步：bestNameMatch=null或者
				 */
				if (isMatch(methodName, mappedName)
						&& (bestNameMatch == null || bestNameMatch.length() <= mappedName.length())) {
					ops = this.nameMap.get(mappedName);
					bestNameMatch = mappedName;
				}
			}
		}

		return ops;
	}

	/**
	 * Return if the given method name matches the mapped name.
	 * <p>The default implementation checks for "xxx*", "*xxx" and "*xxx*" matches,
	 * as well as direct equality. Can be overridden in subclasses.
	 * @param methodName the method name of the class
	 * @param mappedName the name in the descriptor
	 * @return if the names match
	 * @see org.springframework.util.PatternMatchUtils#simpleMatch(String, String)
	 */
	protected boolean isMatch(String methodName, String mappedName) {
		return PatternMatchUtils.simpleMatch(mappedName, methodName);
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof NameMatchCacheOperationSource)) {
			return false;
		}
		NameMatchCacheOperationSource otherTas = (NameMatchCacheOperationSource) other;
		return ObjectUtils.nullSafeEquals(this.nameMap, otherTas.nameMap);
	}

	@Override
	public int hashCode() {
		return NameMatchCacheOperationSource.class.hashCode();
	}

	@Override
	public String toString() {
		return getClass().getName() + ": " + this.nameMap;
	}
}
