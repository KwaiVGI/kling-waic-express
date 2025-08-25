<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { fetchConfig, saveConfig } from '@/api/admin'
import type { AdminConfig } from '@/api/admin'
import { showToast } from 'vant'

// 配置相关状态
const config = ref<AdminConfig | null>(null)
const loading = ref(false)
const saving = ref(false)

// 加载配置
async function loadConfig() {
  loading.value = true
  try {
    config.value = await fetchConfig()
    showToast('配置加载成功')
  }
  catch (error) {
    console.error('加载配置失败:', error)
    showToast('加载配置失败，请稍后重试')
  }
  finally {
    loading.value = false
  }
}

// 保存配置
async function handleSaveConfig() {
  if (!config.value)
    return

  saving.value = true
  try {
    await saveConfig(config.value)
    showToast('配置保存成功')
  }
  catch (error) {
    console.error('保存配置失败:', error)
    showToast('保存配置失败，请稍后重试')
  }
  finally {
    saving.value = false
  }
}

// 页面加载时自动获取配置
onMounted(() => {
  loadConfig()
})
</script>

<template>
  <div class="config-container">
    <div class="config-header">
      <h2>系统配置</h2>
      <div class="config-actions">
        <button
          class="btn-secondary btn"
          :disabled="loading"
          @click="loadConfig"
        >
          {{ loading ? "加载中..." : "刷新配置" }}
        </button>
        <button
          class="btn-primary btn"
          :disabled="saving"
          @click="handleSaveConfig"
        >
          {{ saving ? "保存中..." : "保存配置" }}
        </button>
      </div>
    </div>

    <div v-if="config" class="config-content">
      <div class="config-grid">
        <div class="config-item">
          <div class="switch-item">
            <span class="switch-label">允许打印</span>
            <van-switch
              v-model="config.allowPrint"
              size="20px"
              active-color="#4361ee"
            />
          </div>
        </div>

        <div class="config-item">
          <div class="switch-item">
            <span class="switch-label">图片服务在线</span>
            <van-switch
              v-model="config.imageServiceOnline"
              size="20px"
              active-color="#4361ee"
            />
          </div>
        </div>

        <div class="config-item">
          <div class="switch-item">
            <span class="switch-label">视频服务在线</span>
            <van-switch
              v-model="config.videoServiceOnline"
              size="20px"
              active-color="#4361ee"
            />
          </div>
        </div>

        <div class="config-item">
          <div class="input-item">
            <span class="input-label">图片Token过期时间(秒)</span>
            <input
              v-model.number="config.imageTokenExpireInSeconds"
              type="number"
              class="config-input-inline"
              min="1"
            >
          </div>
        </div>

        <div class="config-item">
          <div class="input-item">
            <span class="input-label">视频Token过期时间(秒)</span>
            <input
              v-model.number="config.videoTokenExpireInSeconds"
              type="number"
              class="config-input-inline"
              min="1"
            >
          </div>
        </div>

        <div class="config-item">
          <div class="input-item">
            <span class="input-label">打印机最大任务数</span>
            <input
              v-model.number="config.maxPrinterJobCount"
              type="number"
              class="config-input-inline"
              min="1"
            >
          </div>
        </div>

        <div class="config-item">
          <div class="input-item">
            <span class="input-label">图片大屏宽高比</span>
            <div class="ratio-input">
              <input
                v-model.number="config.screenImageRatios.first"
                type="number"
                class="config-input-small"
                min="1"
              >
              <span>:</span>
              <input
                v-model.number="config.screenImageRatios.second"
                type="number"
                class="config-input-small"
                min="1"
              >
            </div>
          </div>
        </div>

        <div class="config-item">
          <div class="input-item">
            <span class="input-label">视频大屏宽高比</span>
            <div class="ratio-input">
              <input
                v-model.number="config.screenVideoRatios.first"
                type="number"
                class="config-input-small"
                min="1"
              >
              <span>:</span>
              <input
                v-model.number="config.screenVideoRatios.second"
                type="number"
                class="config-input-small"
                min="1"
              >
            </div>
          </div>
        </div>
      </div>
    </div>

    <div v-else-if="!loading" class="config-empty">
      <p>暂无配置数据，请点击刷新配置</p>
    </div>
  </div>
</template>

<style scoped>
.config-container {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 12px;
  background-color: #f5f7fa;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  height: 100%;
}

.config-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #e5e7eb;
}

.config-header h2 {
  font-size: 1.3rem;
  color: #374151;
  margin: 0;
  font-weight: 600;
}

.config-actions {
  display: flex;
  gap: 8px;
}

.btn {
  padding: 8px 16px;
  border: none;
  border-radius: 4px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-primary {
  background-color: #4361ee;
  color: white;
}

.btn-primary:hover:not(:disabled) {
  background-color: #3651d4;
}

.btn-secondary {
  background-color: #f0f2f5;
  color: #374151;
  border: 1px solid #d1d5db;
}

.btn-secondary:hover:not(:disabled) {
  background-color: #e5e7eb;
}

.config-content {
  flex: 1;
  opacity: 1;
  transition: opacity 0.3s ease;
}

.config-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 16px;
}

.config-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.config-label {
  font-size: 14px;
  color: #374151;
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 8px;
}

.switch-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: white;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  transition: all 0.2s ease;
}

.switch-item:hover {
  border-color: #4361ee;
  box-shadow: 0 0 0 2px rgba(67, 97, 238, 0.1);
}

.switch-label {
  font-size: 14px;
  color: #374151;
  font-weight: 500;
}

.input-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: white;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  transition: all 0.2s ease;
}

.input-item:hover {
  border-color: #4361ee;
  box-shadow: 0 0 0 2px rgba(67, 97, 238, 0.1);
}

.input-label {
  font-size: 14px;
  color: #374151;
  font-weight: 500;
  flex-shrink: 0;
}

.config-input-inline {
  padding: 6px 12px;
  background: #f9fafb;
  border: 1px solid #d1d5db;
  border-radius: 4px;
  color: #374151;
  font-size: 14px;
  transition: all 0.2s ease;
  width: 120px;
  text-align: center;
}

.config-input-inline:focus {
  outline: none;
  border-color: #4361ee;
  background: white;
  box-shadow: 0 0 0 2px rgba(67, 97, 238, 0.1);
}

.config-input,
.config-input-small {
  padding: 8px 12px;
  background: white;
  border: 1px solid #d1d5db;
  border-radius: 4px;
  color: #374151;
  font-size: 14px;
  transition: all 0.2s ease;
}

.config-input:focus,
.config-input-small:focus {
  outline: none;
  border-color: #4361ee;
  box-shadow: 0 0 0 2px rgba(67, 97, 238, 0.1);
}

.config-input-small {
  width: 50px;
  text-align: center;
  padding: 6px 8px;
  background: #f9fafb;
}

.config-input-small:focus {
  background: white;
}

.ratio-input {
  display: flex;
  align-items: center;
  gap: 8px;
}

.ratio-input span {
  color: #6b7280;
  font-weight: 500;
}

.config-empty {
  display: flex;
  justify-content: center;
  align-items: center;
  flex: 1;
  text-align: center;
  padding: 40px;
  color: #6b7280;
}

.config-empty p {
  margin: 0;
  font-size: 14px;
}
</style>
