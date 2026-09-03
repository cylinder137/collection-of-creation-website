package com.zaowuji.back.service;

import com.zaowuji.back.common.BizException;
import com.zaowuji.back.entity.Product;
import com.zaowuji.back.mapper.LicenseMapper;
import com.zaowuji.back.mapper.OrdersMapper;
import com.zaowuji.back.mapper.ProductMapper;
import com.zaowuji.back.vo.ProductVO;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * 产品服务：官网侧列表/详情（带缓存）+ 管理后台 CRUD（写后失效缓存）
 */
@Service
public class ProductService {

    private final ProductMapper productMapper;
    private final LicenseMapper licenseMapper;
    private final OrdersMapper ordersMapper;

    public ProductService(ProductMapper productMapper, LicenseMapper licenseMapper, OrdersMapper ordersMapper) {
        this.productMapper = productMapper;
        this.licenseMapper = licenseMapper;
        this.ordersMapper = ordersMapper;
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

    // ==================== 管理后台 ====================

    /** 全部产品（含下架） */
    public List<ProductVO> listAll() {
        return productMapper.selectAll().stream()
                .map(this::toVO)
                .toList();
    }

    /** 新建产品（写后清缓存，官网立即生效） */
    @CacheEvict(cacheNames = "products", allEntries = true)
    public ProductVO create(Product input) {
        validate(input);
        Product dup = productMapper.selectByCode(input.getCode());
        if (dup != null) {
            throw new BizException("产品编码已存在：" + input.getCode());
        }
        if (input.getSort() == null) {
            input.setSort(0);
        }
        productMapper.insert(input);
        return toVO(productMapper.selectById(input.getId()));
    }

    /** 更新产品（全量字段） */
    @CacheEvict(cacheNames = "products", allEntries = true)
    public ProductVO update(Long id, Product input) {
        Product exist = productMapper.selectById(id);
        if (exist == null) {
            throw new BizException(404, "产品不存在");
        }
        validate(input);
        Product dup = productMapper.selectByCode(input.getCode());
        if (dup != null && !dup.getId().equals(id)) {
            throw new BizException("产品编码已被其他产品占用：" + input.getCode());
        }
        if (input.getSort() == null) {
            input.setSort(exist.getSort());
        }
        input.setId(id);
        productMapper.updateById(input);
        return toVO(productMapper.selectById(id));
    }

    /** 上架 / 下架 */
    @CacheEvict(cacheNames = "products", allEntries = true)
    public ProductVO updateStatus(Long id, Integer status) {
        Product exist = productMapper.selectById(id);
        if (exist == null) {
            throw new BizException(404, "产品不存在");
        }
        if (status == null || (status != 0 && status != 1)) {
            throw new BizException("status 仅支持 0(下架) / 1(上架)");
        }
        productMapper.updateStatus(id, status);
        return toVO(productMapper.selectById(id));
    }

    /**
     * 删除产品（物理删除）。
     * 保护：若该产品已存在订单/激活码记录则拒绝删除（防止历史记录悬空），建议改为下架。
     */
    @CacheEvict(cacheNames = "products", allEntries = true)
    public void delete(Long id) {
        Product exist = productMapper.selectById(id);
        if (exist == null) {
            throw new BizException(404, "产品不存在");
        }
        long licenseCount = licenseMapper.countByProductId(id);
        if (licenseCount > 0) {
            throw new BizException("该产品已有 " + licenseCount + " 条激活码记录，禁止删除（可改为下架）");
        }
        long orderCount = ordersMapper.countByProductId(id);
        if (orderCount > 0) {
            throw new BizException("该产品已有 " + orderCount + " 条订单记录，禁止删除（可改为下架）");
        }
        productMapper.deleteById(id);
    }

    private void validate(Product p) {
        if (p.getName() == null || p.getName().isBlank()) {
            throw new BizException("产品名称不能为空");
        }
        if (p.getCode() == null || !p.getCode().matches("[A-Za-z0-9_-]{1,64}")) {
            throw new BizException("产品编码仅允许字母/数字/下划线/中划线，长度 1-64");
        }
        if (p.getPrice() == null || p.getPrice() < 0) {
            throw new BizException("价格不能为负数");
        }
        if (p.getStatus() == null || (p.getStatus() != 0 && p.getStatus() != 1)) {
            throw new BizException("status 仅支持 0(下架) / 1(上架)");
        }
    }

    private ProductVO toVO(Product p) {
        ProductVO vo = new ProductVO();
        vo.setId(p.getId());
        vo.setName(p.getName());
        vo.setCode(p.getCode());
        vo.setDescription(p.getDescription());
        vo.setVersion(p.getVersion());
        vo.setCoverUrl(p.getCoverUrl());
        vo.setPayQrUrl(p.getPayQrUrl());
        vo.setDownloadUrl(p.getDownloadUrl());
        // 分 -> 元
        vo.setPrice(BigDecimal.valueOf(p.getPrice(), 2));
        vo.setStatus(p.getStatus());
        vo.setSort(p.getSort());
        return vo;
    }
}
