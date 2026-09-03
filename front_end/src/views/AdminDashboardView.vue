<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Refresh, Plus, Checked, Download, Warning, User } from '@element-plus/icons-vue'
import { adminApi } from '@/api'
import type { LicenseRecord, Order, Product, ProductInput, UserDetail, UserInfo } from '@/types'
import { LICENSE_STATUS, ORDER_STATUS } from '@/types'

/**
 * 管理后台主界面
 *
 * 四个工作区：
 * - 订单核验：人工确认收款（待支付 → 已支付），驱动激活码可签发（列表带下单人联系方式）
 * - 用户：买家用户列表（联系方式建档）+ 详情（名下订单 / 激活码）
 * - 激活码：全部签发记录 + 吊销
 * - 产品：增改 / 上下架，维护官网下载入口
 *
 * 所有接口走 /api/admin/**，每次请求自动携带令牌，后端逐次核验身份。
 */

const activeTab = ref<'orders' | 'users' | 'licenses' | 'products'>('orders')
const loading = ref(false)

const orders = ref<Order[]>([])
const users = ref<UserInfo[]>([])
const licenses = ref<LicenseRecord[]>([])
const products = ref<Product[]>([])

/** 待人工核验订单数（角标） */
const pendingCount = computed(() => orders.value.filter((o) => o.status === 0).length)

async function loadAll() {
  loading.value = true
  try {
    const [orderList, userList, licenseList, productList] = await Promise.all([
      adminApi.listOrders(),
      adminApi.listUsers(),
      adminApi.listLicenses(),
      adminApi.listProducts(),
    ])
    orders.value = orderList
    users.value = userList
    licenses.value = licenseList
    products.value = productList
  } finally {
    loading.value = false
  }
}

onMounted(loadAll)

// ==================== 订单核验 ====================

async function reviewPass(order: Order) {
  try {
    await ElMessageBox.confirm(
      `确认已收到订单 ${order.orderNo}（${order.productName}，¥${order.amount}）的款项？\n核验通过后用户即可激活。`,
      '人工核验',
      { type: 'warning', confirmButtonText: '确认收款', cancelButtonText: '取消' },
    )
  } catch {
    return
  }
  await adminApi.reviewPass(order.orderNo)
  ElMessage.success('已核验通过')
  await loadAll()
}

// ==================== 用户管理 ====================

/** 用户详情抽屉数据（null = 未打开） */
const userDetailVisible = ref(false)
const userDetailLoading = ref(false)
const currentUserDetail = ref<UserDetail | null>(null)

async function openUserDetail(id: number) {
  userDetailVisible.value = true
  userDetailLoading.value = true
  currentUserDetail.value = null
  try {
    currentUserDetail.value = await adminApi.userDetail(id)
  } catch {
    userDetailVisible.value = false
  } finally {
    userDetailLoading.value = false
  }
}

// ==================== 激活码吊销 ====================

async function revokeLicense(row: LicenseRecord) {
  try {
    await ElMessageBox.confirm(
      `吊销后该激活码将立即失效，客户端在线核验会直接拒绝。\n确认吊销 ${row.licenseKey.slice(0, 16)}…？`,
      '吊销激活码',
      { type: 'warning', confirmButtonText: '确认吊销', cancelButtonText: '取消' },
    )
  } catch {
    return
  }
  await adminApi.revokeLicense(row.id)
  ElMessage.success('已吊销')
  await loadAll()
}

// ==================== 产品管理 ====================

const PRODUCT_DIALOG_TITLE = { create: '新建产品', edit: '编辑产品' } as const
const productDialog = ref(false)
const productSaving = ref(false)
const editingId = ref<number | null>(null) // null = 新建
const productFormRef = ref<FormInstance>()
const productForm = reactive<ProductInput>({
  name: '',
  code: '',
  description: '',
  version: '',
  coverUrl: '',
  downloadUrl: '',
  price: 0,
  status: 1,
  sort: 0,
})

const productRules: FormRules = {
  name: [{ required: true, message: '请输入产品名称', trigger: 'blur' }],
  code: [
    { required: true, message: '请输入产品编码', trigger: 'blur' },
    {
      pattern: /^[A-Za-z0-9_-]{1,64}$/,
      message: '仅允许字母/数字/下划线/中划线，长度 1-64',
      trigger: 'blur',
    },
  ],
  price: [{ required: true, message: '请输入价格', trigger: 'blur' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }],
}

function openCreate() {
  editingId.value = null
  Object.assign(productForm, {
    name: '',
    code: '',
    description: '',
    version: '',
    coverUrl: '',
    downloadUrl: '',
    price: 0,
    status: 1,
    sort: 0,
  })
  productDialog.value = true
}

function openEdit(p: Product) {
  editingId.value = p.id
  Object.assign(productForm, {
    name: p.name,
    code: p.code,
    description: p.description ?? '',
    version: p.version ?? '',
    coverUrl: p.coverUrl ?? '',
    downloadUrl: p.downloadUrl ?? '',
    price: p.price,
    status: p.status,
    sort: p.sort,
  })
  productDialog.value = true
}

async function saveProduct() {
  if (!productFormRef.value) return
  const valid = await productFormRef.value.validate().catch(() => false)
  if (!valid) return

  productSaving.value = true
  try {
    if (editingId.value == null) {
      await adminApi.createProduct({ ...productForm })
      ElMessage.success('产品已创建')
    } else {
      await adminApi.updateProduct(editingId.value, { ...productForm })
      ElMessage.success('产品已更新')
    }
    productDialog.value = false
    await loadAll()
  } finally {
    productSaving.value = false
  }
}

async function toggleStatus(p: Product) {
  const next = p.status === 1 ? 0 : 1
  await adminApi.setProductStatus(p.id, next)
  ElMessage.success(next === 1 ? '已上架' : '已下架')
  await loadAll()
}

function fmtTime(t: string | null | undefined) {
  return t ? t.replace('T', ' ').slice(0, 19) : '—'
}
</script>

<template>
  <div class="dashboard" v-loading="loading">
    <!-- 概览卡片 -->
    <el-row :gutter="16" class="stat-row">
      <el-col :xs="12" :sm="6">
        <div class="stat-card">
          <div class="stat-label">待核验订单</div>
          <div class="stat-value warn">{{ pendingCount }}</div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="6">
        <div class="stat-card">
          <div class="stat-label">累计订单</div>
          <div class="stat-value">{{ orders.length }}</div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="6">
        <div class="stat-card">
          <div class="stat-label">有效激活码</div>
          <div class="stat-value">{{ licenses.filter((l) => l.status !== 2).length }}</div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="6">
        <div class="stat-card">
          <div class="stat-label">在售产品</div>
          <div class="stat-value">{{ products.filter((p) => p.status === 1).length }}</div>
        </div>
      </el-col>
    </el-row>

    <el-tabs v-model="activeTab" class="dash-tabs">
      <!-- ============ 订单核验 ============ -->
      <el-tab-pane name="orders">
        <template #label>
          <el-badge :value="pendingCount" :hidden="pendingCount === 0" class="tab-badge">
            订单核验
          </el-badge>
        </template>

        <el-table :data="orders" stripe>
          <el-table-column prop="orderNo" label="订单号" width="200" />
          <el-table-column prop="productName" label="产品" min-width="140" />
          <el-table-column label="下单人联系方式" width="170">
            <template #default="{ row }">
              <span v-if="row.contact">{{ row.contact }}</span>
              <span v-else class="cell-muted">—</span>
            </template>
          </el-table-column>
          <el-table-column label="金额" width="100">
            <template #default="{ row }">¥{{ row.amount }}</template>
          </el-table-column>
          <el-table-column label="状态" width="120">
            <template #default="{ row }">
              <el-tag :type="ORDER_STATUS[row.status]?.type ?? 'info'">
                {{ ORDER_STATUS[row.status]?.label ?? row.status }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="下单时间" width="170">
            <template #default="{ row }">{{ fmtTime(row.createdAt) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="130" fixed="right">
            <template #default="{ row }">
              <el-button
                v-if="row.status === 0"
                type="primary"
                size="small"
                :icon="Checked"
                @click="reviewPass(row)"
              >
                确认收款
              </el-button>
              <span v-else class="cell-muted">—</span>
            </template>
          </el-table-column>
          <template #empty>暂无订单</template>
        </el-table>
      </el-tab-pane>

      <!-- ============ 用户管理 ============ -->
      <el-tab-pane label="用户" name="users">
        <el-alert
          class="mb-3"
          type="info"
          :closable="false"
          show-icon
          title="买家按下单联系方式（手机/邮箱）自动建档，作为用户唯一标识（微信认证登录已废除）。"
        />
        <el-table :data="users" stripe>
          <el-table-column prop="id" label="ID" width="70" />
          <el-table-column prop="contact" label="联系方式" min-width="170">
            <template #default="{ row }">
              <span class="contact-cell">{{ row.contact }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="nickname" label="昵称" min-width="120">
            <template #default="{ row }">
              <span v-if="row.nickname">{{ row.nickname }}</span>
              <span v-else class="cell-muted">—</span>
            </template>
          </el-table-column>
          <el-table-column prop="email" label="邮箱" min-width="160">
            <template #default="{ row }">
              <span v-if="row.email">{{ row.email }}</span>
              <span v-else class="cell-muted">—</span>
            </template>
          </el-table-column>
          <el-table-column label="订单数" width="90" align="center">
            <template #default="{ row }">{{ row.orderCount }}</template>
          </el-table-column>
          <el-table-column label="激活码数" width="90" align="center">
            <template #default="{ row }">{{ row.licenseCount }}</template>
          </el-table-column>
          <el-table-column label="首次下单" width="170">
            <template #default="{ row }">{{ fmtTime(row.createdAt) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="90" fixed="right">
            <template #default="{ row }">
              <el-button size="small" type="primary" plain :icon="User" @click="openUserDetail(row.id)">
                详情
              </el-button>
            </template>
          </el-table-column>
          <template #empty>暂无买家用户（用户下单后自动建档）</template>
        </el-table>
      </el-tab-pane>

      <!-- ============ 激活码 ============ -->
      <el-tab-pane label="激活码" name="licenses">
        <el-alert
          class="mb-3"
          type="info"
          :closable="false"
          show-icon
          title="激活码由客户端提交机器码自动签发（绑定机器，无法转移）；吊销后客户端在线核验立即失败。"
        />
        <el-table :data="licenses" stripe>
          <el-table-column label="激活码（机器码哈希-产品ID）" min-width="280">
            <template #default="{ row }">
              <el-tooltip :content="row.licenseKey" placement="top">
                <code class="license-key">{{ row.licenseKey }}</code>
              </el-tooltip>
            </template>
          </el-table-column>
          <el-table-column prop="productName" label="产品" width="140" />
          <el-table-column label="类型" width="80">
            <template #default="{ row }">{{ row.licenseType === 1 ? '永久' : '试用' }}</template>
          </el-table-column>
          <el-table-column label="状态" width="120">
            <template #default="{ row }">
              <el-tag :type="LICENSE_STATUS[row.status]?.type ?? 'info'">
                {{ LICENSE_STATUS[row.status]?.label ?? row.status }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="签发时间" width="170">
            <template #default="{ row }">{{ fmtTime(row.issuedAt) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="110" fixed="right">
            <template #default="{ row }">
              <el-button
                v-if="row.status !== 2"
                type="danger"
                size="small"
                plain
                :icon="Warning"
                @click="revokeLicense(row)"
              >
                吊销
              </el-button>
              <span v-else class="cell-muted">—</span>
            </template>
          </el-table-column>
          <template #empty>暂无激活码</template>
        </el-table>
      </el-tab-pane>

      <!-- ============ 产品管理 ============ -->
      <el-tab-pane label="产品管理" name="products">
        <div class="toolbar">
          <el-alert
            class="grow-alert"
            type="info"
            :closable="false"
            show-icon
            title="官网只展示上架产品；下载地址应为 exe 自解压安装包直链。"
          />
          <el-button type="primary" :icon="Plus" @click="openCreate">新建产品</el-button>
        </div>

        <el-table :data="products" stripe>
          <el-table-column prop="name" label="名称" min-width="140" />
          <el-table-column prop="code" label="编码" width="120">
            <template #default="{ row }">
              <code class="license-key">{{ row.code }}</code>
            </template>
          </el-table-column>
          <el-table-column prop="version" label="版本" width="90" />
          <el-table-column label="价格" width="100">
            <template #default="{ row }">¥{{ row.price }}</template>
          </el-table-column>
          <el-table-column label="状态" width="90">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'info'">
                {{ row.status === 1 ? '上架' : '下架' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="下载地址" min-width="220">
            <template #default="{ row }">
              <span v-if="row.downloadUrl" class="download-cell">
                <el-icon><Download /></el-icon>
                <el-link :href="row.downloadUrl" target="_blank" type="primary">
                  {{ row.downloadUrl.split('/').pop() || '安装包' }}
                </el-link>
              </span>
              <span v-else class="cell-muted">未配置</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="150" fixed="right">
            <template #default="{ row }">
              <el-button size="small" @click="openEdit(row)">编辑</el-button>
              <el-button
                size="small"
                :type="row.status === 1 ? 'warning' : 'success'"
                plain
                @click="toggleStatus(row)"
              >
                {{ row.status === 1 ? '下架' : '上架' }}
              </el-button>
            </template>
          </el-table-column>
          <template #empty>暂无产品</template>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <div class="refresh-bar">
      <el-button :icon="Refresh" @click="loadAll" :loading="loading">刷新数据</el-button>
    </div>

    <!-- 产品编辑弹窗 -->
    <el-dialog
      v-model="productDialog"
      :title="editingId == null ? PRODUCT_DIALOG_TITLE.create : PRODUCT_DIALOG_TITLE.edit"
      width="560px"
      destroy-on-close
    >
      <el-form ref="productFormRef" :model="productForm" :rules="productRules" label-width="92px">
        <el-form-item label="产品名称" prop="name">
          <el-input v-model="productForm.name" placeholder="如：coBrain" maxlength="64" />
        </el-form-item>
        <el-form-item label="产品编码" prop="code">
          <el-input
            v-model="productForm.code"
            placeholder="客户端对接用，如 coBrain"
            maxlength="64"
          />
        </el-form-item>
        <el-form-item label="版本号">
          <el-input v-model="productForm.version" placeholder="如 0.2.0" maxlength="32" />
        </el-form-item>
        <el-form-item label="价格（元）" prop="price">
          <el-input-number
            v-model="productForm.price"
            :min="0"
            :precision="2"
            :step="1"
            style="width: 180px"
          />
        </el-form-item>
        <el-form-item label="安装包直链">
          <el-input
            v-model="productForm.downloadUrl"
            placeholder="exe 自解压安装包下载地址（https://…/coBrain-setup.exe）"
          />
        </el-form-item>
        <el-form-item label="封面图">
          <el-input v-model="productForm.coverUrl" placeholder="https://…（可选）" />
        </el-form-item>
        <el-form-item label="简介">
          <el-input
            v-model="productForm.description"
            type="textarea"
            :rows="3"
            placeholder="官网产品卡片上的一句话介绍"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="上架状态">
          <el-switch
            v-model="productForm.status"
            :active-value="1"
            :inactive-value="0"
            active-text="上架"
            inactive-text="下架"
          />
        </el-form-item>
        <el-form-item label="排序权重">
          <el-input-number v-model="productForm.sort" :min="0" :max="9999" style="width: 140px" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="productDialog = false">取消</el-button>
        <el-button type="primary" :loading="productSaving" @click="saveProduct">保存</el-button>
      </template>
    </el-dialog>
    <!-- 用户详情弹窗 -->
    <el-dialog
      v-model="userDetailVisible"
      title="用户详情"
      width="880px"
      destroy-on-close
    >
      <div v-loading="userDetailLoading" class="user-detail">
        <template v-if="currentUserDetail">
          <el-descriptions :column="3" border class="mb-3">
            <el-descriptions-item label="联系方式">
              {{ currentUserDetail.user.contact }}
            </el-descriptions-item>
            <el-descriptions-item label="昵称">
              {{ currentUserDetail.user.nickname || '—' }}
            </el-descriptions-item>
            <el-descriptions-item label="邮箱">
              {{ currentUserDetail.user.email || '—' }}
            </el-descriptions-item>
            <el-descriptions-item label="订单数">
              {{ currentUserDetail.user.orderCount }}
            </el-descriptions-item>
            <el-descriptions-item label="激活码数">
              {{ currentUserDetail.user.licenseCount }}
            </el-descriptions-item>
            <el-descriptions-item label="首次下单">
              {{ fmtTime(currentUserDetail.user.createdAt) }}
            </el-descriptions-item>
          </el-descriptions>

          <div class="detail-block-title">名下订单（{{ currentUserDetail.orders.length }}）</div>
          <el-table :data="currentUserDetail.orders" size="small" stripe class="mb-3">
            <el-table-column prop="orderNo" label="订单号" width="190" />
            <el-table-column prop="productName" label="产品" min-width="130" />
            <el-table-column label="金额" width="90">
              <template #default="{ row }">¥{{ row.amount }}</template>
            </el-table-column>
            <el-table-column label="状态" width="110">
              <template #default="{ row }">
                <el-tag :type="ORDER_STATUS[row.status]?.type ?? 'info'" size="small">
                  {{ ORDER_STATUS[row.status]?.label ?? row.status }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="下单时间" width="160">
              <template #default="{ row }">{{ fmtTime(row.createdAt) }}</template>
            </el-table-column>
            <template #empty>暂无订单</template>
          </el-table>

          <div class="detail-block-title">名下激活码（{{ currentUserDetail.licenses.length }}）</div>
          <el-table :data="currentUserDetail.licenses" size="small" stripe>
            <el-table-column label="激活码（机器码哈希-产品ID）" min-width="260">
              <template #default="{ row }">
                <el-tooltip :content="row.licenseKey" placement="top">
                  <code class="license-key">{{ row.licenseKey }}</code>
                </el-tooltip>
              </template>
            </el-table-column>
            <el-table-column prop="productName" label="产品" width="140" />
            <el-table-column label="状态" width="110">
              <template #default="{ row }">
                <el-tag :type="LICENSE_STATUS[row.status]?.type ?? 'info'" size="small">
                  {{ LICENSE_STATUS[row.status]?.label ?? row.status }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="签发时间" width="160">
              <template #default="{ row }">{{ fmtTime(row.issuedAt) }}</template>
            </el-table-column>
            <template #empty>暂无激活码</template>
          </el-table>
        </template>
      </div>
    </el-dialog>
  </div>
</template>

<style scoped>
.dashboard {
  background: #fff;
  border: 1px solid var(--border-color);
  border-radius: 14px;
  padding: 20px 24px 16px;
  min-height: 60vh;
}

/* 概览卡片 */
.stat-row {
  margin-bottom: 18px;
}

.stat-card {
  background: linear-gradient(160deg, #f8f9ff, #f2f4fa);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  padding: 14px 18px;
}

.stat-label {
  font-size: 12.5px;
  color: var(--text-tertiary);
}

.stat-value {
  font-size: 26px;
  font-weight: 700;
  line-height: 1.3;
  margin-top: 2px;
}

.stat-value.warn {
  color: #e6a23c;
}

/* 标签页 */
.dash-tabs :deep(.el-tabs__header) {
  margin-bottom: 14px;
}

.tab-badge :deep(.el-badge__content) {
  transform: translate(4px, -8px);
}

/* 表格辅助样式 */
.license-key {
  font-family: 'JetBrains Mono', Consolas, monospace;
  font-size: 12.5px;
  color: var(--brand-color);
  word-break: break-all;
}

.cell-muted {
  color: var(--text-tertiary);
  font-size: 13px;
}

.download-cell {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  min-width: 0;
}

.toolbar {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 14px;
}

.grow-alert {
  flex: 1;
}

.mb-3 {
  margin-bottom: 12px;
}

.contact-cell {
  font-weight: 600;
  color: var(--brand-color);
}

.user-detail {
  min-height: 120px;
}

.detail-block-title {
  font-size: 13.5px;
  font-weight: 600;
  color: var(--text-secondary);
  margin: 2px 0 8px;
}

.refresh-bar {
  display: flex;
  justify-content: flex-end;
  margin-top: 14px;
}
</style>
