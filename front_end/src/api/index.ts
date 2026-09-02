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
  /** 获取全部上架产品 */
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
  /** 创建订单（人工核验支付：扫码转账后管理员后台确认） */
  create(data: CreateOrderParams) {
    return request<Order>({ url: '/orders', method: 'post', data })
  },
  /** 查询订单 */
  detail(orderNo: string) {
    return request<Order>({ url: `/orders/${orderNo}`, method: 'get' })
  },
  /** 订单列表（管理后台） */
  list() {
    return request<Order[]>({ url: '/orders', method: 'get' })
  },
  /** 人工核验通过：待支付 → 已支付（管理后台一键通过） */
  reviewPass(orderNo: string) {
    return request<Order>({ url: `/orders/${orderNo}/review-pass`, method: 'post' })
  },
}

/** 激活码 */
export const activationApi = {
  /** 提交机器码，申请签发激活码 */
  activate(data: ActivateParams) {
    return request<ActivationCode>({ url: '/activations', method: 'post', data })
  },
  /** 查询激活记录（按机器码） */
  list(machineCode: string) {
    return request<ActivationCode[]>({
      url: '/activations',
      method: 'get',
      params: { machineCode },
    })
  },
}
