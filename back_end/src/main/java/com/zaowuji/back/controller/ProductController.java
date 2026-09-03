package com.zaowuji.back.controller;

import com.zaowuji.back.common.ApiResponse;
import com.zaowuji.back.service.ProductService;
import com.zaowuji.back.vo.ProductVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 产品接口（对外公开）
 */
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    /** 获取全部上架产品 */
    @GetMapping
    public ApiResponse<List<ProductVO>> list() {
        return ApiResponse.ok(productService.listOnSale());
    }

    /** 获取产品详情 */
    @GetMapping("/{id}")
    public ApiResponse<ProductVO> detail(@PathVariable Long id) {
        return ApiResponse.ok(productService.detail(id));
    }
}
