/**
 * 前后端类型定义（以 back_end 接口为准）
 *
 * 业务模式说明（2026-09 调整）：
 * 官网不发售激活码，仅提供 exe 自解压安装包下载；
 * 激活由安装程序 / 产品客户端在本机完成（可提权读取机器码 → 向后端申请激活）。
 */

/** 产品（对齐后端 ProductVO） */
export interface Product {
  id: number
  name: string
  /** 产品编码，客户端对接用（如 coBrain） */
  code: string
  description: string
  /** 当前版本号 */
  version: string | null
  /** 封面图 URL */
  coverUrl: string | null
  /** 安装包下载地址（exe 自解压安装包） */
  downloadUrl: string | null
  /** 价格（元） */
  price: number
  /** 1上架 0下架 */
  status: number
  /** 排序权重，越小越靠前 */
  sort: number
}

/** 管理后台产品编辑入参 */
export interface ProductInput {
  name: string
  code: string
  description?: string
  version?: string
  coverUrl?: string
  downloadUrl?: string
  /** 价格（元） */
  price: number
  status: number
  sort?: number
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
  paidAt: string | null
  createdAt: string
}

/** 激活码签发记录（对齐后端 LicenseVO） */
export interface LicenseRecord {
  id: number
  /** license_key：机器码哈希-产品ID */
  licenseKey: string
  /** RSA 签名（base64url） */
  sign: string
  productId: number
  productName: string
  orderId: number | null
  /** 0永久 1试用 */
  licenseType: number
  /** 0未激活 1已激活 2已吊销 3已过期 */
  status: number
  issuedAt: string | null
  activatedAt: string | null
  createdAt: string
}

/** 管理员登录返回 */
export interface AdminLoginResult {
  /** 无状态令牌：后续请求放 Authorization: Bearer <token> */
  token: string
  nickname: string
  /** 1超级管理员 2普通管理员 */
  role: number
}

/** 当前管理员信息 */
export interface AdminProfile {
  id: number
  username: string
  nickname: string
  role: number
}

/** 统一响应体（http.ts 会自动解包，此处仅留档） */
export interface ApiEnvelope<T> {
  code: number
  data: T
  message: string
}

/** 订单状态字典（展示用） */
export const ORDER_STATUS: Record<
  number,
  { label: string; type: 'info' | 'success' | 'warning' | 'danger' | 'primary' }
> = {
  0: { label: '待人工核验', type: 'warning' },
  1: { label: '已支付', type: 'success' },
  2: { label: '已取消', type: 'info' },
  3: { label: '已退款', type: 'danger' },
  4: { label: '已签发', type: 'primary' },
}

/** 激活码状态字典（展示用） */
export const LICENSE_STATUS: Record<
  number,
  { label: string; type: 'info' | 'success' | 'warning' | 'danger' }
> = {
  0: { label: '已签发 / 待激活', type: 'info' },
  1: { label: '已激活', type: 'success' },
  2: { label: '已吊销', type: 'danger' },
  3: { label: '已过期', type: 'warning' },
}
