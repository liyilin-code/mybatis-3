package sample.provider;

import org.apache.ibatis.mapping.DatabaseIdProvider;

import javax.sql.DataSource;
import java.sql.SQLException;

public class TestDatabaseIdProvider implements DatabaseIdProvider {
  @Override
  public String getDatabaseId(DataSource dataSource) throws SQLException {
    return null;
  }
}
