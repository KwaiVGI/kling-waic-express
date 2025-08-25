<script setup lang="ts">
import { locale } from '@/utils/i18n'

const emit = defineEmits(['change'])
function switchLanguage() {
  const newLocale = locale.value === 'zh-CN' ? 'en-US' : 'zh-CN'
  locale.value = newLocale
  emit('change', newLocale)
}
</script>

<template>
  <button class="language-btn" @click="switchLanguage">
    <span>{{ $t("language.switch") }}</span>
    <img
      class="h-16px w-16px"
      src="https://ali.a.yximgs.com/kos/nlav12119/SPLdxJSr_2025-07-22-11-52-18.png"
      alt=""
    >
  </button>
</template>

<style scoped>
.language-btn {
  padding: 0 12px;
  height: 32px;
  min-width: 70px;
  border-radius: 32px;
  background: #fff;
  color: black;
  font-size: 14px;
  border: none;
  cursor: pointer;
  transition: background 0.3s;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  box-shadow: 0px 2px 4px 0px #3b95a329;
}
</style>
