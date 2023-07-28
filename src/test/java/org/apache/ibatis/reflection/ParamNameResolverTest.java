package org.apache.ibatis.reflection;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.reflection.mapper.ParamMapper;
import org.apache.ibatis.session.Configuration;
import org.junit.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ParamNameResolverTest {
  /**
   * 方法参数上没有标注 @Param 注解
   * @throws NoSuchMethodException
   */
  @Test
  public void testGetNames_useActualParamNameAndNoParamAnnotation() throws NoSuchMethodException {
    Configuration config = new Configuration();
    config.setUseActualParamName(true);
    Method method = ParamMapper.class.getMethod("noParamAnnotation", Long.class, String.class);
    ParamNameResolver resolver = new ParamNameResolver(config, method);
    // 返回参数名称
    String[] names = resolver.getNames();
    assertEquals("id", names[0]);
    assertEquals("name", names[1]);
  }

  @Test
  public void testGetNames_doNotUseActualParamNameAndNoParamAnnotation() throws NoSuchMethodException {
    Configuration config = new Configuration();
    config.setUseActualParamName(false);
    Method method = ParamMapper.class.getMethod("noParamAnnotation", Long.class, String.class);
    ParamNameResolver resolver = new ParamNameResolver(config, method);
    // 返回参数名称
    String[] names = resolver.getNames();
    // 因为 useActualParamName=false
    // 属性名称为0, 1, 2...
    assertEquals("0", names[0]);
    assertEquals("1", names[1]);
  }

  @Test
  public void testGetNames_withParamAnnotation() throws NoSuchMethodException {
    Configuration config = new Configuration();
    // 当存在 @Param 注解，useActualParamName 不影响有注解的属性名称
    config.setUseActualParamName(false);
    Method method = ParamMapper.class.getMethod("paramAnnotation", Long.class, String.class);
    ParamNameResolver resolver = new ParamNameResolver(config, method);
    // 返回参数名称
    String[] names = resolver.getNames();
    // 因为 useActualParamName=false
    // 属性名称为0, 1, 2...
    assertEquals("custId", names[0]);
    assertEquals("1", names[1]);
  }

  /**
   * 普通数据类型，@Param("custId") Long id, String name
   * @throws NoSuchMethodException
   */
  @Test
  public void testGetNamedParams_simple() throws NoSuchMethodException {
    Configuration config = new Configuration();
    // 当存在 @Param 注解，useActualParamName 不影响有注解的属性名称
    config.setUseActualParamName(false);
    Method method = ParamMapper.class.getMethod("paramAnnotation", Long.class, String.class);
    ParamNameResolver resolver = new ParamNameResolver(config, method);
    Map<String, Object> map = (Map<String, Object>) resolver.getNamedParams(new Object[] {18L, "myName"});
    assertEquals(18L, map.get("custId"));
    assertEquals(18L, map.get("param1"));
    assertEquals("myName", map.get("1"));
    assertEquals("myName", map.get("param2"));
  }

  /**
   * 单参数(非列表List类型)，没有 @Param 注解，Long id
   *     单个参数(非列表List类型)且没有@Param注解，直接返回参数值，而不会包装为Map
   * @throws NoSuchMethodException
   */
  @Test
  public void testGetNamedParams_singleWithoutParamAnnotation() throws NoSuchMethodException {
    Configuration config = new Configuration();
    // 当存在 @Param 注解，useActualParamName 不影响有注解的属性名称
    config.setUseActualParamName(true);
    Method method = ParamMapper.class.getMethod("singleWithoutParamAnnotation", Long.class);
    ParamNameResolver resolver = new ParamNameResolver(config, method);
    Object value = resolver.getNamedParams(new Object[] {18L});
    assertEquals(18L, value);
  }

  /**
   * 单参数(非列表List类型)，有 @Param 注解，@Param("custId") Long id
   *     单个参数(非列表List类型) 有@Param注解，会包装成Map
   *     {{"custId", 18}, {"param1", 18}}
   * @throws NoSuchMethodException
   */
  @Test
  public void testGetNamedParams_singleWithParamAnnotation() throws NoSuchMethodException {
    Configuration config = new Configuration();
    // 当存在 @Param 注解，useActualParamName 不影响有注解的属性名称
    config.setUseActualParamName(true);
    Method method = ParamMapper.class.getMethod("singleWithParamAnnotation", Long.class);
    ParamNameResolver resolver = new ParamNameResolver(config, method);
    Map<String, Object> map = (Map<String, Object>) resolver.getNamedParams(new Object[] {18L});
    assertEquals(18L, map.get("custId"));
    assertEquals(18L, map.get("param1"));
  }

  /**
   * 单参数(列表List类型)，没有 @Param 注解，List<String> names
   *     单个参数(列表List类型) 没有@Param注解，会包装成Map
   *     {{"custId", 18}, {"param1", 18}}
   * @throws NoSuchMethodException
   */
  @Test
  public void testGetNamedParams_singleListWithoutParamAnnotation() throws NoSuchMethodException {
    Configuration config = new Configuration();
    // 当存在 @Param 注解，useActualParamName 不影响有注解的属性名称
    config.setUseActualParamName(true);
    Method method = ParamMapper.class.getMethod("singleListWithoutParamAnnotation", List.class);
    ParamNameResolver resolver = new ParamNameResolver(config, method);
    List<String> names = new ArrayList<>(Arrays.asList("name1", "name2"));
    Map<String, Object> map = (Map<String, Object>) resolver.getNamedParams(new Object[] {names});
    assertEquals(names, map.get("names"));
    // 针对List添加的两个名称collection和list
    assertEquals(names, map.get("collection"));
    assertEquals(names, map.get("list"));
  }

  /**
   * 单参数(列表List类型)，有 @Param 注解，@Paam("custNames") List<String> names
   *     单个参数(列表List类型) 有@Param注解，有注解都会包装成Map
   *     {{"custNames", names}, {"param1", names}}
   * @throws NoSuchMethodException
   */
  @Test
  public void testGetNamedParams_singleListWithParamAnnotation() throws NoSuchMethodException {
    Configuration config = new Configuration();
    // 当存在 @Param 注解，useActualParamName 不影响有注解的属性名称
    config.setUseActualParamName(true);
    Method method = ParamMapper.class.getMethod("singleListWithParamAnnotation", List.class);
    ParamNameResolver resolver = new ParamNameResolver(config, method);
    List<String> names = new ArrayList<>(Arrays.asList("name1", "name2"));
    Map<String, Object> map = (Map<String, Object>) resolver.getNamedParams(new Object[] {names});
    assertEquals(names, map.get("custNames"));
    assertEquals(names, map.get("param1"));
  }
}
