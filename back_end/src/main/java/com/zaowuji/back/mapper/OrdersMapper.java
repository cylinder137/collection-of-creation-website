package com.zaowuji.back.mapper;

import com.zaowuji.back.entity.Orders;
import com.zaowuji.back.vo.OrderVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 订单 Mapper
 */
@Mapper
public interface OrdersMapper {

    Orders selectByOrderNo(@Param("orderNo") String orderNo);

    Orders selectById(@Param("id") Long id);

    List<Orders> selectAll();

    /** 某用户下的全部订单（新 → 旧） */
    List<Orders> selectByUserId(@Param("userId") Long userId);

    /** 管理端订单列表：LEFT JOIN user 带出下单人联系方式（新 → 旧，金额已换算为元） */
    List<OrderVO> selectAllWithUser();

    int insert(Orders orders);

    int updateStatus(@Param("id") Long id, @Param("status") Integer status,
                     @Param("paidAt") java.time.LocalDateTime paidAt);
}
