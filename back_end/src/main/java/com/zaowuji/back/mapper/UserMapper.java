package com.zaowuji.back.mapper;

import com.zaowuji.back.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 买家用户表 Mapper
 */
@Mapper
public interface UserMapper {

    User selectByOpenid(@Param("openid") String openid);

    User selectById(@Param("id") Long id);

    int insert(User user);
}
