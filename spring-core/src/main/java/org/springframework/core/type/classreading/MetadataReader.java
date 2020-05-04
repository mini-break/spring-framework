/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.core.type.classreading;

import org.springframework.core.io.Resource;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.ClassMetadata;

/**
 * Simple facade for accessing class metadata,
 * as read by an ASM {@link org.springframework.asm.ClassReader}.
 *
 * @author Juergen Hoeller
 * @since 2.5
 *
 * 通过ASM读取class IO流资源组装访问元数据的门面接口
 */
public interface MetadataReader {

	/**
	 * 返回class文件的IO资源引用
	 * （创建的时候需要指定此资源，然后交给`AnnotationMetadataReadingVisitor`去处理）
	 * Return the resource reference for the class file.
	 */
	Resource getResource();

	/**
	 * 为基础class读取基本类元数据，返回基础类的元数据
	 * 实现为通过`AnnotationMetadataReadingVisitor`中获取
	 * Read basic class metadata for the underlying class.
	 */
	ClassMetadata getClassMetadata();

	/**
	 * 为基础类读取完整的注释元数据，包括注释方法的元数据。返回基础类的完整注释元数据
	 * 通过`AnnotationMetadataReadingVisitor`获取
	 * Read full annotation metadata for the underlying class,
	 * including metadata for annotated methods.
	 */
	AnnotationMetadata getAnnotationMetadata();

}
