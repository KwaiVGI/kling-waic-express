import request from '@/utils/request' // 导入您提供的请求函数
import type { TaskType } from './types' // 假设有单独的类型定义

// 定义相关类型（根据实际业务调整）
interface Task {
  id: string
  name: string
  type: TaskType
  // ...其他字段
}

interface TaskOutput {
  id: string
  content: string
  // ...其他字段
}

// 接口定义
const CastingAPI = {
  /**
   * 获取指定类型的下一个任务输出
   * @param type 任务类型
   * @param num 获取数量
   */
  getNextTaskOutputs: (type: TaskType, num: number): Promise<TaskOutput[]> => {
    return request({
      url: `/castings/${type}/${num}`,
      method: 'GET',
    })
  },

  /**
   * 获取分页任务列表
   * @param type 任务类型
   * @param page 页码
   */
  getTaskList: (type: TaskType, page: number): Promise<Task[]> => {
    return request({
      url: `/castings/${type}/list/${page}`,
      method: 'GET',
    })
  },

  /**
   * 提升任务优先级
   * @param type 任务类型
   * @param taskName 任务名称
   * @remark 注意直接传递字符串而非对象
   */
  promoteTask: (type: TaskType, taskName: string): Promise<boolean> => {
    return request({
      url: `/castings/${type}/promote`,
      method: 'POST',
      data: taskName, // 直接传递字符串
    })
  },

  /**
   * 置顶任务
   * @param type 任务类型
   */
  pinTask: (type: TaskType): Promise<boolean> => {
    return request({
      url: `/castings/${type}/pin`,
      method: 'POST',
    })
  },

  /**
   * 取消置顶
   * @param type 任务类型
   */
  unpinTask: (type: TaskType): Promise<boolean> => {
    return request({
      url: `/castings/${type}/unpin`,
      method: 'POST',
    })
  },
}

export default CastingAPI
