package sample.factory;

import org.apache.ibatis.reflection.Reflector;
import org.apache.ibatis.reflection.ReflectorFactory;

public class TestReflectFactory implements ReflectorFactory {
  @Override
  public boolean isClassCacheEnabled() {
    return false;
  }

  @Override
  public void setClassCacheEnabled(boolean classCacheEnabled) {

  }

  @Override
  public Reflector findForClass(Class<?> type) {
    return null;
  }
}
