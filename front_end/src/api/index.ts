import { request } from './http'
import type {
  Product,
  CreateOrderParams,
  Order,
  ActivationCode,
  ActivateParams,
  LoginParams,
  LoginResult,
  UserInfo,
} from '@/types'

/** 认证（后端契约见 front_end/README「接口约定」） */
export const authApi = {
  /** 登录：POST /auth/login → { token, userInfo } */
  login(data: LoginParams) {
    return request<LoginResult>({ url: '/auth/login', method: 'post', data })
  },
  /** 退出登录（使服务端 token 失效） */
  logout() {
    return request<void>({ url: '/auth/logout', method: 'post' })
  },
  /** 获取当前登录用户信息 */
  me() {
    return request<UserInfo>({ url: '/auth/me', method: 'get' })
  },
}

/** 产品 */
export const productApi = {
  /** 获取全部产品 */
  list() {
    return request<Product[]>({ url: '/products', method: 'get' })
  },
  /** 获取产品详情 */
  detail(id: number) {
    return request<Product>({ url: `/products/${id}`, method: 'get' })
  },
}

/** 订单 */
export const orderApi = {
  /** 创建订单（微信支付） */
  create(data: CreateOrderParams) {
    return request<Order>({ url: '/orders', method: 'post', data })
  },
  /** 查询订单 */
  detail(orderNo: string) {
    return request<Order>({ url: `/orders/${orderNo}`, method: 'get' })
  },
}

/** 激活码 */
export const activationApi = {
  /** 提交机器码，申请签发激活码 */
  activate(data: ActivateParams) {
    return request<ActivationCode>({ url: '/activations', method: 'post', data })
  },
  /** 查询激活记录 */
  list() {
    return request<ActivationCode[]>({ url: '/activations', method: 'get' })
  },
}
