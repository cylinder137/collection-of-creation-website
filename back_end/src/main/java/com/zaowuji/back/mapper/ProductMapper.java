package com.zaowuji.back.mapper;

import com.zaowuji.back.entity.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 产品表 Mapper
 */
@Mapper
public interface ProductMapper {

    /** 查询所有上架产品（按 sort 升序） */
    List<Product> selectOnSale();

    /** 按 id 查询产品（不限上下架） */
    Product selectById(@Param("id") Long id);

    /** 按编码查询产品 */
    Product selectByCode(@Param("code") String code);
}
