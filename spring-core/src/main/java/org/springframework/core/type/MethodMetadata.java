/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.core.type;

/**
 * Interface that defines abstract access to the annotations of a specific
 * class, in a form that does not require that class to be loaded yet.
 *
 * @author Juergen Hoeller
 * @author Mark Pollack
 * @author Chris Beams
 * @author Phillip Webb
 * @since 3.0
 * @see StandardMethodMetadata
 * @see AnnotationMetadata#getAnnotatedMethods
 * @see AnnotatedTypeMetadata
 *
 * 类元数据模型在包org.springframework.core.type下，是spring对class文件的描述单元，
 * 包含ClassMetadata(提供对class类的信息访问)，MethodMetadata（提供对class类里方法信息的访问），AnnotationMetadata（提供对class类里的注解信息访问）等元数据，都是对外提供对class属性的访问，
 * 同时这些元数据是通过ASM字节码框架解析字节码获取生成。
 */
public interface MethodMetadata extends AnnotatedTypeMetadata {

	/**
	 * 方法名称
	 * Return the name of the method.
	 */
	String getMethodName();

	/**
	 * 此方法所属类的全类名
	 * Return the fully-qualified name of the class that declares this method.
	 */
	String getDeclaringClassName();

	/**
	 * 方法返回值的全类名
	 * Return the fully-qualified name of this method's declared return type.
	 * @since 4.2
	 */
	String getReturnTypeName();

	/**
	 * 是否是抽象方法
	 * Return whether the underlying method is effectively abstract:
	 * i.e. marked as abstract on a class or declared as a regular,
	 * non-default method in an interface.
	 * @since 4.2
	 */
	boolean isAbstract();

	/**
	 * 是否是静态方法
	 * Return whether the underlying method is declared as 'static'.
	 */
	boolean isStatic();

	/**
	 * 是否是final方法
	 * Return whether the underlying method is marked as 'final'.
	 */
	boolean isFinal();

	/**
	 * 是否可以被复写（不是静态、不是final、不是private的  就表示可以被复写）
	 * Return whether the underlying method is overridable,
	 * i.e. not marked as static, final or private.
	 */
	boolean isOverridable();

}
