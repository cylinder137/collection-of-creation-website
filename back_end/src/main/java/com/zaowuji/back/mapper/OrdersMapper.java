package com.zaowuji.back.mapper;

import com.zaowuji.back.entity.Orders;
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

    int insert(Orders orders);

    int updateStatus(@Param("id") Long id, @Param("status") Integer status,
                     @Param("paidAt") java.time.LocalDateTime paidAt);
}
