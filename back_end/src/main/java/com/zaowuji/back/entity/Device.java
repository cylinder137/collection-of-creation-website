package com.zaowuji.back.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 机器码登记表（machine_code 只存 SHA-256 哈希）
 */
@Data
public class Device {
    private Long id;
    /** 机器码 SHA-256 哈希（hex，64 字符） */
    private String machineCode;
    private Long productId;
    private Long orderId;
    private Long userId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
