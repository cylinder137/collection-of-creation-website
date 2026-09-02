<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { productApi } from '@/api'
import type { Product } from '@/types'

/**
 * 造物集官网主页
 *
 * 业务定位（2026-09 调整）：
 * 官网不发售激活码、不做在线购买，只负责「产品展示 + 安装包下载」。
 * 购买与激活全部下沉到桌面客户端：安装程序可提权读取本机机器码并向后端直接发起激活请求，
 * 绕开浏览器的沙箱限制。
 */

const products = ref<Product[]>([])
const loading = ref(false)
const loadError = ref('')

async function loadProducts() {
  loading.value = true
  loadError.value = ''
  try {
    products.value = await productApi.list()
  } catch {
    loadError.value = '产品信息加载失败，请稍后刷新重试'
  } finally {
    loading.value = false
  }
}

onMounted(loadProducts)

/** Hero 右侧的产品统计 */
const stats = computed(() => [
  { value: products.value.length ? `${products.value.length}` : '—', label: '在售产品' },
  { value: '1 分钟', label: '安装耗时' },
  { value: '离线可用', label: '激活后校验' },
])

const guideSteps = [
  {
    step: '01',
    title: '下载安装包',
    desc: '从上方产品卡片获取最新版 exe 自解压安装包，无需注册、无需留联系方式。',
  },
  {
    step: '02',
    title: '安装并完成激活',
    desc: '安装程序会自动采集本机机器码；在客户端内完成购买后即可一键激活，全程不用手动复制机器码。',
  },
  {
    step: '03',
    title: '启动即用',
    desc: '激活信息落盘后，后续每次启动只做本地签名校验，断网也能正常打开，不打扰你的创作。',
  },
]

const faqs = [
  {
    q: '激活码在哪里买？官网怎么没有购买按钮？',
    a: '购买入口做在了产品客户端里：打开已安装的软件，在「帮助 / 激活」中选择购买，按提示付款即可。官网只负责分发安装包，不参与交易，这样你不必在网页上手动复制机器码，也不用担心换浏览器导致机器码对不上。',
  },
  {
    q: '激活需要一直联网吗？',
    a: '不需要。首次激活需要联网向服务端申请一次激活码；激活成功后，激活码与 RSA 签名会保存在本机，之后每次启动只做本地验签，完全离线也能用。服务端仅在你主动发起在线核验时才会被访问。',
  },
  {
    q: '一个激活码能用几台电脑？',
    a: '一单一机：每笔订单只能为一台机器签发激活码。激活码与机器码哈希强绑定，换机器后原激活码无法使用。',
  },
  {
    q: '换电脑或重装系统后怎么办？',
    a: '重装系统一般不影响（机器码取自硬件特征）。更换整机属于新设备，请联系客服（官网底部邮箱或在客户端内提交解绑申请），我们会为你做设备迁移。',
  },
  {
    q: '支持哪些操作系统？',
    a: '目前所有产品均提供 Windows 10 及以上版本的 exe 自解压安装包，安装过程需要管理员权限（用于读取硬件特征生成机器码）。',
  },
  {
    q: '安装包安全吗？会不会有捆绑？',
    a: '所有安装包均由造物集官方签名打包，不含任何第三方捆绑。若杀软误报，可比对官网提供的文件哈希或联系我们处理。',
  },
]

function scrollToAnchor(selector: string) {
  document.querySelector(selector)?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

function download(product: Product) {
  if (!product.downloadUrl) return
  window.open(product.downloadUrl, '_blank')
}
</script>

<template>
  <div class="home">
    <!-- ============ Hero ============ -->
    <section id="top" class="hero">
      <div class="hero-glow" />
      <div class="container hero-inner">
        <div class="hero-copy">
          <span class="hero-badge">大连造物集有限公司 · 官方主页</span>
          <h1 class="hero-title">
            创造者的集合，<br />
            <span class="hero-title-accent">趁手的工具箱</span>
          </h1>
          <p class="hero-desc">
            我们为创作者打磨可靠的桌面软件。下载、安装、激活，三步到位；
            装好之后它只安静地待在后台，把舞台留给你。
          </p>
          <div class="hero-actions">
            <el-button type="primary" size="large" round @click="scrollToAnchor('#products')">
              <el-icon class="mr-1"><Download /></el-icon>
              下载客户端
            </el-button>
            <el-button size="large" round @click="scrollToAnchor('#guide')">
              了解激活流程
            </el-button>
          </div>
          <p class="hero-note">Windows 10+ · exe 自解压安装包 · 无需注册即可下载</p>
        </div>

        <div class="hero-panel">
          <div class="panel-card">
            <div class="panel-card-head">
              <span class="dot dot-red" />
              <span class="dot dot-yellow" />
              <span class="dot dot-green" />
              <span class="panel-card-title">造物集 · 产品矩阵</span>
            </div>
            <ul class="panel-list">
              <li v-for="s in stats" :key="s.label">
                <span class="panel-value">{{ s.value }}</span>
                <span class="panel-label">{{ s.label }}</span>
              </li>
            </ul>
            <div class="panel-foot">
              <el-icon><Lock /></el-icon>
              <span>激活码 RSA 签名 + 机器码绑定，本地验签</span>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- ============ 产品下载 ============ -->
    <section id="products" class="section section--subtle">
      <div class="container">
        <div class="section-head">
          <span class="section-eyebrow">PRODUCTS</span>
          <h2 class="section-title">选择你的工具</h2>
          <p class="section-desc">
            所有产品均为 Windows 自解压安装包，下载后双击即可开始安装。
            购买与激活在客户端内完成，官网不发售激活码。
          </p>
        </div>

        <div v-if="loading" class="product-grid">
          <el-skeleton v-for="i in 3" :key="i" style="height: 240px" animated />
        </div>

        <el-alert
          v-else-if="loadError"
          :title="loadError"
          type="error"
          show-icon
          :closable="false"
        />

        <div v-else-if="products.length" class="product-grid">
          <article v-for="product in products" :key="product.id" class="product-card">
            <header class="product-card-head">
              <div class="product-logo">{{ product.name.slice(0, 1) }}</div>
              <div>
                <h3 class="product-name">{{ product.name }}</h3>
                <span class="product-version">
                  {{ product.version ? `v${product.version}` : '最新版本' }}
                </span>
              </div>
            </header>

            <p class="product-desc">{{ product.description }}</p>

            <div class="product-meta">
              <span class="product-price">
                <em>¥</em>{{ product.price.toFixed(2) }}
              </span>
              <span class="product-price-note">买断制 · 一单一机</span>
            </div>

            <el-button
              class="product-download"
              type="primary"
              :disabled="!product.downloadUrl"
              @click="download(product)"
            >
              <el-icon class="mr-1"><Download /></el-icon>
              {{ product.downloadUrl ? '下载安装包' : '下载即将开放' }}
            </el-button>
          </article>
        </div>

        <el-empty v-else description="暂无在售产品" />
      </div>
    </section>

    <!-- ============ 安装与激活 ============ -->
    <section id="guide" class="section">
      <div class="container">
        <div class="section-head">
          <span class="section-eyebrow">GETTING STARTED</span>
          <h2 class="section-title">三步，从下载到开工</h2>
          <p class="section-desc">
            激活逻辑放在本机客户端而不是网页：安装程序可以申请管理员权限直接读取硬件特征，
            不必再受浏览器沙箱的限制。
          </p>
        </div>

        <div class="guide-grid">
          <div v-for="item in guideSteps" :key="item.step" class="guide-card">
            <span class="guide-step">{{ item.step }}</span>
            <h3 class="guide-title">{{ item.title }}</h3>
            <p class="guide-desc">{{ item.desc }}</p>
          </div>
        </div>
      </div>
    </section>

    <!-- ============ 常见问题 ============ -->
    <section id="faq" class="section section--subtle">
      <div class="container container--narrow">
        <div class="section-head">
          <span class="section-eyebrow">FAQ</span>
          <h2 class="section-title">常见问题</h2>
        </div>

        <el-collapse class="faq-collapse">
          <el-collapse-item v-for="(item, index) in faqs" :key="index" :name="index">
            <template #title>
              <span class="faq-question">{{ item.q }}</span>
            </template>
            <p class="faq-answer">{{ item.a }}</p>
          </el-collapse-item>
        </el-collapse>
      </div>
    </section>

    <!-- ============ 关于我们 ============ -->
    <section id="about" class="section">
      <div class="container about">
        <div class="about-copy">
          <span class="section-eyebrow">ABOUT US</span>
          <h2 class="section-title">关于造物集</h2>
          <p>
            大连造物集有限公司成立于 2026 年，是一支由在校开发者组成的小而精的团队。
            我们相信工具的最高境界是「感觉不到它的存在」——所以我们把精力花在稳定性、
            启动速度和激活体验这些不出彩但每天都会被感知的地方。
          </p>
          <p>
            造物集，创造者的集合。我们做工具，你把想法变成作品。
          </p>
          <div class="about-tags">
            <span class="about-tag">本地优先</span>
            <span class="about-tag">离线可用</span>
            <span class="about-tag">买断制</span>
            <span class="about-tag">无广告 · 无捆绑</span>
          </div>
        </div>
        <div class="about-card">
          <h4>商务与合作</h4>
          <p class="about-card-line">产品咨询、批量授权、渠道合作</p>
          <p class="about-card-mail">contact@collectionofcreation.uk</p>
          <el-divider />
          <h4>技术支持</h4>
          <p class="about-card-line">激活异常、换机迁移、发票问题</p>
          <p class="about-card-mail">support@collectionofcreation.uk</p>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
/* ---------- Hero ---------- */

.hero {
  position: relative;
  overflow: hidden;
  padding: 96px 0 88px;
  background:
    radial-gradient(1200px 480px at 12% -10%, rgba(79, 110, 247, 0.12), transparent 60%),
    radial-gradient(900px 420px at 88% 0%, rgba(168, 85, 247, 0.12), transparent 62%),
    var(--bg-page);
}

.hero-glow {
  position: absolute;
  top: -180px;
  left: 50%;
  transform: translateX(-50%);
  width: 900px;
  height: 420px;
  background: var(--brand-gradient);
  opacity: 0.07;
  filter: blur(90px);
  border-radius: 50%;
  pointer-events: none;
}

.hero-inner {
  position: relative;
  display: grid;
  grid-template-columns: 1.15fr 0.85fr;
  gap: 56px;
  align-items: center;
}

.hero-badge {
  display: inline-block;
  font-size: 13px;
  font-weight: 600;
  color: var(--brand-color);
  background: var(--brand-color-light);
  border: 1px solid rgba(79, 110, 247, 0.16);
  padding: 6px 14px;
  border-radius: 999px;
  margin-bottom: 24px;
}

.hero-title {
  font-size: 54px;
  line-height: 1.18;
  font-weight: 800;
  letter-spacing: -0.02em;
  margin-bottom: 22px;
}

.hero-title-accent {
  background: var(--brand-gradient);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}

.hero-desc {
  font-size: 17px;
  color: var(--text-secondary);
  max-width: 520px;
  margin-bottom: 34px;
}

.hero-actions {
  display: flex;
  gap: 14px;
  flex-wrap: wrap;
  margin-bottom: 18px;
}

.hero-note {
  font-size: 13px;
  color: var(--text-tertiary);
}

.hero-panel {
  display: flex;
  justify-content: flex-end;
}

.panel-card {
  width: 100%;
  max-width: 380px;
  background: #fff;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-lg);
  overflow: hidden;
}

.panel-card-head {
  display: flex;
  align-items: center;
  gap: 7px;
  padding: 14px 18px;
  background: var(--bg-subtle);
  border-bottom: 1px solid var(--border-color);
}

.dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  display: inline-block;
}

.dot-red {
  background: #f76560;
}
.dot-yellow {
  background: #f7bd4b;
}
.dot-green {
  background: #4ecb73;
}

.panel-card-title {
  margin-left: 8px;
  font-size: 13px;
  color: var(--text-secondary);
  font-weight: 500;
}

.panel-list {
  list-style: none;
  padding: 8px 18px 4px;
}

.panel-list li {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  padding: 14px 0;
  border-bottom: 1px dashed var(--border-color);
}

.panel-list li:last-child {
  border-bottom: none;
}

.panel-value {
  font-size: 22px;
  font-weight: 700;
  color: var(--text-main);
}

.panel-label {
  font-size: 14px;
  color: var(--text-secondary);
}

.panel-foot {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 18px;
  background: var(--bg-sunken);
  font-size: 12.5px;
  color: var(--text-secondary);
}

/* ---------- 产品卡片 ---------- */

.product-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 24px;
}

.product-card {
  display: flex;
  flex-direction: column;
  background: #fff;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  padding: 28px;
  box-shadow: var(--shadow-sm);
  transition: transform 0.22s, box-shadow 0.22s, border-color 0.22s;
}

.product-card:hover {
  transform: translateY(-4px);
  border-color: rgba(79, 110, 247, 0.32);
  box-shadow: var(--shadow-md);
}

.product-card-head {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 16px;
}

.product-logo {
  width: 46px;
  height: 46px;
  border-radius: 12px;
  background: var(--brand-gradient);
  color: #fff;
  font-size: 20px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.product-name {
  font-size: 18px;
  font-weight: 700;
}

.product-version {
  font-size: 13px;
  color: var(--text-tertiary);
}

.product-desc {
  font-size: 14.5px;
  color: var(--text-secondary);
  min-height: 70px;
  margin-bottom: 18px;
}

.product-meta {
  display: flex;
  align-items: baseline;
  gap: 10px;
  margin-bottom: 20px;
}

.product-price {
  font-size: 28px;
  font-weight: 700;
  color: var(--text-main);
}

.product-price em {
  font-size: 16px;
  font-style: normal;
  margin-right: 2px;
}

.product-price-note {
  font-size: 13px;
  color: var(--text-tertiary);
}

.product-download {
  width: 100%;
  margin-top: auto;
}

.mr-1 {
  margin-right: 4px;
}

/* ---------- 引导步骤 ---------- */

.guide-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 24px;
}

.guide-card {
  position: relative;
  background: #fff;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  padding: 32px 28px 28px;
  box-shadow: var(--shadow-sm);
}

.guide-step {
  font-size: 15px;
  font-weight: 800;
  letter-spacing: 0.06em;
  background: var(--brand-gradient);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}

.guide-title {
  font-size: 19px;
  font-weight: 700;
  margin: 8px 0 12px;
}

.guide-desc {
  font-size: 14.5px;
  color: var(--text-secondary);
}

/* ---------- FAQ ---------- */

.container--narrow {
  max-width: 860px;
}

.faq-collapse {
  background: #fff;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  overflow: hidden;
  padding: 0 20px;
}

.faq-question {
  font-size: 15.5px;
  font-weight: 600;
}

.faq-answer {
  font-size: 14.5px;
  color: var(--text-secondary);
  line-height: 1.8;
  padding-right: 12px;
}

/* ---------- 关于我们 ---------- */

.about {
  display: grid;
  grid-template-columns: 1.25fr 0.75fr;
  gap: 56px;
  align-items: start;
}

.about-copy p {
  font-size: 15.5px;
  color: var(--text-secondary);
  margin-bottom: 16px;
  line-height: 1.9;
}

.about-tags {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  margin-top: 22px;
}

.about-tag {
  font-size: 13px;
  color: var(--brand-color);
  background: var(--brand-color-light);
  padding: 6px 14px;
  border-radius: 999px;
}

.about-card {
  background: var(--bg-subtle);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  padding: 28px;
}

.about-card h4 {
  font-size: 15px;
  font-weight: 700;
  margin-bottom: 8px;
}

.about-card-line {
  font-size: 13.5px;
  color: var(--text-secondary);
}

.about-card-mail {
  font-size: 14.5px;
  font-weight: 600;
  color: var(--brand-color);
  margin-top: 2px;
  word-break: break-all;
}

.about-card .el-divider {
  margin: 20px 0;
}

/* ---------- 响应式 ---------- */

@media (max-width: 960px) {
  .hero-inner,
  .about {
    grid-template-columns: 1fr;
    gap: 40px;
  }

  .hero-title {
    font-size: 38px;
  }

  .hero-panel {
    justify-content: flex-start;
  }
}
</style>
