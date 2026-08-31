/**
 * 前后端类型定义（对齐 back_end/后端接口文档.md v0.1）
 * 字段以后端返回为准
 */

/** 产品（对齐后端 ProductVO） */
export interface Product {
  id: number
  name: string
  /** 产品编码 */
  code: string
  description: string
  /** 当前版本号 */
  version: string | null
  /** 封面图 URL */
  coverUrl: string | null
  /** 价格（元，后端已从分转换） */
  price: number
  /** 1上架 0下架 */
  status: number
  /** 排序权重 */
  sort: number
}

/** 创建订单入参 */
export interface CreateOrderParams {
  productId: number
  /** 下单人联系方式（手机/邮箱） */
  contact: string
  remark?: string
}

/** 订单（对齐后端 OrderVO） */
export interface Order {
  id: number
  orderNo: string
  productId: number
  productName: string
  /** 金额（元） */
  amount: number
  /** 0待支付 1已支付 2已取消 3已退款 4已签发 */
  status: number
  createdAt: string
}

/** 激活码（对齐后端 ActivationCodeVO） */
export interface ActivationCode {
  id: number
  /** 激活码内容 */
  code: string
  productId: number
  productName: string
  /** 绑定的机器码 */
  machineCode: string
  /** 0未激活 1已激活 2已吊销 3已过期 */
  status: number
  createdAt: string
}

/** 提交机器码换取激活码入参 */
export interface ActivateParams {
  productId: number
  machineCode: string
  /** 关联订单号（可选）：下单后激活时携带，后端校验订单并绑定 */
  orderNo?: string
}
