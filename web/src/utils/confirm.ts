import { createApp, h } from "vue";

// 定义弹窗配置选项类型
interface DeleteConfirmOptions {
  title?: string;
  message?: string;
  confirmText?: string;
  cancelText?: string;
  type?: "danger" | "warning" | "info";
}

/**
 * 命令式删除确认弹窗
 * @param options 弹窗配置选项
 * @returns Promise 用户操作结果（true: 确认, false: 取消）
 */
export const confirmDelete = (options: DeleteConfirmOptions = {}) => {
  return new Promise<boolean>((resolve) => {
    // 创建挂载点
    const mountPoint = document.createElement("div");
    document.body.appendChild(mountPoint);

    // 默认配置
    const {
      title = "删除确认",
      message = "确定要删除这条记录吗？删除后数据将无法恢复！",
      confirmText = "确认删除",
      cancelText = "取消",
      type = "danger",
    } = options;

    // 创建Vue应用实例
    const app = createApp({
      render() {
        return h("div", { class: "pc-modal-mask" }, [
          h("div", { class: "modal-container" }, [
            // 弹窗头部
            h("div", { class: "modal-header" }, [
              h("i", { class: "fas fa-exclamation-triangle" }),
              h("h3", {}, title),
            ]),

            // 弹窗内容
            h("div", { class: "modal-body" }, message),

            // 弹窗底部
            h("div", { class: "modal-footer" }, [
              h(
                "button",
                {
                  class: "modal-btn cancel",
                  onClick: () => {
                    resolve(false);
                    app.unmount();
                    document.body.removeChild(mountPoint);
                  },
                },
                cancelText
              ),
              h(
                "button",
                {
                  class: "modal-btn confirm",
                  onClick: () => {
                    resolve(true);
                    app.unmount();
                    document.body.removeChild(mountPoint);
                  },
                },
                confirmText
              ),
            ]),
          ]),
        ]);
      },
    });

    // 挂载应用
    app.mount(mountPoint);
  });
};
