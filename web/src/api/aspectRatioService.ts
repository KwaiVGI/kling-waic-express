/**
 * 宽高比服务
 * 用于获取图片容器的宽高比配置
 */

export interface AspectRatioResponse {
  ratio: [number, number];
  success: boolean;
  message?: string;
}

class AspectRatioService {
  /**
   * 获取当前的容器宽高比配置
   * @returns Promise<[number, number]> 容器宽高比数组，如 [9, 16]
   */
  async getAspectRatio(): Promise<[number, number]> {
    try {
      // TODO: 替换为真实的API调用
      // const response = await fetch('/api/aspect-ratio');
      // const data = await response.json();
      // return data.ratio;

      // Mock实现
      return this.mockGetAspectRatio();
    } catch (error) {
      console.error("获取宽高比失败:", error);
      // 返回默认值
      return [9, 16];
    }
  }

  /**
   * Mock实现 - 模拟API返回
   */
  private async mockGetAspectRatio(): Promise<[number, number]> {
    return new Promise((resolve) => {
      setTimeout(() => {
        // 预定义的容器宽高比选项
        const containerRatioOptions: [number, number][] = [
          [9, 16], // 竖屏 9:16 (默认)
          [16, 9], // 横屏 16:9
          [4, 3], // 传统 4:3
          [3, 4], // 竖屏 3:4
          [1, 1], // 正方形 1:1
          [21, 9], // 超宽屏 21:9
        ];

        // 支持通过URL参数指定容器宽高比，方便测试
        const urlParams = new URLSearchParams(window.location.search);
        const ratioParam = urlParams.get("containerRatio") || urlParams.get("ratio");

        if (ratioParam) {
          const [w, h] = ratioParam.split(":").map(Number);
          if (w && h && w > 0 && h > 0) {
            console.log(`使用URL参数指定的容器宽高比: ${w}:${h}`);
            resolve([w, h]);
            return;
          }
        }

        // 可以根据时间或其他条件返回不同的宽高比进行测试
        // const randomIndex = Math.floor(Math.random() * containerRatioOptions.length);
        // resolve(containerRatioOptions[randomIndex]);

        // 默认返回 9:16
        resolve([9, 16]);
      }, 100); // 模拟网络延迟
    });
  }

  /**
   * 设置宽高比（如果API支持）
   * @param ratio 宽高比数组
   */
  async setAspectRatio(ratio: [number, number]): Promise<boolean> {
    try {
      // TODO: 实现设置宽高比的API调用
      console.log("设置宽高比:", ratio);
      return true;
    } catch (error) {
      console.error("设置宽高比失败:", error);
      return false;
    }
  }
}

export const aspectRatioService = new AspectRatioService();
