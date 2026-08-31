package com.zaowuji.back.controller;

import com.zaowuji.back.common.ApiResponse;
import com.zaowuji.back.dto.ActivateParams;
import com.zaowuji.back.service.ActivationService;
import com.zaowuji.back.vo.ActivationCodeVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 激活码接口
 */
@RestController
@RequestMapping("/api/activations")
public class ActivationController {

    private final ActivationService activationService;

    public ActivationController(ActivationService activationService) {
        this.activationService = activationService;
    }

    /** 提交机器码，申请签发激活码 */
    @PostMapping
    public ApiResponse<ActivationCodeVO> activate(@Valid @RequestBody ActivateParams params) {
        return ApiResponse.ok(activationService.activate(params.getProductId(), params.getMachineCode()));
    }

    /** 查询激活记录（按机器码） */
    @GetMapping
    public ApiResponse<List<ActivationCodeVO>> list(@RequestParam String machineCode) {
        return ApiResponse.ok(activationService.list(machineCode));
    }
}
