import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

import App from './App.vue'
import router from './router'
import { assertHumanEnv } from './utils/antiCrawler'

// Element Plus 全量样式
import 'element-plus/dist/index.css'
// 全局自定义样式
import './styles/index.css'

// 反爬：生产环境先检测自动化/无头浏览器特征，命中则拒绝挂载（开发环境放行）
assertHumanEnv()

const app = createApp(App)

// 全局注册 Element Plus 图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.use(createPinia())
app.use(router)
app.use(ElementPlus, { locale: zhCn })

app.mount('#app')
