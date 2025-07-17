import type { TaskType } from "./type";
import {
  getCastings,
  getPined,
  operateCasting,
  TaskOperateAction,
} from "./admin";

// 定义接口类型
export interface CastingImage {
  id: string;
  name: string;
  url: string;
  createdAt: string;
}

// 分页结果类型
export interface PaginatedResult<T> {
  items: T[];
  total: number;
  page: number;
  limit: number;
  totalPages: number;
}

// 模拟API服务
const DEFAULT_TYPE = "STYLED_IMAGE";

// 模拟数据
const mockImages: CastingImage[] = Array.from({ length: 50 }, (_, i) => ({
  id: `img-${i + 1}`,
  name: `图片 ${i + 1}`,
  url: `https://picsum.photos/1920/1080?random=${i}`,
  createdAt: new Date(
    Date.now() - Math.random() * 30 * 24 * 60 * 60 * 1000
  ).toISOString(),
  isPinned: i === 2, // 默认第三张图片为固定状态
  isActive: i === 0, // 默认第一张图片为活动状态
}));

// 当前固定图片ID
let pinnedImageId: string | null = mockImages[2].id;
// 当前活动图片索引
let activeIndex = 0;
let lastScore = null;

export const castingService = {
  // 获取当前展示图片
  async getCurrentCasting(
    type: TaskType = "STYLED_IMAGE",
    num: number = 1
  ): Promise<CastingImage[]> {
    // 模拟网络延迟
    await new Promise((resolve) => setTimeout(resolve, 500));

    // 如果有固定图片，返回固定图片
    if (pinnedImageId) {
      const pinnedImage = mockImages.find((img) => img.id === pinnedImageId);
      return pinnedImage ? [pinnedImage] : [mockImages[activeIndex]];
    }

    // 轮播模式，返回当前活动图片
    const result = [mockImages[activeIndex]];

    // 更新活动索引（模拟轮播）
    activeIndex = (activeIndex + 1) % mockImages.length;

    return result;
  },

  // 获取分页图片列表
  async getCastingList({
    type,
    keyword,
    page,
    limit,
  }: {
    keyword: string;
    type: TaskType;
    page: number;
    limit: number;
  }): Promise<PaginatedResult<CastingImage>> {
    // // 分页
    const start = (page - 1) * limit;
    const end = start + limit;
    const paginated = mockImages.slice(start, end);
    const { castings, hasMore, score, total } = await getCastings({
      type,
      keyword,
      score: page === 1 ? null : lastScore,
      pageNum: page,
      pageSize: limit,
    });
    lastScore = score;

    return {
      items: castings.map((item) => ({
        id: item.name,
        name: item.name.replace("casting:", ""),
        url: item.task.outputs.url,
        createdAt: item.task.createTime,
        isPinned: false,
        isActive: false,
      })),
      total,
      page,
      limit,
      totalPages: Math.ceil(mockImages.length / limit),
    };
  },

  // 删除图片
  async deleteImage(
    type: TaskType = DEFAULT_TYPE,
    imageNo: string
  ): Promise<any> {
    const res = await operateCasting({
      type,
      name: imageNo,
      action: TaskOperateAction.DELETE,
    });
    return res;
  },

  // 置顶图片
  async promoteImage(
    type: TaskType = DEFAULT_TYPE,
    imageNo: string
  ): Promise<any> {
    const res = await operateCasting({
      type,
      name: imageNo,
      action: TaskOperateAction.PROMOTE,
    });
    return res;
  },

  // 获取当前固定展示的图片
  async getPinedImage(type: TaskType = DEFAULT_TYPE) {
    const res = await getPined({ type });
    return res;
  },

  // 固定展示图片
  async pinImage(type: TaskType = DEFAULT_TYPE, imageNo: string): Promise<any> {
    const res = await operateCasting({
      type,
      name: imageNo,
      action: TaskOperateAction.PIN,
    });
    return res;
  },

  // 取消固定展示
  async unpinImage(
    type: TaskType = DEFAULT_TYPE,
    imageNo: string
  ): Promise<any> {
    const res = await operateCasting({
      type,
      name: imageNo,
      action: TaskOperateAction.UNPIN,
    });
    return res;
  },
};
