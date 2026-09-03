package com.zaowuji.back.mapper;

import com.zaowuji.back.entity.License;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 激活码表 Mapper
 */
@Mapper
public interface LicenseMapper {

    License selectByLicenseKey(@Param("licenseKey") String licenseKey);

    License selectById(@Param("id") Long id);

    List<License> selectAll();

    /** 按用户查激活记录 */
    List<License> selectByUser(@Param("userId") Long userId);

    /** 按机器码哈希查激活记录 */
    List<License> selectByMachineCode(@Param("machineCode") String machineCode);

    /** 按产品统计激活码数量（删除产品前保护性检查） */
    int countByProductId(@Param("productId") Long productId);

    int insert(License license);

    /** 更新激活码状态（吊销/激活等；revokedAt 可为 null） */
    int updateStatus(@Param("id") Long id, @Param("status") Integer status,
                     @Param("activatedAt") java.time.LocalDateTime activatedAt,
                     @Param("revokedAt") java.time.LocalDateTime revokedAt);
}
