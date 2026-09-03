package com.zaowuji.back.mapper;

import com.zaowuji.back.entity.AdminUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 后台管理员表 Mapper
 */
@Mapper
public interface AdminUserMapper {

    AdminUser selectByUsername(@Param("username") String username);

    AdminUser selectById(@Param("id") Long id);
}
