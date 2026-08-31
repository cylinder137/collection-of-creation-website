package com.zaowuji.back.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作日志表
 */
@Data
public class OperationLog {
    private Long id;
    private Long adminId;
    private String action;
    private String detail;
    /** 来源 IP（兼容 IPv6） */
    private String ip;
    private LocalDateTime createdAt;
}
