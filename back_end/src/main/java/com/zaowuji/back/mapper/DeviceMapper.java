package com.zaowuji.back.mapper;

import com.zaowuji.back.entity.Device;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 机器码登记表 Mapper
 */
@Mapper
public interface DeviceMapper {

    /** 按机器码哈希查询（唯一索引） */
    Device selectByMachineCode(@Param("machineCode") String machineCode);

    int insert(Device device);
}
