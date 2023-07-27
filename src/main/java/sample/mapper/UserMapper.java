package sample.mapper;

import sample.param.UserDo;

public interface UserMapper {
  UserDo selectById(Long id);
}
