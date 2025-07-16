import request from '@/utils/request'

// 定义接口类型
export interface CastingImage {
  id: string;
  title: string;
  description: string;
  url: string;
  width: number;
  height: number;
  createdAt: string;
  isPinned: boolean;
  isActive: boolean;
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
const API_BASE_URL = 'https://api.example.com';
const DEFAULT_TYPE = 'default';

// 模拟数据
const mockImages: CastingImage[] = Array.from({ length: 50 }, (_, i) => ({
  id: `img-${i + 1}`,
  title: `图片 ${i + 1}`,
  description: `这是第 ${i + 1} 张图片的描述，展示美丽的风景或重要内容`,
  url: `https://picsum.photos/1920/1080?random=${i}`,
  width: 1920,
  height: 1080,
  createdAt: new Date(Date.now() - Math.random() * 30 * 24 * 60 * 60 * 1000).toISOString(),
  isPinned: i === 2, // 默认第三张图片为固定状态
  isActive: i === 0, // 默认第一张图片为活动状态
}));

// 当前固定图片ID
let pinnedImageId: string | null = mockImages[2].id;
// 当前活动图片索引
let activeIndex = 0;

export const castingService = {
  // 获取当前展示图片
  async getCurrentCasting(type: string = DEFAULT_TYPE, num: number = 1): Promise<CastingImage[]> {
    // 模拟网络延迟
    await new Promise(resolve => setTimeout(resolve, 500));
    
    // 如果有固定图片，返回固定图片
    if (pinnedImageId) {
      const pinnedImage = mockImages.find(img => img.id === pinnedImageId);
      return pinnedImage ? [pinnedImage] : [mockImages[activeIndex]];
    }
    
    // 轮播模式，返回当前活动图片
    const result = [mockImages[activeIndex]];
    
    // 更新活动索引（模拟轮播）
    activeIndex = (activeIndex + 1) % mockImages.length;
    
    return result;
  },
  
  // 获取分页图片列表
  async getCastingList(
    type: string = DEFAULT_TYPE, 
    page: number = 1, 
    limit: number = 12
  ): Promise<PaginatedResult<CastingImage>> {
    // // 模拟网络延迟
    // await new Promise(resolve => setTimeout(resolve, 500));
    
    // // 分页
    const start = (page - 1) * limit;
    const end = start + limit;
    const paginated = mockImages.slice(start, end);
    const res = await request.get(`/api/castings/${type}/list`)
    
    return {
      items: paginated,
      total: mockImages.length,
      page,
      limit,
      totalPages: Math.ceil(mockImages.length / limit)
    };
  },
  
  // 置顶图片
  async promoteImage(type: string = DEFAULT_TYPE, imageId: string): Promise<void> {
    // 模拟网络延迟
    await new Promise(resolve => setTimeout(resolve, 300));
    
    // 在实际应用中，这里会有真正的API调用
    const index = mockImages.findIndex(img => img.id === imageId);
    if (index !== -1) {
      // 将图片移动到数组开头
      const [image] = mockImages.splice(index, 1);
      mockImages.unshift(image);
      // 更新活动索引
      activeIndex = 0;
    }
    
    console.log(`Promoted image: ${imageId}`);
  },
  
  // 固定展示图片
  async pinImage(type: string = DEFAULT_TYPE, imageId: string): Promise<void> {
    // 模拟网络延迟
    await new Promise(resolve => setTimeout(resolve, 300));
    
    pinnedImageId = imageId;
    console.log(`Pinned image: ${imageId}`);
  },
  
  // 取消固定展示
  async unpinImage(type: string = DEFAULT_TYPE): Promise<void> {
    // 模拟网络延迟
    await new Promise(resolve => setTimeout(resolve, 300));
    
    pinnedImageId = null;
    console.log('Unpinned image');
  }
};