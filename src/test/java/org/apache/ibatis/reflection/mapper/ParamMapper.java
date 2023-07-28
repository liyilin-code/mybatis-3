package org.apache.ibatis.reflection.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

public interface ParamMapper {
  void noParamAnnotation(Long id, String name);
  void paramAnnotation(@Param("custId") Long id, String name);
  void singleWithoutParamAnnotation(Long id);
  void singleWithParamAnnotation(@Param("custId") Long id);
  void singleListWithoutParamAnnotation(List<String> names);
  void singleListWithParamAnnotation(@Param("custNames") List<String> names);
}
