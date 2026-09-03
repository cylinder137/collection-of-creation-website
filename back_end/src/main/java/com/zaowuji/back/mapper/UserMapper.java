package com.zaowuji.back.mapper;

import com.zaowuji.back.entity.User;
import com.zaowuji.back.vo.UserVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 买家用户表 Mapper
 */
@Mapper
public interface UserMapper {

    /** 按联系方式（手机/邮箱）查询 */
    User selectByContact(@Param("contact") String contact);

    User selectById(@Param("id") Long id);

    /** 用户列表（新 → 旧，含订单数/激活码数统计），管理后台用 */
    List<UserVO> selectAllWithStats();

    int insert(User user);
}
