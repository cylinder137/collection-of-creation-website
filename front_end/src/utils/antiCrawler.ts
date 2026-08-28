/**
 * 爬虫/自动化环境检测（轻量指纹）
 *
 * 命中特征即阻断应用挂载，数据层根本不会初始化——
 * 爬虫想继续，必须完整伪装真实浏览器指纹，而不只是"能跑 JS"。
 */

/** 常见无头浏览器 / 自动化框架 / 爬虫 UA 特征 */
const HEADLESS_UA = [
  /headless/i,
  /phantom/i,
  /selenium/i,
  /puppeteer/i,
  /playwright/i,
  /crawler/i,
  /spider/i,
  /scraper/i,
  /python-requests/i,
  /curl\//i,
  /wget\//i,
]

/**
 * 返回命中原因；未命中返回 null。
 * 组合判断减少误伤（隐私浏览器可能单项异常，但多项同时异常基本是无头环境）。
 */
export function detectBotEnv(): string | null {
  const nav = navigator

  // 硬特征：W3C 自动化标志（Selenium/Puppeteer/Playwright 均为 true）
  if (nav.webdriver) return 'navigator.webdriver=true（自动化控制特征）'

  const ua = nav.userAgent || ''
  if (HEADLESS_UA.some((re) => re.test(ua))) return `UA 命中无头/爬虫特征`

  // 组合软特征：无插件 + 无语言列表（真实中文浏览器几乎不会同时为空）
  const noPlugins = (nav.plugins?.length ?? 0) === 0
  const noLangs = (nav.languages?.length ?? 0) === 0
  if (noPlugins && noLangs) return '插件与语言列表同时为空（无头浏览器特征）'

  return null
}

/**
 * 生产环境启用：检测到爬虫环境时替换页面并中止后续代码。
 * 开发环境（import.meta.env.DEV）放行，不影响本地调试与 preview。
 */
export function assertHumanEnv(): void {
  if (import.meta.env.DEV) return
  const reason = detectBotEnv()
  if (reason) {
    document.body.innerHTML =
      '<div style="padding:48px;text-align:center;color:#666;font-size:14px">访问环境异常，请使用正常浏览器访问</div>'
    throw new Error(`[anti-crawler] blocked: ${reason}`)
  }
}
