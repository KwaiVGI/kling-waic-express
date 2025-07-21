import type { TaskType } from "./type";
import {
  getCastings,
  getPined,
  getPrintList,
  getScreenList,
  operateCasting,
  printImageFromConsole,
  TaskOperateAction,
} from "./admin";

// 定义接口类型
export interface CastingImage {
  id: string;
  name: string;
  url: string;
  createdAt?: string;
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

let lastScore = null;

export const castingService = {
  // 获取当前展示图片
  async getCurrentCasting(
    type: TaskType = "STYLED_IMAGE",
    num: number = 1
  ): Promise<CastingImage[]> {
    if (type === "STYLED_IMAGE") {
      const res = await castingService.getPinedImage();

      // 如果有固定图片，返回固定图片
      if (res) {
        return [
          {
            id: res.name,
            name: res.name,
            url: res.task.outputs.url,
          },
        ];
      }
    }
    const list = await getScreenList({
      type,
      num,
    });

    return list.map((item) => ({
      id: item.name,
      name: item.name,
      url: item.task.outputs.url,
    }));
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
  async printImage(name: string) {
    const res = await printImageFromConsole({
      type: "STYLED_IMAGE",
      name,
    });
    return res;
  },
  async getPrintList(keyword = "") {
    const res = await getPrintList({ keyword });
    console.log(res);
    const items = res.map((item) => ({
      id: item.name,
      name: item.name.replace("printing:No.", ""),
      url: item.task.outputs.url,
    }));
    return items;
  },
};
