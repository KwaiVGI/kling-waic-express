import { ref, computed } from 'vue'
import { showToast } from 'vant'

export type CreationType = 'image' | 'video'

export default function useCreation(creationType: CreationType) {
  // 图片上传大小限制（单位：字节）
  const maxFileSize = ref<number>(10 * 1024 * 1024) // 默认10MB
  const maxFileSizeMB = computed(() => Math.floor(maxFileSize.value / (1024 * 1024)))
  const uploaderRef = ref()
  // 状态管理
  const fileList = ref<any[]>([])
  const uploadedImage = ref<string | null>(null)
  const generatedResult = ref<string | null>(null)
  const showPreview = ref(false)
  const previewItems = ref<string[]>([])
  const previewIndex = ref(0)
  const showSaveGuide = ref(false)
  const isLoading = ref(false)
  const loadingText = ref("处理中...")
  const isGenerating = ref(false)
  const isSaving = ref(false)
  const isPrinting = ref(false)
  
  // 计算模糊背景样式
  const blurStyle = computed(() => {
    if (!generatedResult.value) return {}
    return {
      backgroundImage: `url(${uploadedImage.value})`,
      filter: "blur(20px) brightness(0.8)",
    }
  })

  // 文件大小超出限制
  const onOversize = () => {
    showToast({
      type: "fail",
      message: `图片大小不能超过${maxFileSizeMB.value}MB`,
      duration: 2000,
    })
  }

  // 处理图片上传
  const handleUpload = (file: any) => {

    if (file.file) {
        // 检查文件大小
        if (file.file.size > maxFileSize.value) {
          onOversize()
          return
        }

        const reader = new FileReader()
        reader.onload = (e) => {
          uploadedImage.value = e.target?.result as string
          generatedResult.value = null
        }
        reader.readAsDataURL(file.file)
      } else {
        showToast({
          type: "fail",
          message: "上传失败，请重试",
          duration: 1500,
        })
      }
  }

  // 打开预览
  const openPreview = (url: string) => {
    previewItems.value = [url]
    previewIndex.value = 0
    showPreview.value = true
  }

  // 删除图片
  const handleDelete = () => {
     fileList.value = []
    uploadedImage.value = null
    generatedResult.value = null
  }

  // 替换图片
  const handleReplace = () => {
    if (uploaderRef.value) {
      uploaderRef.value.chooseFile()
    }
  }

  // 生成内容
  const generate = async (generateFn: (file: string, type: CreationType) => Promise<string>) => {
    if (!uploadedImage.value) {
      showToast("请先上传图片")
      return
    }

    isGenerating.value = true

    try {
      const result = await generateFn(uploadedImage.value, creationType)
      generatedResult.value = result
      showToast({
        type: "success",
        message: "创作成功！",
        duration: 1500,
      })
    } catch (error) {
      generatedResult.value = null
      showToast({
        type: "fail",
        message: "哎呀失败了，换一张图试试吧~",
        duration: 2000,
      })
      console.error("生成失败:", error)
    } finally {
      isGenerating.value = false
    }
  }

  // 保存内容
  const save = (filename: string, extension: string) => {
    if (!generatedResult.value) {
      showToast("没有可保存的内容")
      return
    }

    // 检查是否在微信环境中
    const isWeChat = /MicroMessenger/i.test(navigator.userAgent)

    if (isWeChat) {
      showSaveGuide.value = true
      return
    }

    isSaving.value = true

    try {
      const link = document.createElement("a")
      link.href = generatedResult.value
      link.download = `${filename}_${new Date().getTime()}.${extension}`
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      showToast({
        type: "success",
        message: "保存成功！",
        duration: 1500,
      })
    } catch (error) {
      showToast({
        type: "fail",
        message: "保存失败，请重试",
        duration: 1500,
      })
      console.error("保存失败:", error)
    } finally {
      isSaving.value = false
    }
  }

  // 返回编辑
  const backToEdit = () => {
    generatedResult.value = null
  }

  // 处理保存指导弹窗确认
  const handleSaveGuideConfirm = () => {
    showToast("请在浏览器中打开本页面进行保存")
  }

  // 打印图片（仅图片类型）
  const printImage = () => {
    if (!generatedResult.value || creationType !== 'image') {
      showToast("没有可打印的内容")
      return
    }

    isPrinting.value = true

    setTimeout(() => {
      showToast({
        type: "success",
        message: "打印任务已发送",
        duration: 1500,
      })
      isPrinting.value = false
    }, 1500)
  }

  return {
    uploaderRef,
    creationType,
    maxFileSize,
    maxFileSizeMB,
    fileList,
    uploadedImage,
    generatedResult,
    isLoading,
    loadingText,
    isGenerating,
    isSaving,
    isPrinting,
    showPreview,
    previewItems,
    previewIndex,
    showSaveGuide,
    blurStyle,
    
    handleUpload,
    onOversize,
    openPreview,
    handleDelete,
    handleReplace,
    generate,
    save,
    backToEdit,
    handleSaveGuideConfirm,
    printImage,
  }
}