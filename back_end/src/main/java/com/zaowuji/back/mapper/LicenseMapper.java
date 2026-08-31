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

    /** 按用户查激活记录 */
    List<License> selectByUser(@Param("userId") Long userId);

    /** 按机器码哈希查激活记录 */
    List<License> selectByMachineCode(@Param("machineCode") String machineCode);

    int insert(License license);
}
