package com.zaowuji.back.mapper;

import com.zaowuji.back.entity.Orders;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 订单表 Mapper
 */
@Mapper
public interface OrdersMapper {

    Orders selectByOrderNo(@Param("orderNo") String orderNo);

    Orders selectById(@Param("id") Long id);

    int insert(Orders orders);
}
