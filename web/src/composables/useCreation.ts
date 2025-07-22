import { ref, computed } from "vue";
import { showToast } from "vant";
import { printImageTask } from "@/api/creation";
import { saveAs } from "file-saver";
import { updateQueryParams } from "@/utils/url";

export type CreationType = "image" | "video";

export default function useCreation(creationType: CreationType) {
  const { t } = useI18n();
  // 图片上传大小限制（单位：字节）
  const maxFileSize = ref<number>(3000 * 1024 * 1024); // 默认10MB
  const maxFileSizeMB = computed(() =>
    Math.floor(maxFileSize.value / (1024 * 1024))
  );
  const uploaderRef = ref();
  // 状态管理
  const fileList = ref<any[]>([]);
  const uploadedImage = ref<string | null>(null);
  const uploadedFile = ref<File | null>(null);
  const generatedResult = ref<string | null>(null);
  const showPreview = ref(false);
  const previewItems = ref<string[]>([]);
  const previewIndex = ref(0);
  const showSaveGuide = ref(false);
  const isLoading = ref(false);
  const isGenerating = ref(false);
  const isSaving = ref(false);
  const isPrinting = ref(false);

  // 计算模糊背景样式
  const blurStyle = computed(() => {
    if (!generatedResult.value) return {};
    return {
      backgroundImage: `url(${uploadedImage.value})`,
      filter: "blur(20px) brightness(0.8)",
    };
  });

  // 文件大小超出限制
  const onOversize = () => {
    // showToast({
    //   // type: "fail",
    //   message: `图片大小不能超过${maxFileSizeMB.value}MB`,
    //   duration: 2000,
    // });
  };

  // 处理图片上传
  const handleUpload = (file: any) => {
    if (file.file) {
      // 检查文件大小
      // if (file.file.size > maxFileSize.value) {
      //   onOversize();
      //   return;
      // }
      uploadedFile.value = file.file;

      const reader = new FileReader();
      reader.onload = (e) => {
        uploadedImage.value = e.target?.result as string;
        generatedResult.value = null;
      };
      reader.readAsDataURL(file.file);
    } else {
      showToast(t("errors.generic.operationFailed"));
    }
  };

  // 打开预览
  const openPreview = (url: string) => {
    previewItems.value = [url];
    previewIndex.value = 0;
    showPreview.value = true;
  };

  // 删除图片
  const handleDelete = () => {
    fileList.value = [];
    uploadedImage.value = null;
    generatedResult.value = null;
  };

  // 替换图片
  const handleReplace = () => {
    if (uploaderRef.value) {
      uploaderRef.value.chooseFile();
    }
  };

  // 生成内容
  const generate = async (
    generateFn: (
      file: File | string,
      type: CreationType,
      signal?: AbortSignal
    ) => Promise<string>,
    signal?: AbortSignal
  ) => {
    if (!uploadedImage.value) {
      showToast();
      return;
    }

    isGenerating.value = true;

    try {
      const result = await generateFn(
        uploadedFile.value || uploadedImage.value,
        creationType,
        signal
      );
      generatedResult.value = result;
      showToast({
        message: t("status.success"),
        duration: 2500,
        position: "top",
      });
    } catch (error) {
      generatedResult.value = null;
      console.error("生成失败:", error);
      throw error;
    } finally {
      isGenerating.value = false;
    }
  };

  // 保存内容
  const save = (filename: string, extension: string) => {
    if (!generatedResult.value) {
      return;
    }

    // 检查是否在微信、鸿蒙系统中
    const isWeChat = /MicroMessenger/i.test(navigator.userAgent);
    const isHarmonyOS = /Harmony|HwBrowser|HuaweiBrowser/i.test(
      navigator.userAgent
    );
    const isIOS = /iPad|iPhone|iPod/.test(navigator.userAgent);
    if (isWeChat) {
      if (creationType === "image") {
        showToast({
          message: t("results.saveInstructions"),
          duration: 3000,
        });
      } else {
        showToast({
          message: t("results.browserSaveTip"),
          duration: 3000,
        });
      }
      return;
    }

    if (isHarmonyOS) {
      showSaveGuide.value = true;
      if (creationType === "image") {
        showToast({
          message: t("results.saveInstructions"),
          duration: 3000,
        });
        return;
      }
    }
    if (isIOS) {
      if (creationType === "image") {
        showToast({
          message: t("results.saveInstructions"),
          duration: 3000,
        });
        return;
      }
    }

    isSaving.value = true;

    try {
      // const link = document.createElement("a");
      // link.href = generatedResult.value;
      // link.download = `${filename}_${new Date().getTime()}.${extension}`;
      // document.body.appendChild(link);
      // link.click();
      // document.body.removeChild(link);
      saveAs(
        generatedResult.value,
        `${filename}_${new Date().getTime()}.${extension}`
      );
      showToast({
        // type: "success",
        message: t("status.preparingDownload"),
        duration: 2500,
      });
    } catch (error) {
      showToast({
        // type: "fail",
        message: t("errors.generic.saveFailed"),
        duration: 2500,
      });
      console.error("保存失败:", error);
    } finally {
      isSaving.value = false;
    }
  };

  // 返回编辑
  const backToEdit = () => {
    generatedResult.value = null;
    // 将图片URL放到查询参数上
    updateQueryParams({
      resultUrl: null,
    });
  };

  // 打印图片（仅图片类型）
  const printImage = async (name: string) => {
    if (!generatedResult.value || creationType !== "image") {
      showToast(t("print.noContent"));
      return;
    }

    isPrinting.value = true;
    try {
      await printImageTask({ type: "STYLED_IMAGE", name });
      showToast({
        message: t("print.taskSent"),
        duration: 3500,
      });
    } catch (error) {
      if (error.message === "DUPLICATE_PRINT") {
        showToast(t("print.duplicateWarning"));
      } else {
        showToast(t("errors.generic.operationFailed"));
      }
    } finally {
      isPrinting.value = false;
    }
  };

  return {
    uploaderRef,
    creationType,
    maxFileSize,
    maxFileSizeMB,
    fileList,
    uploadedImage,
    generatedResult,
    isLoading,
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
    printImage,
  };
}
