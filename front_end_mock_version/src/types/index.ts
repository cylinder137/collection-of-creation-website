/** 产品 */
export interface Product {
  id: number
  name: string
  /** 一句话简介 */
  slogan: string
  description: string
  /** 封面图 URL */
  cover?: string
  /** 价格（元） */
  price: number
  /** 支持平台 */
  platforms?: string[]
  tags?: string[]
}

/** 创建订单入参 */
export interface CreateOrderParams {
  productId: number
  /** 下单人联系方式（手机/邮箱） */
  contact: string
  remark?: string
}

/** 订单 */
export interface Order {
  id: number
  orderNo: string
  productId: number
  productName: string
  amount: number
  status: 'PENDING' | 'PAID' | 'CANCELLED'
  createdAt: string
}

/** 激活码 */
export interface ActivationCode {
  id: number
  code: string
  productId: number
  productName: string
  /** 绑定的机器码 */
  machineCode: string
  status: 'UNUSED' | 'ACTIVATED' | 'DISABLED'
  createdAt: string
}

/** 提交机器码换取激活码入参 */
export interface ActivateParams {
  productId: number
  machineCode: string
}
