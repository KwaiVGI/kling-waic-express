import { createI18n } from 'vue-i18n'
import enUS from 'vant/es/locale/lang/en-US'
import zhCN from 'vant/es/locale/lang/zh-CN'
import { Locale as LocaleVant } from 'vant'
import type { PickerColumn } from 'vant'
import en from '@/locales/en-US.json'
import zh from '@/locales/zh-CN.json'
import setPageTitle from './set-page-title'

const FALLBACK_LOCALE = 'zh-CN'

const vantLocales = {
  'zh-CN': zhCN,
  'en-US': enUS,
}

export const languageColumns: PickerColumn = [
  { text: '简体中文', value: 'zh-CN' },
  { text: 'English', value: 'en-US' },
]

export type Locale = 'zh-CN' | 'en-US'

export const i18n = setupI18n()
type I18n = typeof i18n

export const locale = computed<Locale>({
  get() {
    return i18n.global.locale.value as Locale
  },
  set(language: string) {
    setLang(language, i18n)
  },
})

function setupI18n() {
  const locale = getI18nLocale()
  const i18n = createI18n({
    locale,
    legacy: false,
    fallbackLocale: FALLBACK_LOCALE,
    messages: {
      'zh-CN': zh,
      'en-US': en,
    },
  })
  setLang(locale, i18n)
  return i18n
}

async function setLang(lang: string, i18n: I18n) {
  // await loadLocaleMsg(lang, i18n);

  document.querySelector('html').setAttribute('lang', lang)
  localStorage.setItem('language', lang)
  i18n.global.locale.value = lang as Locale

  // 设置 vant 组件语言包
  LocaleVant.use(lang, vantLocales[lang])
  const { t } = i18n.global
  setPageTitle(t('brand.appName'))
}

// 加载本地语言包
async function loadLocaleMsg(locale: string, i18n: I18n) {
  const messages = await import(`../locales/${locale}.json`)
  i18n.global.setLocaleMessage(locale, messages.default)
}

// 获取当前语言对应的语言包名称
function getI18nLocale() {
  const urlParams = new URLSearchParams(window.location.search)
  const urlLang = urlParams.get('lang')
  const storedLocale
    = urlLang || localStorage.getItem('language') || navigator.language

  const langs = languageColumns.map(v => v.value as string)

  // 存在当前语言的语言包 或 存在当前语言的任意地区的语言包
  const foundLocale = langs.find(
    v => v === storedLocale || v.indexOf(storedLocale) === 0,
  )

  // 若未找到，则使用 默认语言包
  const locale = foundLocale || FALLBACK_LOCALE

  return locale as Locale
}
