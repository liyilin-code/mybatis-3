package sample.factory;

import org.apache.ibatis.reflection.factory.ObjectFactory;

import java.util.List;

public class TestObjectFactory implements ObjectFactory {
  @Override
  public <T> T create(Class<T> type) {
    return null;
  }

  @Override
  public <T> T create(Class<T> type, List<Class<?>> constructorArgTypes, List<Object> constructorArgs) {
    return null;
  }

  @Override
  public <T> boolean isCollection(Class<T> type) {
    return false;
  }
}
