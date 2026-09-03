package com.zaowuji.back.service;

import com.zaowuji.back.common.BizException;
import com.zaowuji.back.entity.License;
import com.zaowuji.back.entity.User;
import com.zaowuji.back.mapper.LicenseMapper;
import com.zaowuji.back.mapper.UserMapper;
import com.zaowuji.back.vo.LicenseVO;
import com.zaowuji.back.vo.UserDetailVO;
import com.zaowuji.back.vo.UserVO;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 买家用户服务（管理后台）：用户列表 / 用户详情（聚合订单、激活码）
 * <p>微信认证登录已废除，用户以联系方式（contact）为唯一标识。
 */
@Service
public class UserService {

    private final UserMapper userMapper;
    private final LicenseMapper licenseMapper;
    private final OrderService orderService;
    private final ProductService productService;

    public UserService(UserMapper userMapper, LicenseMapper licenseMapper,
                       OrderService orderService, ProductService productService) {
        this.userMapper = userMapper;
        this.licenseMapper = licenseMapper;
        this.orderService = orderService;
        this.productService = productService;
    }

    /**
     * 用户列表（新 → 旧，含订单数/激活码数）
     */
    public List<UserVO> list() {
        return userMapper.selectAllWithStats();
    }

    /**
     * 用户详情：基本信息 + 名下订单 + 名下激活码
     */
    public UserDetailVO detail(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BizException(404, "用户不存在");
        }
        UserDetailVO vo = new UserDetailVO();
        vo.setUser(toVO(user));
        vo.setOrders(orderService.listByUser(id));
        vo.setLicenses(licenseMapper.selectByUser(id).stream().map(this::toLicenseVO).toList());
        return vo;
    }

    private UserVO toVO(User u) {
        UserVO vo = new UserVO();
        vo.setId(u.getId());
        vo.setContact(u.getContact());
        vo.setNickname(u.getNickname());
        vo.setEmail(u.getEmail());
        vo.setCreatedAt(u.getCreatedAt());
        vo.setUpdatedAt(u.getUpdatedAt());
        return vo;
    }

    private LicenseVO toLicenseVO(License l) {
        LicenseVO vo = new LicenseVO();
        vo.setId(l.getId());
        vo.setLicenseKey(l.getLicenseKey());
        vo.setSign(l.getSign());
        vo.setProductId(l.getProductId());
        vo.setProductName(productService.getById(l.getProductId()).getName());
        vo.setOrderId(l.getOrderId());
        vo.setLicenseType(l.getLicenseType());
        vo.setStatus(l.getStatus());
        vo.setIssuedAt(l.getIssuedAt());
        vo.setActivatedAt(l.getActivatedAt());
        vo.setCreatedAt(l.getCreatedAt());
        return vo;
    }
}
