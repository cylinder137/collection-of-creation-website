package com.zaowuji.back.mapper;

import com.zaowuji.back.entity.OperationLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 操作日志表 Mapper
 */
@Mapper
public interface OperationLogMapper {

    int insert(OperationLog log);
}
