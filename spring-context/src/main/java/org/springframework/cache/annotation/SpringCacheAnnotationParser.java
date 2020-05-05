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

package org.springframework.cache.annotation;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;

import org.springframework.cache.interceptor.CacheEvictOperation;
import org.springframework.cache.interceptor.CacheOperation;
import org.springframework.cache.interceptor.CachePutOperation;
import org.springframework.cache.interceptor.CacheableOperation;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Strategy implementation for parsing Spring's {@link Caching}, {@link Cacheable},
 * {@link CacheEvict}, and {@link CachePut} annotations.
 *
 * @author Costin Leau
 * @author Juergen Hoeller
 * @author Chris Beams
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @author Sam Brannen
 * @since 3.1
 */
@SuppressWarnings("serial")
public class SpringCacheAnnotationParser implements CacheAnnotationParser, Serializable {

	/**
	 * 处理class和Method
	 * 使用DefaultCacheConfig，把它传给parseCacheAnnotations()  来给注解属性搞定默认值
	 * 至于为何自己要新new一个呢？其实是因为@CacheConfig它只能放在类上（传Method也能获取到类）
	 * 返回值都可以为null（没有标注此注解方法、类  那肯定返回null啊）
	 */
	@Override
	@Nullable
	public Collection<CacheOperation> parseCacheAnnotations(Class<?> type) {
		DefaultCacheConfig defaultConfig = getDefaultCacheConfig(type);
		return parseCacheAnnotations(defaultConfig, type);
	}

	@Override
	@Nullable
	public Collection<CacheOperation> parseCacheAnnotations(Method method) {
		DefaultCacheConfig defaultConfig = getDefaultCacheConfig(method.getDeclaringClass());
		return parseCacheAnnotations(defaultConfig, method);
	}

	/**
	 * 找到方法/类上的注解们
	 */
	@Nullable
	private Collection<CacheOperation> parseCacheAnnotations(DefaultCacheConfig cachingConfig, AnnotatedElement ae) {
		// 第三个参数传的false：表示接口的注解它也会看一下
		Collection<CacheOperation> ops = parseCacheAnnotations(cachingConfig, ae, false);
		// 若出现接口方法里也标了，实例方法里也标了，那就继续处理。让以实例方法里标注的为准
		if (ops != null && ops.size() > 1 && ae.getAnnotations().length > 0) {
			// More than one operation found -> local declarations override interface-declared ones...
			Collection<CacheOperation> localOps = parseCacheAnnotations(cachingConfig, ae, true);
			if (localOps != null) {
				return localOps;
			}
		}
		return ops;
	}

	@Nullable
	private Collection<CacheOperation> parseCacheAnnotations(
			DefaultCacheConfig cachingConfig, AnnotatedElement ae, boolean localOnly) {

		Collection<CacheOperation> ops = null;

		// localOnly=true，只看自己的不看接口的。false表示接口的也得看
		Collection<Cacheable> cacheables = (localOnly ? AnnotatedElementUtils.getAllMergedAnnotations(ae, Cacheable.class) :
				AnnotatedElementUtils.findAllMergedAnnotations(ae, Cacheable.class));
		if (!cacheables.isEmpty()) {
			ops = lazyInit(null);
			for (Cacheable cacheable : cacheables) {
				ops.add(parseCacheableAnnotation(ae, cachingConfig, cacheable));
			}
		}

		Collection<CacheEvict> evicts = (localOnly ? AnnotatedElementUtils.getAllMergedAnnotations(ae, CacheEvict.class) :
				AnnotatedElementUtils.findAllMergedAnnotations(ae, CacheEvict.class));
		if (!evicts.isEmpty()) {
			ops = lazyInit(ops);
			for (CacheEvict evict : evicts) {
				ops.add(parseEvictAnnotation(ae, cachingConfig, evict));
			}
		}

		Collection<CachePut> puts = (localOnly ? AnnotatedElementUtils.getAllMergedAnnotations(ae, CachePut.class) :
				AnnotatedElementUtils.findAllMergedAnnotations(ae, CachePut.class));
		if (!puts.isEmpty()) {
			ops = lazyInit(ops);
			for (CachePut put : puts) {
				ops.add(parsePutAnnotation(ae, cachingConfig, put));
			}
		}

		Collection<Caching> cachings = (localOnly ? AnnotatedElementUtils.getAllMergedAnnotations(ae, Caching.class) :
				AnnotatedElementUtils.findAllMergedAnnotations(ae, Caching.class));
		if (!cachings.isEmpty()) {
			ops = lazyInit(ops);
			for (Caching caching : cachings) {
				Collection<CacheOperation> cachingOps = parseCachingAnnotation(ae, cachingConfig, caching);
				if (cachingOps != null) {
					ops.addAll(cachingOps);
				}
			}
		}

		return ops;
	}

	private <T extends Annotation> Collection<CacheOperation> lazyInit(@Nullable Collection<CacheOperation> ops) {
		// 这里为何写个1，因为绝大多数情况下，我们都只会标注一个注解
		return (ops != null ? ops : new ArrayList<>(1));
	}

	CacheableOperation parseCacheableAnnotation(AnnotatedElement ae, DefaultCacheConfig defaultConfig, Cacheable cacheable) {
		// 这个builder是CacheOperation.Builder的子类，父类规定了所有注解通用的一些属性
		CacheableOperation.Builder builder = new CacheableOperation.Builder();

		builder.setName(ae.toString());
		builder.setCacheNames(cacheable.cacheNames());
		builder.setCondition(cacheable.condition());
		builder.setUnless(cacheable.unless());
		builder.setKey(cacheable.key());
		builder.setKeyGenerator(cacheable.keyGenerator());
		builder.setCacheManager(cacheable.cacheManager());
		builder.setCacheResolver(cacheable.cacheResolver());
		builder.setSync(cacheable.sync());

		// DefaultCacheConfig是本类的一个内部类，来处理buider，给他赋值默认值，比如默认的keyGenerator等等
		defaultConfig.applyDefault(builder);
		CacheableOperation op = builder.build();
		// 校验。key和KeyGenerator至少得有一个，CacheManager和CacheResolver至少得配置一个
		validateCacheOperation(ae, op);

		return op;
	}

	CacheEvictOperation parseEvictAnnotation(AnnotatedElement ae, DefaultCacheConfig defaultConfig, CacheEvict cacheEvict) {
		CacheEvictOperation.Builder builder = new CacheEvictOperation.Builder();

		builder.setName(ae.toString());
		builder.setCacheNames(cacheEvict.cacheNames());
		builder.setCondition(cacheEvict.condition());
		builder.setKey(cacheEvict.key());
		builder.setKeyGenerator(cacheEvict.keyGenerator());
		builder.setCacheManager(cacheEvict.cacheManager());
		builder.setCacheResolver(cacheEvict.cacheResolver());
		builder.setCacheWide(cacheEvict.allEntries());
		builder.setBeforeInvocation(cacheEvict.beforeInvocation());

		defaultConfig.applyDefault(builder);
		CacheEvictOperation op = builder.build();
		validateCacheOperation(ae, op);

		return op;
	}

	CacheOperation parsePutAnnotation(AnnotatedElement ae, DefaultCacheConfig defaultConfig, CachePut cachePut) {
		CachePutOperation.Builder builder = new CachePutOperation.Builder();

		builder.setName(ae.toString());
		builder.setCacheNames(cachePut.cacheNames());
		builder.setCondition(cachePut.condition());
		builder.setUnless(cachePut.unless());
		builder.setKey(cachePut.key());
		builder.setKeyGenerator(cachePut.keyGenerator());
		builder.setCacheManager(cachePut.cacheManager());
		builder.setCacheResolver(cachePut.cacheResolver());

		defaultConfig.applyDefault(builder);
		CachePutOperation op = builder.build();
		validateCacheOperation(ae, op);

		return op;
	}

	@Nullable
	Collection<CacheOperation> parseCachingAnnotation(AnnotatedElement ae, DefaultCacheConfig defaultConfig, Caching caching) {
		Collection<CacheOperation> ops = null;

		/**
		 * 这里的方法parseCacheableAnnotation/parsePutAnnotation等 说白了  就是把注解属性，转换封装成为`CacheOperation`对象
		 */

		Cacheable[] cacheables = caching.cacheable();
		if (!ObjectUtils.isEmpty(cacheables)) {
			ops = lazyInit(null);
			for (Cacheable cacheable : cacheables) {
				ops.add(parseCacheableAnnotation(ae, defaultConfig, cacheable));
			}
		}
		CacheEvict[] cacheEvicts = caching.evict();
		if (!ObjectUtils.isEmpty(cacheEvicts)) {
			ops = lazyInit(ops);
			for (CacheEvict cacheEvict : cacheEvicts) {
				ops.add(parseEvictAnnotation(ae, defaultConfig, cacheEvict));
			}
		}
		CachePut[] cachePuts = caching.put();
		if (!ObjectUtils.isEmpty(cachePuts)) {
			ops = lazyInit(ops);
			for (CachePut cachePut : cachePuts) {
				ops.add(parsePutAnnotation(ae, defaultConfig, cachePut));
			}
		}

		return ops;
	}

	/**
	 * Provides the {@link DefaultCacheConfig} instance for the specified {@link Class}.
	 * @param target the class-level to handle
	 * @return the default config (never {@code null})
	 */
	DefaultCacheConfig getDefaultCacheConfig(Class<?> target) {
		CacheConfig annotation = AnnotatedElementUtils.findMergedAnnotation(target, CacheConfig.class);
		if (annotation != null) {
			return new DefaultCacheConfig(annotation.cacheNames(), annotation.keyGenerator(),
					annotation.cacheManager(), annotation.cacheResolver());
		}
		return new DefaultCacheConfig();
	}

	/**
	 * Validates the specified {@link CacheOperation}.
	 * <p>Throws an {@link IllegalStateException} if the state of the operation is
	 * invalid. As there might be multiple sources for default values, this ensure
	 * that the operation is in a proper state before being returned.
	 * @param ae the annotated element of the cache operation
	 * @param operation the {@link CacheOperation} to validate
	 */
	private void validateCacheOperation(AnnotatedElement ae, CacheOperation operation) {
		if (StringUtils.hasText(operation.getKey()) && StringUtils.hasText(operation.getKeyGenerator())) {
			throw new IllegalStateException("Invalid cache annotation configuration on '" +
					ae.toString() + "'. Both 'key' and 'keyGenerator' attributes have been set. " +
					"These attributes are mutually exclusive: either set the SpEL expression used to" +
					"compute the key at runtime or set the name of the KeyGenerator bean to use.");
		}
		if (StringUtils.hasText(operation.getCacheManager()) && StringUtils.hasText(operation.getCacheResolver())) {
			throw new IllegalStateException("Invalid cache annotation configuration on '" +
					ae.toString() + "'. Both 'cacheManager' and 'cacheResolver' attributes have been set. " +
					"These attributes are mutually exclusive: the cache manager is used to configure a" +
					"default cache resolver if none is set. If a cache resolver is set, the cache manager" +
					"won't be used.");
		}
	}

	@Override
	public boolean equals(Object other) {
		return (this == other || other instanceof SpringCacheAnnotationParser);
	}

	@Override
	public int hashCode() {
		return SpringCacheAnnotationParser.class.hashCode();
	}


	/**
	 * 设置默认值的私有静态内部类
	 * Provides default settings for a given set of cache operations.
	 */
	static class DefaultCacheConfig {

		@Nullable
		private final String[] cacheNames;

		@Nullable
		private final String keyGenerator;

		@Nullable
		private final String cacheManager;

		@Nullable
		private final String cacheResolver;

		public DefaultCacheConfig() {
			this(null, null, null, null);
		}

		private DefaultCacheConfig(@Nullable String[] cacheNames, @Nullable String keyGenerator,
				@Nullable String cacheManager, @Nullable String cacheResolver) {

			this.cacheNames = cacheNames;
			this.keyGenerator = keyGenerator;
			this.cacheManager = cacheManager;
			this.cacheResolver = cacheResolver;
		}

		/**
		 * Apply the defaults to the specified {@link CacheOperation.Builder}.
		 * @param builder the operation builder to update
		 */
		public void applyDefault(CacheOperation.Builder builder) {
			if (builder.getCacheNames().isEmpty() && this.cacheNames != null) {
				builder.setCacheNames(this.cacheNames);
			}
			if (!StringUtils.hasText(builder.getKey()) && !StringUtils.hasText(builder.getKeyGenerator()) &&
					StringUtils.hasText(this.keyGenerator)) {
				builder.setKeyGenerator(this.keyGenerator);
			}

			if (StringUtils.hasText(builder.getCacheManager()) || StringUtils.hasText(builder.getCacheResolver())) {
				// One of these is set so we should not inherit anything
			}
			else if (StringUtils.hasText(this.cacheResolver)) {
				builder.setCacheResolver(this.cacheResolver);
			}
			else if (StringUtils.hasText(this.cacheManager)) {
				builder.setCacheManager(this.cacheManager);
			}
		}
	}

}
