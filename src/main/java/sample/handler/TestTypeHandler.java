package sample.handler;

import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandler;
import sample.param.UserDo;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class TestTypeHandler implements TypeHandler<UserDo> {
  @Override
  public void setParameter(PreparedStatement ps, int i, UserDo parameter, JdbcType jdbcType) throws SQLException {

  }

  @Override
  public UserDo getResult(ResultSet rs, String columnName) throws SQLException {
    return null;
  }

  @Override
  public UserDo getResult(ResultSet rs, int columnIndex) throws SQLException {
    return null;
  }

  @Override
  public UserDo getResult(CallableStatement cs, int columnIndex) throws SQLException {
    return null;
  }
}
