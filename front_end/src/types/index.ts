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

/** 登录入参 */
export interface LoginParams {
  username: string
  password: string
  /** 记住我（由后端控制 token 时效） */
  remember?: boolean
}

/** 用户信息 */
export interface UserInfo {
  id: number
  username: string
  nickname?: string
  /** 角色：admin 可进管理后台 */
  role?: 'admin' | 'user'
}

/** 登录返回（后端契约：POST /auth/login → data 为此结构） */
export interface LoginResult {
  token: string
  userInfo: UserInfo
}
