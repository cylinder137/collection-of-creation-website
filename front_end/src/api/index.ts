import { request } from './http'
import type {
  Product,
  ProductInput,
  Order,
  AdminLoginResult,
  AdminProfile,
  LicenseRecord,
  UserInfo,
  UserDetail,
} from '@/types'

/** 产品（官网公开接口：展示 + 下载） */
export const productApi = {
  /** 上架产品列表 */
  list() {
    return request<Product[]>({ url: '/products', method: 'get' })
  },
  /** 产品详情 */
  detail(id: number) {
    return request<Product>({ url: `/products/${id}`, method: 'get' })
  },
}

/**
 * 管理后台（RESTful 无状态）
 * 每次请求都携带 Authorization: Bearer <token>，后端逐次核验管理员身份与状态。
 */
export const adminApi = {
  /** 管理员登录 → 换取无状态令牌 */
  login(username: string, password: string) {
    return request<AdminLoginResult>({
      url: '/admin/login',
      method: 'post',
      data: { username, password },
    })
  },

  /** 当前管理员信息（顺带校验令牌是否仍然有效） */
  me() {
    return request<AdminProfile>({ url: '/admin/me', method: 'get' })
  },

  // ---------- 买家用户 ----------

  /** 用户列表（新 → 旧，含订单数/激活码数） */
  listUsers() {
    return request<UserInfo[]>({ url: '/admin/users', method: 'get' })
  },

  /** 用户详情（基本信息 + 名下订单 + 名下激活码） */
  userDetail(id: number) {
    return request<UserDetail>({ url: `/admin/users/${id}`, method: 'get' })
  },

  // ---------- 订单 ----------

  /** 订单列表（新 → 旧） */
  listOrders() {
    return request<Order[]>({ url: '/admin/orders', method: 'get' })
  },

  /** 人工核验通过：待支付(0) → 已支付(1) */
  reviewPass(orderNo: string) {
    return request<Order>({ url: `/admin/orders/${orderNo}/review-pass`, method: 'post' })
  },
  /** 拒收订单（账单不符；已签发激活码会一并吊销） */
  rejectOrder(orderNo: string) {
    return request<Order>({ url: `/admin/orders/${orderNo}/reject`, method: 'post' })
  },

  // ---------- 激活码 ----------

  /** 激活码签发记录（新 → 旧） */
  listLicenses() {
    return request<LicenseRecord[]>({ url: '/admin/licenses', method: 'get' })
  },

  /** 吊销激活码 */
  revokeLicense(id: number) {
    return request<{ id: number; status: number }>({
      url: `/admin/licenses/${id}/revoke`,
      method: 'post',
    })
  },

  // ---------- 产品 ----------

  /** 产品列表（含下架，管理用） */
  listProducts() {
    return request<Product[]>({ url: '/admin/products', method: 'get' })
  },

  /** 新建产品 */
  createProduct(data: ProductInput) {
    return request<Product>({ url: '/admin/products', method: 'post', data })
  },

  /** 更新产品（全量字段） */
  updateProduct(id: number, data: ProductInput) {
    return request<Product>({ url: `/admin/products/${id}`, method: 'put', data })
  },

  /** 上架 / 下架 */
  setProductStatus(id: number, status: number) {
    return request<Product>({
      url: `/admin/products/${id}/status`,
      method: 'patch',
      data: { status },
    })
  },

  /** 删除产品（无订单/激活码关联时后端才允许物理删除） */
  deleteProduct(id: number) {
    return request<null>({ url: `/admin/products/${id}`, method: 'delete' })
  },

  /**
   * 上传文件（封面图 cover / 安装包 package），返回 /uploads/... 相对 URL
   * （走 /api/admin/**，自动携带 Bearer 令牌与反爬签名头）
   */
  uploadFile(file: File, kind: 'cover' | 'qr' | 'package') {
    const fd = new FormData()
    fd.append('file', file)
    fd.append('kind', kind)
    return request<{ url: string }>({
      url: '/admin/upload',
      method: 'post',
      data: fd,
      // 显式清掉默认 JSON 头：FormData 由浏览器自动生成 multipart boundary
      headers: { 'Content-Type': undefined },
    })
  },
}
