import { request } from './http'
import type {
  Product,
  CreateOrderParams,
  Order,
  ActivationCode,
  ActivateParams,
} from '@/types'

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
