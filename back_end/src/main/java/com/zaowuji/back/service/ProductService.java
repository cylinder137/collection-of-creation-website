package com.zaowuji.back.service;

import com.zaowuji.back.common.BizException;
import com.zaowuji.back.entity.Product;
import com.zaowuji.back.mapper.ProductMapper;
import com.zaowuji.back.vo.ProductVO;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * 产品服务：列表/详情（带本地缓存，重复请求直接命中缓存，不查库）
 */
@Service
public class ProductService {

    private final ProductMapper productMapper;

    public ProductService(ProductMapper productMapper) {
        this.productMapper = productMapper;
    }

    /**
     * 上架产品列表（缓存 5 分钟）
     */
    @Cacheable(cacheNames = "products", key = "'list'")
    public List<ProductVO> listOnSale() {
        return productMapper.selectOnSale().stream()
                .map(this::toVO)
                .toList();
    }

    /**
     * 产品详情（缓存 5 分钟，按 id）
     */
    @Cacheable(cacheNames = "products", key = "'detail:' + #id")
    public ProductVO detail(Long id) {
        Product p = productMapper.selectById(id);
        if (p == null) {
            throw new BizException(404, "产品不存在");
        }
        return toVO(p);
    }

    /**
     * 内部使用：查实体（下单时校验产品有效性，不缓存）
     */
    public Product getById(Long id) {
        Product p = productMapper.selectById(id);
        if (p == null) {
            throw new BizException(404, "产品不存在");
        }
        return p;
    }

    private ProductVO toVO(Product p) {
        ProductVO vo = new ProductVO();
        vo.setId(p.getId());
        vo.setName(p.getName());
        vo.setCode(p.getCode());
        vo.setDescription(p.getDescription());
        vo.setVersion(p.getVersion());
        vo.setCoverUrl(p.getCoverUrl());
        // 分 -> 元
        vo.setPrice(BigDecimal.valueOf(p.getPrice(), 2));
        vo.setStatus(p.getStatus());
        vo.setSort(p.getSort());
        return vo;
    }
}
