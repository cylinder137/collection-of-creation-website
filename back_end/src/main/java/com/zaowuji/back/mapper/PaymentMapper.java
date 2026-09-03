package com.zaowuji.back.mapper;

import com.zaowuji.back.entity.Payment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 支付流水表 Mapper
 */
@Mapper
public interface PaymentMapper {

    /** 按订单号查询（唯一，防微信重复回调） */
    Payment selectByOrderNo(@Param("orderNo") String orderNo);

    int insert(Payment payment);
}
