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
package org.apache.ibatis.builder.xml;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import org.apache.ibatis.builder.BuilderException;
import org.apache.ibatis.builder.IncompleteElementException;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.parsing.PropertyParser;
import org.apache.ibatis.parsing.XNode;
import org.apache.ibatis.session.Configuration;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author Frank D. Martinez [mnesarco]
 */
public class XMLIncludeTransformer {

  private final Configuration configuration;
  private final MapperBuilderAssistant builderAssistant;

  public XMLIncludeTransformer(Configuration configuration, MapperBuilderAssistant builderAssistant) {
    this.configuration = configuration;
    this.builderAssistant = builderAssistant;
  }

  public void applyIncludes(Node source) {
    Properties variablesContext = new Properties();
    // 读取全局属性信息
    Properties configurationVariables = configuration.getVariables();
    // 这边可以用config配置文件中配置的<property>属性
    Optional.ofNullable(configurationVariables).ifPresent(variablesContext::putAll);
    applyIncludes(source, variablesContext, false);
  }

  /**
   * 递归处理Node节点中<include>标签
   * 1. 找到目标sql片段节点
   *  <sql id="byId">
   *      AND schoolId = #{schoolId}
   *  </sql>
   *
   * 2. 替换掉<include>节点
   * <select id="selectByMyId" resultMap="userMap">
   *     SELECT * FROM USER
   *     <sql id="byId">
   *         AND schoolId = #{schoolId}
   *     </sql>
   * </select>
   *
   * 3. sql节点中内容拉出来
   * <select id="selectByMyId" resultMap="userMap">
   *     SELECT * FROM USER
   *     AND schoolId = #{schoolId}
   * </select>
   *
   * Recursively apply includes through all SQL fragments.
   *
   * @param source
   *          Include node in DOM tree
   * @param variablesContext
   *          Current context for static variables with values
   */
  private void applyIncludes(Node source, final Properties variablesContext, boolean included) {
    if ("include".equals(source.getNodeName())) {
      // 1. 获取include引用的sql片段节点
      // <sql id="byId">
      //    AND schoolId = #{schoolId}
      // </sql>
      Node toInclude = findSqlFragment(getStringAttribute(source, "refid"), variablesContext);
      // include节点也可以定义属性，这边是解析出属性，通过全局属性消除占位符，同时合并为完整的属性列表
      Properties toIncludeContext = getVariablesContext(source, variablesContext);
      // 递归处理<sql>元素节点中包含的子<include>节点
      applyIncludes(toInclude, toIncludeContext, true);
      if (toInclude.getOwnerDocument() != source.getOwnerDocument()) {
        toInclude = source.getOwnerDocument().importNode(toInclude, true);
      }
      // 2. <include>节点的替换
      // 原Node节点
      // <select id="selectByMyId" resultMap="userMap">
      //    SELECT * FROM USER
      //    <include refid="byId"/>
      // </select>
      // 替换后Node节点
      // <select id="selectByMyId" resultMap="userMap">
      //    SELECT * FROM USER
      //    <sql id="byId">
      //        AND schoolId = #{schoolId}
      //    </sql>
      // </select>
      source.getParentNode().replaceChild(toInclude, source);
      // 3. <sql>节点内容添加到前面，删除掉<sql>节点
      // 新Node如下
      // <select id="selectByMyId" resultMap="userMap">
      //    SELECT * FROM USER
      //    AND schoolId = #{schoolId}
      // </select>
      while (toInclude.hasChildNodes()) {
        toInclude.getParentNode().insertBefore(toInclude.getFirstChild(), toInclude);
      }
      toInclude.getParentNode().removeChild(toInclude);
    } else if (source.getNodeType() == Node.ELEMENT_NODE) {
      // 元素节点
      if (included && !variablesContext.isEmpty()) {
        // replace variables in attribute values
        NamedNodeMap attributes = source.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
          Node attr = attributes.item(i);
          attr.setNodeValue(PropertyParser.parse(attr.getNodeValue(), variablesContext));
        }
      }
      NodeList children = source.getChildNodes();
      for (int i = 0; i < children.getLength(); i++) {
        applyIncludes(children.item(i), variablesContext, included);
      }
    } else if (included && (source.getNodeType() == Node.TEXT_NODE || source.getNodeType() == Node.CDATA_SECTION_NODE)
        && !variablesContext.isEmpty()) {
      // replace variables in text node
      // 属性值替换变量
      // 比如
      // SELECT * FROM ${tableName}
      // 全局定义了属性tableName,就会替换掉
      source.setNodeValue(PropertyParser.parse(source.getNodeValue(), variablesContext));
    }
  }

  private Node findSqlFragment(String refid, Properties variables) {
    refid = PropertyParser.parse(refid, variables);
    refid = builderAssistant.applyCurrentNamespace(refid, true);
    try {
      XNode nodeToInclude = configuration.getSqlFragments().get(refid);
      return nodeToInclude.getNode().cloneNode(true);
    } catch (IllegalArgumentException e) {
      throw new IncompleteElementException("Could not find SQL statement to include with refid '" + refid + "'", e);
    }
  }

  private String getStringAttribute(Node node, String name) {
    return node.getAttributes().getNamedItem(name).getNodeValue();
  }

  /**
   * Read placeholders and their values from include node definition.
   *
   * @param node
   *          Include node instance
   * @param inheritedVariablesContext
   *          Current context used for replace variables in new variables values
   *
   * @return variables context from include instance (no inherited values)
   */
  private Properties getVariablesContext(Node node, Properties inheritedVariablesContext) {
    Map<String, String> declaredProperties = null;
    NodeList children = node.getChildNodes();
    for (int i = 0; i < children.getLength(); i++) {
      Node n = children.item(i);
      if (n.getNodeType() == Node.ELEMENT_NODE) {
        String name = getStringAttribute(n, "name");
        // Replace variables inside
        String value = PropertyParser.parse(getStringAttribute(n, "value"), inheritedVariablesContext);
        if (declaredProperties == null) {
          declaredProperties = new HashMap<>();
        }
        if (declaredProperties.put(name, value) != null) {
          throw new BuilderException("Variable " + name + " defined twice in the same include definition");
        }
      }
    }
    if (declaredProperties == null) {
      return inheritedVariablesContext;
    }
    Properties newProperties = new Properties();
    newProperties.putAll(inheritedVariablesContext);
    newProperties.putAll(declaredProperties);
    return newProperties;
  }
}
