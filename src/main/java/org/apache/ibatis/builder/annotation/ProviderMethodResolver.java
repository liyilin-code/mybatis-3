/*
 *    Copyright 2009-2023 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.apache.ibatis.builder.annotation;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.ibatis.builder.BuilderException;

/**
 * SQL Provider接口，提供了默认方法resolveMethod，查询匹配的Provider方法
 * @SelectProvider注解可以指定SQL提供者类，当指定的类实现了ProviderMethodResolver接口时，会采用策略决定当前Provider类中哪一个方法用于提供当前Mapper方法的SQL语句
 * 策略如下：
 * 1. 找到Provider类中method名与所在Mapper接口方法的名一致
 * 2. 方法返回类型为String
 * 所以如果Provider实现了ProviderMethodResolver接口，就不需要指定哪个method来提供SQL语句了
 *
 * The interface that resolve an SQL provider method via an SQL provider class.
 * <p>
 * This interface need to implements at an SQL provider class and it need to define the default constructor for creating
 * a new instance.
 *
 * @since 3.5.1
 *
 * @author Kazuki Shimizu
 */
public interface ProviderMethodResolver {

  /**
   * Resolve an SQL provider method.
   * <p>
   * The default implementation return a method that matches following conditions.
   * <ul>
   * <li>Method name matches with mapper method</li>
   * <li>Return type matches the {@link CharSequence}({@link String}, {@link StringBuilder}, etc...)</li>
   * </ul>
   * If matched method is zero or multiple, it throws a {@link BuilderException}.
   *
   * @param context
   *          a context for SQL provider
   *
   * @return an SQL provider method
   *
   * @throws BuilderException
   *           Throws when cannot resolve a target method
   */
  default Method resolveMethod(ProviderContext context) {
    // 1. 当前实现中method名称和所在Mapper方法名称一致
    List<Method> sameNameMethods = Arrays.stream(getClass().getMethods())
        .filter(m -> m.getName().equals(context.getMapperMethod().getName())).collect(Collectors.toList());
    if (sameNameMethods.isEmpty()) {
      throw new BuilderException("Cannot resolve the provider method because '" + context.getMapperMethod().getName()
          + "' not found in SqlProvider '" + getClass().getName() + "'.");
    }
    // 2. 方法返回类型为String
    List<Method> targetMethods = sameNameMethods.stream()
        .filter(m -> CharSequence.class.isAssignableFrom(m.getReturnType())).collect(Collectors.toList());
    if (targetMethods.size() == 1) {
      return targetMethods.get(0);
    }
    if (targetMethods.isEmpty()) {
      throw new BuilderException("Cannot resolve the provider method because '" + context.getMapperMethod().getName()
          + "' does not return the CharSequence or its subclass in SqlProvider '" + getClass().getName() + "'.");
    }
    throw new BuilderException("Cannot resolve the provider method because '" + context.getMapperMethod().getName()
        + "' is found multiple in SqlProvider '" + getClass().getName() + "'.");
  }

}
