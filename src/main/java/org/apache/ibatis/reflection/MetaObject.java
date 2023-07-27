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
package org.apache.ibatis.reflection;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.reflection.factory.ObjectFactory;
import org.apache.ibatis.reflection.property.PropertyTokenizer;
import org.apache.ibatis.reflection.wrapper.BeanWrapper;
import org.apache.ibatis.reflection.wrapper.CollectionWrapper;
import org.apache.ibatis.reflection.wrapper.MapWrapper;
import org.apache.ibatis.reflection.wrapper.ObjectWrapper;
import org.apache.ibatis.reflection.wrapper.ObjectWrapperFactory;

/**
 * 与MetaClass对应，MetaClass提供了查看Class细节的能力，MetaObject提供了查看Object细节的能力
 *
 * @author Clinton Begin
 */
public class MetaObject {

  private final Object originalObject;
  private final ObjectWrapper objectWrapper;
  private final ObjectFactory objectFactory;
  private final ObjectWrapperFactory objectWrapperFactory;
  private final ReflectorFactory reflectorFactory;

  private MetaObject(Object object, ObjectFactory objectFactory, ObjectWrapperFactory objectWrapperFactory,
      ReflectorFactory reflectorFactory) {
    this.originalObject = object;
    this.objectFactory = objectFactory;
    this.objectWrapperFactory = objectWrapperFactory;
    this.reflectorFactory = reflectorFactory;

    if (object instanceof ObjectWrapper) {
      this.objectWrapper = (ObjectWrapper) object;
    } else if (objectWrapperFactory.hasWrapperFor(object)) {
      this.objectWrapper = objectWrapperFactory.getWrapperFor(this, object);
    } else if (object instanceof Map) {
      this.objectWrapper = new MapWrapper(this, (Map) object);
    } else if (object instanceof Collection) {
      this.objectWrapper = new CollectionWrapper(this, (Collection) object);
    } else {
      this.objectWrapper = new BeanWrapper(this, object);
    }
  }

  public static MetaObject forObject(Object object, ObjectFactory objectFactory,
      ObjectWrapperFactory objectWrapperFactory, ReflectorFactory reflectorFactory) {
    if (object == null) {
      return SystemMetaObject.NULL_META_OBJECT;
    }
    return new MetaObject(object, objectFactory, objectWrapperFactory, reflectorFactory);
  }

  public ObjectFactory getObjectFactory() {
    return objectFactory;
  }

  public ObjectWrapperFactory getObjectWrapperFactory() {
    return objectWrapperFactory;
  }

  public ReflectorFactory getReflectorFactory() {
    return reflectorFactory;
  }

  public Object getOriginalObject() {
    return originalObject;
  }

  /**
   * 查看对象中propName对应的属性名，支持连字符转换为驼峰
   * 最终调用的MetaClass.findProperty
   */
  public String findProperty(String propName, boolean useCamelCaseMapping) {
    return objectWrapper.findProperty(propName, useCamelCaseMapping);
  }

  /**
   * 获取属性名，注意如果是JavaBean对象，和MetaClass.getGetterNames一致，返回对象属性名
   *     但是如果是Map对象，返回的是键名称集合
   * @return
   */
  public String[] getGetterNames() {
    return objectWrapper.getGetterNames();
  }

  /**
   * 和getGetterNames一致
   * @return
   */
  public String[] getSetterNames() {
    return objectWrapper.getSetterNames();
  }

  /**
   * 返回name属性对应的类型
   * @param name
   * @return
   */
  public Class<?> getSetterType(String name) {
    return objectWrapper.getSetterType(name);
  }

  /**
   * 和getSetterType一致
   * @param name
   * @return
   */
  public Class<?> getGetterType(String name) {
    return objectWrapper.getGetterType(name);
  }

  /**
   * 是否有name匹配的属性
   * @param name
   * @return
   */
  public boolean hasSetter(String name) {
    return objectWrapper.hasSetter(name);
  }

  /**
   * 是否有name匹配的属性
   * @param name
   * @return
   */
  public boolean hasGetter(String name) {
    return objectWrapper.hasGetter(name);
  }

  /**
   * 获取name属性对应的值
   * @param name
   * @return
   */
  public Object getValue(String name) {
    PropertyTokenizer prop = new PropertyTokenizer(name);
    if (!prop.hasNext()) {
      return objectWrapper.get(prop);
    }
    MetaObject metaValue = metaObjectForProperty(prop.getIndexedName());
    if (metaValue == SystemMetaObject.NULL_META_OBJECT) {
      return null;
    } else {
      return metaValue.getValue(prop.getChildren());
    }
  }

  /**
   * 设置name属性为value
   * @param name
   * @param value
   */
  public void setValue(String name, Object value) {
    PropertyTokenizer prop = new PropertyTokenizer(name);
    if (prop.hasNext()) {
      MetaObject metaValue = metaObjectForProperty(prop.getIndexedName());
      if (metaValue == SystemMetaObject.NULL_META_OBJECT) {
        if (value == null) {
          // don't instantiate child path if value is null
          return;
        }
        metaValue = objectWrapper.instantiatePropertyValue(name, prop, objectFactory);
      }
      metaValue.setValue(prop.getChildren(), value);
    } else {
      objectWrapper.set(prop, value);
    }
  }

  public MetaObject metaObjectForProperty(String name) {
    Object value = getValue(name);
    return MetaObject.forObject(value, objectFactory, objectWrapperFactory, reflectorFactory);
  }

  public ObjectWrapper getObjectWrapper() {
    return objectWrapper;
  }

  public boolean isCollection() {
    return objectWrapper.isCollection();
  }

  public void add(Object element) {
    objectWrapper.add(element);
  }

  public <E> void addAll(List<E> list) {
    objectWrapper.addAll(list);
  }

}
